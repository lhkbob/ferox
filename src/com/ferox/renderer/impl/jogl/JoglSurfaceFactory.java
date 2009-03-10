package com.ferox.renderer.impl.jogl;

import java.awt.Frame;
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
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.Threading;
import javax.swing.SwingUtilities;

import com.ferox.math.Color;
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
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;
import com.ferox.resource.TextureImage.TextureTarget;

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
	private Queue<JoglOnscreenSurface> zombieWindowSurfaces;
	private Map<JoglContext, Queue<JoglFbo>> zombieFbos;
	
	private List<JoglRenderSurface> realizedSurfaces; // surfaces that own their gl contexts
	
	private JoglContext currentContext;
	private JoglRenderSurface currentSurface;
	private GL currentGL;
	
	private List<JoglRenderSurface> queuedSurfaces;
	private int queuePosition;
	
	private AbstractRenderer renderer;
	private JoglTransformDriver transformDriver;
	
	/* Variables for creating surfaces. */
	private JoglContext shadowContext;
	private GLPbuffer shadowPbuffer; // use this if pbuffers are supported
	private GLCanvas shadowCanvas;
	
	private Frame shadowFrame; // only created if we're using the shadowCanvas and need to make the canvas current
	private Runnable validateFrame;
	private Runnable hideFrame;
	
	private Set<Integer> usedIds;
	private int windowCreatedCount;
	private boolean fullscreenCreated;

	private boolean debugGL;
	
	/** Construct a surface factory for the given renderer (this renderer
	 * must use this surface factory or undefined results will happen).
	 * 
	 * If debug is true, opengl errors will be checked after each opengl call. */
	public JoglSurfaceFactory(AbstractRenderer renderer, RenderCapabilities caps, boolean debug) throws RenderException {
		if (renderer == null)
			throw new RenderException("Cannot create a surface factory with a null renderer");
		this.renderer = renderer;
		this.transformDriver = new JoglTransformDriver(this);
		
		this.windowCreatedCount = 0;
		this.fullscreenCreated = false;
		
		// initialize the data structures
		this.realizedSurfaces = new ArrayList<JoglRenderSurface>();
		this.zombieWindowSurfaces = new LinkedList<JoglOnscreenSurface>();
		this.zombieFbos = new HashMap<JoglContext, Queue<JoglFbo>>();
		
		// set up the shadow context
		GLCapabilities glCaps = new GLCapabilities();
		glCaps.setDepthBits(0); 
		glCaps.setDoubleBuffered(false);
		
		if (GLDrawableFactory.getFactory().canCreateGLPbuffer()) {
			this.shadowPbuffer = GLDrawableFactory.getFactory().createGLPbuffer(glCaps, new DefaultGLCapabilitiesChooser(), 1, 1, null);
			this.shadowCanvas = null;
			this.shadowContext = new JoglContext(this.shadowPbuffer.getContext(), new JoglStateRecord(caps));
		} else {
			this.shadowPbuffer = null;
			this.shadowCanvas = new GLCanvas(glCaps);
			this.shadowContext = new JoglContext(this.shadowCanvas.getContext(), new JoglStateRecord(caps));
			
			this.validateFrame = new Runnable() {
				public void run() {
					if (JoglSurfaceFactory.this.shadowFrame == null) {
						// create it
						JoglSurfaceFactory.this.shadowFrame = new Frame();
						JoglSurfaceFactory.this.shadowFrame.setBounds(0, 0, 1, 1);
						JoglSurfaceFactory.this.shadowFrame.setUndecorated(true);
						JoglSurfaceFactory.this.shadowFrame.add(JoglSurfaceFactory.this.shadowCanvas);
					}
					// make it visible
					JoglSurfaceFactory.this.shadowFrame.setVisible(true);
				}
			};
			this.hideFrame = new Runnable() {
				public void run() {
					JoglSurfaceFactory.this.shadowFrame.setVisible(false);
				}
			};
		}
		
		this.debugGL = debug;
		
		this.currentSurface = null;
		this.currentContext = null;
		this.usedIds = new HashSet<Integer>();
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
		if (s.getContext() != null)
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
		if (s.getContext() != null)
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
		
		if (s.getContext() != null)
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
		
		if (s.getContext() != null)
			this.realizedSurfaces.add(s);
		return s;
	}

	@Override
	public void destroy(ContextRecordSurface surface) {
		JoglRenderSurface d = (JoglRenderSurface) surface;
		if (d == this.currentSurface)
			this.release();
		d.onDestroySurface();
		this.relinquishSurface(d);
	}

	@Override
	public void destroy() {
		this.processNotifiedSurfaces();
		this.release(); // will hide the shadow frame if needed
		
		if (this.realizedSurfaces.size() > 0) {
			// this shouldn't happen, but we don't want a memory leak
			for (int i = this.realizedSurfaces.size(); i >= 0; i--)
				this.destroy(this.realizedSurfaces.get(i));
		}
		
		if (this.shadowCanvas != null) {
			if (this.shadowFrame != null) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							JoglSurfaceFactory.this.shadowFrame.dispose();
						}
					});
				} catch (Exception e) { /* Do nothing ?? */	}
				this.shadowFrame = null;
			}
			this.shadowContext.getContext().destroy();
		} else {
			this.shadowPbuffer.destroy();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void startFrame(List<ContextRecordSurface> queuedSurfaces) {
		this.processNotifiedSurfaces();
		// This cast is okay, since this list will only have surfaces created by this factory
		this.queuedSurfaces = (List<JoglRenderSurface>) (List) queuedSurfaces;
		this.queuePosition = 0;
	}

	@Override
	public JoglRenderSurface getCurrentSurface() {
		return this.currentSurface;
	}
	
	@Override
	public void clearBuffers() {
		if (this.currentSurface != null) {
			FramebufferRecord record = this.currentContext.getStateRecord().frameRecord;
			GL gl = this.currentGL;

			Color clearColor = this.currentSurface.getClearColor();
			if (record.clearColor.equals(clearColor)) {
				gl.glClearColor(clearColor.getRed(), clearColor.getGreen(), clearColor.getBlue(), clearColor.getAlpha());
				clearColor.get(record.clearColor);
			}
			float clearDepth = this.currentSurface.getClearDepth();
			if (record.clearDepth != clearDepth) {
				gl.glClearDepth(clearDepth);
				record.clearDepth = clearDepth;
			}
			int clearStencil = this.currentSurface.getClearStencil();
			if (record.clearStencil != clearStencil) {
				gl.glClearStencil(clearStencil);
				record.clearStencil = clearStencil;
			}
				
			int clearBits = 0;
			if (this.currentSurface.isColorBufferCleared())
				clearBits |= GL.GL_COLOR_BUFFER_BIT;
			if (this.currentSurface.isDepthBufferCleared())
				clearBits |= GL.GL_DEPTH_BUFFER_BIT;
			if (this.currentSurface.isStencilBufferCleared())
				clearBits |= GL.GL_STENCIL_BUFFER_BIT;
			
			if (clearBits != 0)
				gl.glClear(clearBits);
			
			// these aren't covered by any drivers at the moment, so make sure they're
			// enabled correctly
			PixelOpRecord pr = this.currentContext.getStateRecord().pixelOpRecord;
			if (!pr.enableScissorTest) {
				gl.glEnable(GL.GL_SCISSOR_TEST);
				pr.enableScissorTest = true;
			}
			// this isn't part of the state record, but overhead should be minimal
			gl.glEnable(GL.GL_RESCALE_NORMAL);
		}
	}

	@Override
	public void swapBuffers() {
		if (this.currentSurface != null)
			this.currentSurface.swapBuffers();
	}
	
	@Override
	public void makeCurrent(ContextRecordSurface surface) throws RenderException {
		if (this.currentSurface != surface) {
			JoglRenderSurface next = (JoglRenderSurface) surface;
			if (this.currentSurface != null)
				this.currentSurface.onRelease(next);
			
			JoglContext context = next.getContext();
			if (context == null) {
				if (this.currentContext == null) {
					if (this.queuedSurfaces != null) {
						// try to find the next context that will be made current
						int size = this.queuedSurfaces.size();
						for (int i = this.queuePosition + 1; i < size; i++) {
							context = this.queuedSurfaces.get(i).getContext();
							if (context != null)
								break;
						}
					}
					
					if (context == null) // didn't find anything before
						context = (this.realizedSurfaces.size() > 0 ? this.realizedSurfaces.get(0).getContext() : this.shadowContext);
				} else
					context = this.currentContext;
			}
			
			this.makeCurrent(context);
			this.currentSurface = next;
			next.onMakeCurrent();
			
			// update our position in the queue
			if (this.queuedSurfaces != null) {
				this.queuePosition++;
				if (this.queuePosition == this.queuedSurfaces.size())
					this.queuedSurfaces = null;
			}
		}
	}

	@Override
	public void makeShadowContextCurrent() throws RenderException {
		// we don't just call release, in case the current surface is using the shadow context
		if (this.currentSurface != null)
			this.currentSurface.onRelease(null);
		this.currentSurface = null;
		this.makeCurrent(this.shadowContext);
	}

	@Override
	public void release() {
		if (this.currentSurface != null)
			this.currentSurface.onRelease(null);
		this.currentSurface = null;
		this.releaseContext();
	}
	
	/** Return the transform driver used by this factory's renderer. */
	public JoglTransformDriver getTransformDriver() {
		return this.transformDriver;
	}
	
	/** Returns the current, intended view transform in
	 * the modelview matrix.  Because of the implementation
	 * of JoglTransformDriver, this may not actually be reflected
	 * in the opengl state.  See JoglTransformDriver for more details. */
	public Transform getCurrentView() {
		return this.transformDriver.getCurrentViewTransform();
	}
	
	/** Return the JoglContext that is current. */
	public final JoglContext getCurrentContext() {
		return this.currentContext;
	}
	
	/** Return the GL instance that is associated with the current
	 * context.  Returns null if there is no current context. */
	public final GL getGL() {
		return this.currentGL;
	}
	
	/** Return the context that represents the shadow context
	 * every realized surface must share its own context with. */
	public final JoglContext getShadowContext() {
		return this.shadowContext;
	}
	
	private void makeCurrent(JoglContext context) {
		if (this.currentContext != context) {
			this.releaseContext();
			
			if (context == this.shadowContext && this.shadowCanvas != null) {
				// make the shadow canvas's context ready for use
				if (this.shadowFrame == null || !this.shadowFrame.isVisible()) {
					this.shadowContext = new JoglContext(this.shadowCanvas.getContext(), new JoglStateRecord(this.renderer.getCapabilities()));
					context = this.shadowContext;
					try {
						SwingUtilities.invokeAndWait(this.validateFrame);
					} catch (Exception e) {
						throw new RenderException("Error making shadow context current", e);
					}
				}
			}
			
			// make the GLContext current on this thread
			GLContext c = context.getContext();
			int res = c.makeCurrent();
			while (res == GLContext.CONTEXT_NOT_CURRENT) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException ie) { }
				res = c.makeCurrent();
			}
			this.currentContext = context;
			this.currentGL = (this.debugGL ? new DebugGL(c.getGL()) : c.getGL());
		}
	}
	
	private void releaseContext() {
		if (this.currentContext != null) {
			GLContext c = this.currentContext.getContext();
			while(GLContext.getCurrent() == c)
				c.release();
			
			if (this.currentContext == this.shadowContext && this.shadowFrame != null)
				SwingUtilities.invokeLater(this.hideFrame); // need to hide the shadow context now
			
			this.currentContext = null;
			this.currentGL = null;
		}
	}
	
	private void relinquishSurface(JoglRenderSurface surface) {
		JoglContext c = surface.getContext();
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
	
	private void processNotifiedSurfaces() {
		int count = this.zombieWindowSurfaces.size();
		if (count > 0) {
			synchronized(this.zombieWindowSurfaces) {
				this.release(); // don't want the surface to be current
				JoglOnscreenSurface surface;
				while (count > 0) {
					surface = this.zombieWindowSurfaces.poll();
					if (surface == null)
						break;
					
					surface.onDestroySurface();
					this.renderer.notifyOnscreenSurfaceClosed(surface);
					this.relinquishSurface(surface);
					count--;
				}
			}
		}
		
		// perform zombie fbos afterwards in case a notified window's
		// context was in the zombieFbo map.
		if (this.zombieFbos.size() > 0) {
			this.release();
			Queue<JoglFbo> toDestroy;
			JoglContext current;
			
			for (Entry<JoglContext, Queue<JoglFbo>> e: this.zombieFbos.entrySet()) {
				current = e.getKey();
				this.makeCurrent(current);
				toDestroy = e.getValue();
				while(!toDestroy.isEmpty()) {
					toDestroy.poll().destroy(current);
				}
			}
			this.zombieFbos.clear();
			this.release();
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
	void notifyFboZombie(JoglContext owner, JoglFbo fbo) {
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
		surface.setDestroyed(); // flag it as destroyed so the Renderer won't use it
		synchronized(this.zombieWindowSurfaces) {
			this.zombieWindowSurfaces.add(surface);
		};
	}

	@Override
	public void runOnWorkerThread(Runnable runner) {
		if (Threading.isOpenGLThread())
			runner.run();
		else
			Threading.invokeOnOpenGLThread(runner);
	}
}
