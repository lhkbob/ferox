package com.ferox.renderer.impl.jogl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

import javax.media.opengl.DebugGL;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.Threading;
import javax.swing.SwingUtilities;

import com.ferox.math.Transform;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.WindowSurface;
import com.ferox.renderer.impl.AbstractRenderer;
import com.ferox.renderer.impl.ContextRecordSurface;
import com.ferox.renderer.impl.SurfaceFactory;
import com.ferox.renderer.impl.jogl.drivers.JoglTransformDriver;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.texture.TextureImage.TextureTarget;

/** The JoglSurfaceFactory provides an implementation of SurfaceFactory
 * for use with jogl based renderers.  Its internals with context management
 * are inherently not thread safe and should not be used from multiple threads.
 * 
 * This was chosen because it greatly simplified implementation and a surface factory
 * should only be used internally by an AbstractRenderer, which imposes single threadedness
 * anyway.  
 * 
 * This class is public so that new jogl based renderers can be more easily developed
 * (e.g. to replace the drivers being used by implementing smarter driver factories).  
 * Even so, it should only be created inside a constructor of a renderer implementation
 * and used only be that renderer.  Otherwise undefined and dangerous behavior WILL occur.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglSurfaceFactory implements SurfaceFactory {	
	/* Variables for the shadow context. */
	private ShadowContext shadowContext;
	
	/* Variables for surfaces. */
	private Set<Integer> usedIds;
	private int windowCreatedCount;
	private boolean fullscreenCreated;

	private Queue<JoglOnscreenSurface> zombieWindowSurfaces;
	private Map<GLAutoDrawable, Queue<JoglFbo>> zombieFbos;
		
	private List<JoglRenderSurface> realizedSurfaces;
	
	/* Misc variables. */
	private boolean debugGL;
	private AbstractRenderer renderer;
	private JoglTransformDriver transformDriver;
	
	private List<AttachableSurfaceGLEventListener> queuedRenderActions;
	
	private GLAutoDrawable currentDrawable;
	private JoglStateRecord currentRecord;
	private GL currentGL;
	
	/** Construct a surface factory for the given renderer (this renderer
	 * must use this surface factory or undefined results will happen).
	 * 
	 * If debug is true, opengl errors will be checked after each opengl call. */
	public JoglSurfaceFactory(AbstractRenderer renderer, RenderCapabilities caps, boolean debug) throws RenderException {
		if (renderer == null)
			throw new RenderException("Cannot create a surface factory with a null renderer");

		this.debugGL = debug;
		this.renderer = renderer;
		this.transformDriver = new JoglTransformDriver(this);
		
		this.windowCreatedCount = 0;
		this.fullscreenCreated = false;
		
		this.usedIds = new HashSet<Integer>();
		
		this.realizedSurfaces = new ArrayList<JoglRenderSurface>();
		this.queuedRenderActions = new ArrayList<AttachableSurfaceGLEventListener>();
		
		// initialize the data structures
		this.zombieWindowSurfaces = new LinkedList<JoglOnscreenSurface>();
		this.zombieFbos = new HashMap<GLAutoDrawable, Queue<JoglFbo>>();
		
		// create the shadow context
		if (caps.getPbufferSupport()) {
			this.shadowContext = new PbufferShadowContext(caps);
		} else {
			this.shadowContext = new OnscreenShadowContext(caps);
		}
		
	}
	
	/** Get the renderer that uses this factory. */
	public AbstractRenderer getRenderer() {
		return this.renderer;
	}
	
	@Override
	public FullscreenSurface createFullscreenSurface(DisplayOptions options, int width, int height) throws RenderException {
		if (this.windowCreatedCount > 0)
			throw new RenderException("Cannot create a FullscreenSurface when there are created WindowSurfaces");
		
		JoglFullscreenSurface s = null;
		int id = this.nextId();
		try {
			s = new JoglFullscreenSurface(this, id, options, width, height);
		} catch (RuntimeException e) {
			this.usedIds.remove(id);
			throw e;
		}
		
		this.fullscreenCreated = true;
		if (s.getGLAutoDrawable() != null)
			this.realizedSurfaces.add(s);
		return s;
	}

	@Override
	public WindowSurface createWindowSurface(DisplayOptions options, int x,	int y, int width, int height,
											 boolean resizable, boolean undecorated) throws RenderException {
		if (this.fullscreenCreated)
			throw new RenderException("Cannot create a WindowSurface when there is already a FullscreenSurface");
		
		JoglWindowSurface s = null;
		int id = this.nextId();
		try {
			s = new JoglWindowSurface(this, id, options, x, y, width, height, resizable, undecorated);
		} catch (RuntimeException e) {
			this.usedIds.remove(id);
			throw e;
		}
		
		this.windowCreatedCount++;
		if (s.getGLAutoDrawable() != null)
			this.realizedSurfaces.add(s);
		return s;
	}
	
	@Override
	public TextureSurface createTextureSurface(DisplayOptions options, TextureTarget target, int width, int height, int depth, int layer,
											   int numColorTargets, boolean useDepthRenderBuffer) throws RenderException {
		JoglTextureSurface s = null;
		int id = this.nextId();
		
		try {
			s = new JoglTextureSurface(this, id, options, target, width, height, depth, layer, numColorTargets, useDepthRenderBuffer);
		} catch (RuntimeException e) {
			this.usedIds.remove(id);
			throw e;
		}
		
		if (s.getGLAutoDrawable() != null)
			this.realizedSurfaces.add(s);
		return s;
	}

	@Override
	public TextureSurface createTextureSurface(TextureSurface share, int layer) throws RenderException {
		JoglTextureSurface s = null;
		int id = this.nextId();
		
		try {
			s = new JoglTextureSurface(this, id, (JoglTextureSurface) share, layer);
		} catch (RuntimeException e) {
			this.usedIds.remove(id);
			throw e;
		}
		
		if (s.getGLAutoDrawable() != null)
			this.realizedSurfaces.add(s);
		return s;
	}

	@Override
	public void destroy(ContextRecordSurface surface) {
		JoglRenderSurface d = (JoglRenderSurface) surface;
		d.destroySurface();
		this.relinquishSurface(d);
	}

	@Override
	public void destroy() {
		this.processNotifiedSurfaces();
		
		if (this.realizedSurfaces.size() > 0) {
			// this shouldn't happen, but we don't want a memory leak
			for (int i = this.realizedSurfaces.size(); i >= 0; i--)
				this.destroy(this.realizedSurfaces.get(i));
		}
		
		this.shadowContext.destroy();
	}
	
	@Override
	public void renderFrame(List<ContextRecordSurface> queuedSurfaces, Runnable resourceAction) {
		int size = queuedSurfaces.size();
		JoglRenderSurface s;
		for (int i = 0; i < size; i++) {
			s = (JoglRenderSurface) queuedSurfaces.get(i);
			if (s.getGLAutoDrawable() != null)
				this.queuedRenderActions.add(s);
		}
		
		if (this.queuedRenderActions.size() == 0) {
			// we don't have any displaying surfaces to use, so
			// we use the shadow context
			this.queuedRenderActions.add(this.shadowContext);
		}
		
		// assign the resource action
		this.queuedRenderActions.get(0).assignResourceAction(resourceAction);
		
		// attach all queued surfaces to the queued render actions
		int raIndex = 0;
		GLAutoDrawable queued;
		GLAutoDrawable realized = this.queuedRenderActions.get(raIndex).getGLAutoDrawable();

		for (int i = 0; i < size; i++) {
			s = (JoglRenderSurface) queuedSurfaces.get(i);
			queued = s.getGLAutoDrawable();
			if (queued != null && queued != realized) {
				// advance to next realized surface
				raIndex++;
				realized = this.queuedRenderActions.get(raIndex).getGLAutoDrawable();
			}
			
			// attach s to the current realized surface
			this.queuedRenderActions.get(raIndex).attachRenderSurface(s);
		}
		
		try {
			// render all the actions now
			AttachableSurfaceGLEventListener renderAction;
			size = this.queuedRenderActions.size();
			for (int i = 0; i < size; i++) {
				renderAction = this.queuedRenderActions.get(i);

				this.currentDrawable = renderAction.getGLAutoDrawable();
				this.currentRecord = renderAction.getStateRecord();
				//this.currentGL = (this.debugGL ? new TraceGL(new DebugGL(this.currentDrawable.getGL()), System.out) : this.currentDrawable.getGL());
				this.currentGL = (this.debugGL ? new DebugGL(this.currentDrawable.getGL()) : this.currentDrawable.getGL());

				renderAction.render();
			}
		} finally {
			// must clear the list even when an exception occurs
			this.queuedRenderActions.clear();
		}
	}
	
	@Override
	public boolean isGraphicsThread() {
		return !Threading.isSingleThreaded() || Threading.isOpenGLThread();
	}
	
	/** Return the GLAutoDrawable that is currently executing its
	 * GLEventListeners.  This is not necessarily associated with
	 * the actual "current" surface, since fbos can be attached
	 * to a drawable to get a valid context. */
	public GLAutoDrawable getDisplayingDrawable() {
		return this.currentDrawable;
	}
	
	/** Return the transform driver used by this factory's renderer. */
	public JoglTransformDriver getTransformDriver() {
		return this.transformDriver;
	}
	
	/** Returns the current, intended view transform in
	 * the modelview matrix.  Because of the implementation
	 * of JoglTransformDriver, this may not actually be reflected
	 * in the opengl state.  See JoglTransformDriver for more details. */
	public Transform getViewTransform() {
		return this.transformDriver.getCurrentViewTransform();
	}
	
	/** Returns the current state record, which will only be not
	 * null when a JoglRenderSurface is having its display() method
	 * invoked. */
	public JoglStateRecord getRecord() {
		return this.currentRecord;
	}
	
	/** Return the GL instance that is associated with the current
	 * context.  This must only be called from within a method
	 * in the stack of a display() call of a JoglRenderSurface. */
	public GL getGL() {
		return this.currentGL;
	}
	
	/** Return the context that represents the shadow context
	 * every realized surface must share its own context with. */
	public GLContext getShadowContext() {
		return this.shadowContext.getContext();
	}
	
	/** Utility method to invoke a Runnable on the AWT event dispatch
	 * thread (e.g. for modifying AWT and Swing components). 
	 * 
	 * This will throw an runtime exception if a problem occurs.
	 * It works properly if called from the AWT thread. 
	 * 
	 * This should be used when EventQueue.invokeAndWait() or
	 * SwingUtilities.invokeAndWait() would be used, except that
	 * this is thread safe. */
	public static void invokeOnAwtThread(Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private void relinquishSurface(JoglRenderSurface surface) {
		GLAutoDrawable c = surface.getGLAutoDrawable();
		if (c != null) {
			this.realizedSurfaces.remove(surface);
			this.zombieFbos.remove(c); // the fbos are automatically destroyed
		}
		
		if (surface instanceof JoglWindowSurface)
			this.windowCreatedCount--;
		else if (surface instanceof JoglFullscreenSurface)
			this.fullscreenCreated = false;
		
		this.usedIds.remove(surface.getSurfaceId());
	}
	
	/* Utility class to cleanup fbos for a given GLAutoDrawable. */
	private class ZombieFboCleanupAction implements Runnable {
		private Queue<JoglFbo> fbos;
		
		public ZombieFboCleanupAction(Queue<JoglFbo> toCleanup) {
			this.fbos = toCleanup;
		}
		
		@Override
		public void run() {
			while(!this.fbos.isEmpty())
				this.fbos.poll().destroy(currentGL);
		}
	}
	
	private void processNotifiedSurfaces() {
		int count = this.zombieWindowSurfaces.size();
		if (count > 0) {
			synchronized(this.zombieWindowSurfaces) {
				JoglOnscreenSurface surface;
				while (count > 0) {
					surface = this.zombieWindowSurfaces.poll();
					if (surface == null)
						break;
					
					surface.destroySurface();
					this.renderer.notifyOnscreenSurfaceClosed(surface);
					this.relinquishSurface(surface);
					count--;
				}
			}
		}
			
		// perform zombie fbos afterwards in case a notified window's
		// context was in the zombieFbo map.
		if (this.zombieFbos.size() > 0) {
			ZombieFboCleanupAction cleanup;
			for (Entry<GLAutoDrawable, Queue<JoglFbo>> e: this.zombieFbos.entrySet()) {
				cleanup = new ZombieFboCleanupAction(e.getValue());
				this.renderFrame(AbstractRenderer.EMPTY_LIST, cleanup);
			}
			this.zombieFbos.clear();
		}
	}
	
	private int nextId() {
		int counter = 0;
		while(this.usedIds.contains(counter))
			counter++;
		this.usedIds.add(counter);
		return counter;
	}
	
	/** Should only be called when the given fbo should be 
	 * scheduled for destruction.  At the next possible time,
	 * the fbo will have its destroy() method called on the
	 * owner context. */
	void notifyFboZombie(GLAutoDrawable owner, JoglFbo fbo) {
		Queue<JoglFbo> queue = this.zombieFbos.get(owner);
		if (queue == null) {
			queue = new LinkedList<JoglFbo>();
			this.zombieFbos.put(owner, queue);
		}
		queue.add(fbo);
	}
	
	/** Notify the surface factory that an onscreen surface was
	 * closed by user action.  This will then clean-up other
	 * resources at an appropriate time on the main thread.
	 * 
	 * It is expected that this method is called from the AWT
	 * thread, while event handling the closing window. */
	void notifyOnscreenSurfaceZombie(JoglOnscreenSurface surface) {
		surface.markDestroyed(); // flag it as destroyed so the Renderer won't use it
		synchronized(this.zombieWindowSurfaces) {
			this.zombieWindowSurfaces.add(surface);
		};
	}
}
