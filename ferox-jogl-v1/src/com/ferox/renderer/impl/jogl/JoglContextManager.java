package com.ferox.renderer.impl.jogl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import javax.media.opengl.DebugGL;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.TraceGL;

import com.ferox.math.Color4f;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.WindowSurface;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.ContextManager;
import com.ferox.renderer.impl.jogl.drivers.JoglTransformDriver;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * <p>
 * The JoglContextManager provides an implementation of ContextManager for use
 * with jogl based renderers. Its internals with context management are
 * inherently not thread safe and should not be used from multiple threads.
 * </p>
 * <p>
 * This was chosen because it greatly simplified implementation and a surface
 * factory should only be used internally by an AbstractFramework, which imposes
 * single threadedness anyway.
 * </p>
 * <p>
 * This class is public so that new jogl based renderers can be more easily
 * developed (e.g. to replace the drivers being used by implementing smarter
 * driver factories). Even so, it should only be created inside a constructor of
 * a renderer implementation and used only be that renderer. Otherwise undefined
 * and dangerous behavior WILL occur.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class JoglContextManager implements ContextManager {
	private static final boolean TRACE = true;

	/* Variables for the shadow context. */
	private ShadowContext shadowContext;

	/* Variables for surfaces. */
	private final List<RenderSurface> activeSurfaces;
	private int windowCreatedCount;
	private boolean fullscreenCreated;

	private final Map<GLAutoDrawable, Queue<JoglFbo>> zombieFbos;
	private final Object destroyLock = new Object();

	private final List<JoglRenderSurface> realizedSurfaces;

	/* Misc variables. */
	private final boolean debugGL;
	private final AbstractFramework renderer;
	private final JoglTransformDriver transformDriver;

	private final ActionRunner actionRunner;
	
	private GLAutoDrawable currentDrawable;
	private JoglStateRecord currentRecord;
	private GL currentGL;

	/**
	 * Construct a surface factory for the given renderer (this renderer must
	 * use this surface factory or undefined results will happen). If debug is
	 * true, opengl errors will be checked after each opengl call.
	 */
	public JoglContextManager(AbstractFramework renderer, RenderCapabilities caps, boolean debug) {
		if (renderer == null)
			throw new NullPointerException("Cannot create a surface factory with a null renderer");

		this.renderer = renderer;
		debugGL = debug;
		transformDriver = new JoglTransformDriver(this);

		windowCreatedCount = 0;
		fullscreenCreated = false;

		activeSurfaces = new ArrayList<RenderSurface>();
		realizedSurfaces = new ArrayList<JoglRenderSurface>();

		zombieFbos = new HashMap<GLAutoDrawable, Queue<JoglFbo>>();
		actionRunner = new ActionRunner();
		
		// create the shadow context
		if (caps.getPbufferSupport())
			shadowContext = new PbufferShadowContext(caps);
		else
			shadowContext = new OnscreenShadowContext(caps);
	}

	/** Get the renderer that uses this factory. */
	public AbstractFramework getFramework() {
		return renderer;
	}

	@Override
	public FullscreenSurface createFullscreenSurface(DisplayOptions options, int width, int height) {
		if (windowCreatedCount > 0)
			throw new SurfaceCreationException("Cannot create a FullscreenSurface when there are created WindowSurfaces");

		JoglFullscreenSurface s = null;
		s = new JoglFullscreenSurface(this, options, width, height);
		activeSurfaces.add(s);

		fullscreenCreated = true;
		if (s.getGLAutoDrawable() != null)
			realizedSurfaces.add(s);
		return s;
	}

	@Override
	public WindowSurface createWindowSurface(DisplayOptions options, 
										     int x, int y, int width, int height, 
										     boolean resizable, boolean undecorated) {
		if (fullscreenCreated)
			throw new SurfaceCreationException("Cannot create a WindowSurface when there is already a FullscreenSurface");

		JoglWindowSurface s = null;
		s = new JoglWindowSurface(this, options, x, y, width, height, resizable, undecorated);
		activeSurfaces.add(s);

		windowCreatedCount++;
		if (s.getGLAutoDrawable() != null)
			realizedSurfaces.add(s);
		return s;
	}

	@Override
	public TextureSurface createTextureSurface(DisplayOptions options, TextureTarget target, 
											   int width, int height, int depth, int layer,
											   int numColorTargets, boolean useDepthRenderBuffer) {
		JoglTextureSurface s = null;

		s = new JoglTextureSurface(this, options, target, 
								   width, height, depth, layer, 
								   numColorTargets, useDepthRenderBuffer);
		activeSurfaces.add(s);

		if (s.getGLAutoDrawable() != null)
			realizedSurfaces.add(s);
		return s;
	}

	@Override
	public TextureSurface createTextureSurface(TextureSurface share, int layer) {
		JoglTextureSurface s = null;

		s = new JoglTextureSurface(this, (JoglTextureSurface) share, layer);
		activeSurfaces.add(s);

		if (s.getGLAutoDrawable() != null)
			realizedSurfaces.add(s);
		return s;
	}

	/** This implementation can be called from multiple threads. */
	@Override
	public void destroy(RenderSurface surface) {
		synchronized (destroyLock) {
			JoglRenderSurface d = (JoglRenderSurface) surface;
			d.destroySurface();
			relinquishSurface(d);
		}
	}

	@Override
	public void destroy() {
		processZombieFbos();

		if (realizedSurfaces.size() > 0)
			// this shouldn't happen, but we don't want a memory leak
			for (int i = realizedSurfaces.size(); i >= 0; i--)
				this.destroy(realizedSurfaces.get(i));

		shadowContext.destroy();
	}
	
	@Override
	public void runOnGraphicsThread(Action actions) {
		if (actions == null)
			return; // nothing to do
		actionRunner.run(actions);
	}
	
	@Override
	public void clear(RenderSurface surface, boolean cColor, boolean cDepth, boolean cStencil) {
		JoglRenderSurface s = (JoglRenderSurface) surface;
		FramebufferRecord fr = s.getStateRecord().frameRecord;
		PixelOpRecord pr = s.getStateRecord().pixelOpRecord;

		GL gl = getGL();

		// disable this to make sure color clearing happens over whole surface
		gl.glDisable(GL.GL_SCISSOR_TEST);
		pr.enableScissorTest = false;

		Color4f clearColor = s.getClearColor();
		if (!clearColor.equals(fr.clearColor)) {
			gl.glClearColor(clearColor.getRed(), clearColor.getGreen(), clearColor.getBlue(), clearColor.getAlpha());
			JoglUtil.get(clearColor, fr.clearColor);
		}
		float clearDepth = s.getClearDepth();
		if (fr.clearDepth != clearDepth) {
			gl.glClearDepth(clearDepth);
			fr.clearDepth = clearDepth;
		}
		int clearStencil = s.getClearStencil();
		if (fr.clearStencil != clearStencil) {
			gl.glClearStencil(clearStencil);
			fr.clearStencil = clearStencil;
		}

		int clearBits = 0;
		if (cColor && s.hasColorBuffer())
			clearBits |= GL.GL_COLOR_BUFFER_BIT;
		if (cDepth && s.hasDepthBuffer())
			clearBits |= GL.GL_DEPTH_BUFFER_BIT;
		if (cStencil && s.hasStencilBuffer())
			clearBits |= GL.GL_STENCIL_BUFFER_BIT;

		if (clearBits != 0)
			gl.glClear(clearBits);
	}

	@Override
	public boolean isGraphicsThread() {
		return GLContext.getCurrent() != null;
	}

	@Override
	public List<RenderSurface> getActiveSurfaces() {
		return activeSurfaces;
	}

	/**
	 * Return the GLAutoDrawable that is currently executing its
	 * GLEventListeners. This is not necessarily associated with the actual
	 * "current" surface, since fbos can be attached to a drawable to get a
	 * valid context.
	 */
	public GLAutoDrawable getDisplayingDrawable() {
		return currentDrawable;
	}

	/** Return the transform driver used by this factory's renderer. */
	public JoglTransformDriver getTransformDriver() {
		return transformDriver;
	}

	/**
	 * Returns the current state record, which will only be not null when a
	 * JoglRenderSurface is having its display() method invoked.
	 */
	public JoglStateRecord getRecord() {
		return currentRecord;
	}

	/**
	 * Return the GL instance that is associated with the current context. This
	 * must only be called from within a method in the stack of a display() call
	 * of a JoglRenderSurface.
	 */
	public GL getGL() {
		return currentGL;
	}

	/**
	 * Return the context that represents the shadow context every realized
	 * surface must share its own context with.
	 */
	public GLContext getShadowContext() {
		return shadowContext.getContext();
	}
	
	/**
	 * Should only be called when the given fbo should be scheduled for
	 * destruction. At the next possible time, the fbo will have its destroy()
	 * method called on the owner context.
	 */
	void notifyFboZombie(GLAutoDrawable owner, JoglFbo fbo) {
		Queue<JoglFbo> queue = zombieFbos.get(owner);
		if (queue == null) {
			queue = new LinkedList<JoglFbo>();
			zombieFbos.put(owner, queue);
		}
		queue.add(fbo);
	}
	
	private void processZombieFbos() {
		if (zombieFbos.size() > 0) {
			for (Entry<GLAutoDrawable, Queue<JoglFbo>> e : zombieFbos.entrySet()) {
				if (e.getKey() == shadowContext.getGLAutoDrawable()) {
					// shadow context
					runOnGraphicsThread(new ZombieFboCleanupAction(null, e.getValue()));
				} else {
					// look-up the associated JoglRenderSurface
					for (RenderSurface s: activeSurfaces) {
						if (((JoglRenderSurface) s).getGLAutoDrawable() == e.getKey()) {
							runOnGraphicsThread(new ZombieFboCleanupAction(s, e.getValue()));
							break;
						}
					}
				}
			}
			zombieFbos.clear();
		}
	}

	private void relinquishSurface(JoglRenderSurface surface) {
		GLAutoDrawable c = surface.getGLAutoDrawable();
		if (c != null) {
			realizedSurfaces.remove(surface);
			zombieFbos.remove(c); // the fbos are automatically destroyed
		}

		if (surface instanceof JoglWindowSurface)
			windowCreatedCount--;
		else if (surface instanceof JoglFullscreenSurface)
			fullscreenCreated = false;

		activeSurfaces.remove(surface);
	}
	
	private class ActionRunner {
		private Action batchHead;
		private Action batchTail;
		
		public void run(Action actions) {
			batchHead = null;
			batchTail = null;
			FrameworkGLEventListener batchOwner = null;
			JoglRenderSurface lastSurface = null; // may not be batchOwner
			
			JoglRenderSurface s;
			while(actions != null) {
				s = (JoglRenderSurface) actions.getRenderSurface();
				if (s == null || s == lastSurface) {
					// just add it
					add(actions);
				} else {
					// add post action for last surface if not null
					if (lastSurface != null)
						add(lastSurface.getPostRenderAction());
					
					if (s.getGLAutoDrawable() != null && batchOwner != s) {
						// run the current batch
						if (batchHead != null && batchOwner != null)
							runBatch(batchOwner, actions);
						
						batchOwner = s;
					} 
					
					// start a new surface
					add(s.getPreRenderAction());
					add(actions);
				}
				
				lastSurface = s;
				actions = actions.next();
			}
			
			if (batchHead != null) {
				add(lastSurface.getPostRenderAction());
				runBatch(batchOwner, null);
			}
		}
		
		private void add(Action a) {
			if (batchTail != null)
				batchTail.setNext(a);
			batchTail = a;
			if (batchHead == null)
				batchHead = a;
		}
		
		private void runBatch(FrameworkGLEventListener batchOwner, Action next) {
			if (batchOwner == null)
				batchOwner = shadowContext;
			
			batchTail.setNext(null);
			
			currentDrawable = batchOwner.getGLAutoDrawable();
			currentRecord = batchOwner.getStateRecord();
			currentGL = (debugGL ? (TRACE ? new TraceGL(currentDrawable.getGL(), System.out) 
										  : new DebugGL(currentDrawable.getGL())) 
							     : currentDrawable.getGL());
			batchOwner.render(batchHead);
			
			batchHead = null;
			batchTail = null;
		}
	}

	/* Utility class to cleanup fbos for a given GLAutoDrawable. */
	private class ZombieFboCleanupAction extends Action {
		private final Queue<JoglFbo> fbos;

		public ZombieFboCleanupAction(RenderSurface fboOwner, Queue<JoglFbo> toCleanup) {
			super(fboOwner);
			fbos = toCleanup;
		}

		@Override
		public void perform() {
			while (!fbos.isEmpty())
				fbos.poll().destroy(currentGL);
		}
	}
}
