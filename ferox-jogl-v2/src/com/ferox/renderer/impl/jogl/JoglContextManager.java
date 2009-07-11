package com.ferox.renderer.impl.jogl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;
import javax.media.opengl.TraceGL2;
import javax.media.opengl.TraceGL3;

import com.ferox.math.Transform;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.WindowSurface;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.ContextManager;
import com.ferox.renderer.impl.jogl.drivers.JoglTransformDriver;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
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
	private static final boolean TRACE = false;

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
	private final GLProfile profile;
	
	private final boolean debugGL;
	private final AbstractFramework renderer;
	private final JoglTransformDriver transformDriver;

	private final List<AttachableSurfaceGLEventListener> queuedRenderActions;

	private GLAutoDrawable currentDrawable;
	private JoglStateRecord currentRecord;
	private GL2ES2 currentGL;

	/**
	 * Construct a surface factory for the given renderer (this renderer must
	 * use this surface factory or undefined results will happen). If debug is
	 * true, opengl errors will be checked after each opengl call.
	 */
	public JoglContextManager(AbstractFramework renderer, RenderCapabilities caps, GLProfile profile, boolean debug) {
		if (renderer == null)
			throw new NullPointerException("Cannot create a surface factory with a null renderer");
		if (!profile.isGL2() && !profile.isGL3())
			throw new IllegalArgumentException("GLProfile must be one of GL2 or GL3, not: " + profile);
		
		this.profile = profile;
		this.renderer = renderer;

		debugGL = debug;
		transformDriver = new JoglTransformDriver(this);

		windowCreatedCount = 0;
		fullscreenCreated = false;

		activeSurfaces = new ArrayList<RenderSurface>();

		realizedSurfaces = new ArrayList<JoglRenderSurface>();
		queuedRenderActions = new ArrayList<AttachableSurfaceGLEventListener>();

		zombieFbos = new HashMap<GLAutoDrawable, Queue<JoglFbo>>();

		// create the shadow context
		if (caps.getPbufferSupport())
			shadowContext = new PbufferShadowContext(profile, caps);
		else
			shadowContext = new OnscreenShadowContext(profile, caps);

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
		s = new JoglFullscreenSurface(this, profile, options, width, height);
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
		s = new JoglWindowSurface(this, profile, options, x, y, width, height, resizable, undecorated);
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

		s = new JoglTextureSurface(this, profile, options, target, 
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

		s = new JoglTextureSurface(this, profile, (JoglTextureSurface) share, layer);
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
	public void runOnGraphicsThread(List<RenderSurface> queuedSurfaces, Runnable resourceAction) {
		int size = (queuedSurfaces == null ? 0 : queuedSurfaces.size());
		JoglRenderSurface s;
		for (int i = 0; i < size; i++) {
			s = (JoglRenderSurface) queuedSurfaces.get(i);
			if (s.getGLAutoDrawable() != null)
				queuedRenderActions.add(s);
		}

		if (queuedRenderActions.size() == 0)
			// we don't have any displaying surfaces to use, so
			// we use the shadow context
			queuedRenderActions.add(shadowContext);

		// assign the resource action
		queuedRenderActions.get(0).assignResourceAction(resourceAction);

		// attach all queued surfaces to the queued render actions
		int raIndex = 0;
		GLAutoDrawable queued;
		GLAutoDrawable realized = queuedRenderActions.get(raIndex).getGLAutoDrawable();

		for (int i = 0; i < size; i++) {
			s = (JoglRenderSurface) queuedSurfaces.get(i);
			queued = s.getGLAutoDrawable();
			if (queued != null && queued != realized) {
				// advance to next realized surface
				raIndex++;
				realized = queuedRenderActions.get(raIndex).getGLAutoDrawable();
			}

			// attach s to the current realized surface
			queuedRenderActions.get(raIndex).attachRenderSurface(s);
		}

		try {
			// render all the actions now
			AttachableSurfaceGLEventListener renderAction;
			size = queuedRenderActions.size();
			for (int i = 0; i < size; i++) {
				renderAction = queuedRenderActions.get(i);

				currentDrawable = renderAction.getGLAutoDrawable();
				currentRecord = renderAction.getStateRecord();

				GL gl = currentDrawable.getGL();
				if (gl.isGL2()) {
					if (TRACE)
						gl = new TraceGL2(gl.getGL2(), System.out);
					if (debugGL)
						gl = new DebugGL2(gl.getGL2());
					
					currentGL = gl.getGL2ES2();
				} else { // assume gl3
					if (TRACE)
						gl = new TraceGL3(gl.getGL3(), System.out);
					if (debugGL)
						gl = new DebugGL3(gl.getGL3());
					
					currentGL = gl.getGL2ES2();
				}

				renderAction.render();
			}
		} finally {
			// must clear the list even when an exception occurs
			queuedRenderActions.clear();
		}
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
	 * Returns the current, intended view transform in the modelview matrix.
	 * Because of the implementation of JoglTransformDriver, this may not
	 * actually be reflected in the opengl state. See JoglTransformDriver for
	 * more details.
	 */
	public Transform getViewTransform() {
		return transformDriver.getCurrentViewTransform();
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
	public GL2ES2 getGL() {
		return currentGL;
	}

	/**
	 * Return the context that represents the shadow context every realized
	 * surface must share its own context with.
	 */
	public GLContext getShadowContext() {
		return shadowContext.getContext();
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

	/* Utility class to cleanup fbos for a given GLAutoDrawable. */
	private class ZombieFboCleanupAction implements Runnable {
		private final Queue<JoglFbo> fbos;

		public ZombieFboCleanupAction(Queue<JoglFbo> toCleanup) {
			fbos = toCleanup;
		}

		@Override
		public void run() {
			while (!fbos.isEmpty())
				fbos.poll().destroy(currentGL);
		}
	}

	private void processZombieFbos() {
		if (zombieFbos.size() > 0) {
			ZombieFboCleanupAction cleanup;
			for (Entry<GLAutoDrawable, Queue<JoglFbo>> e : zombieFbos.entrySet()) {
				cleanup = new ZombieFboCleanupAction(e.getValue());
				runOnGraphicsThread(null, cleanup);
			}
			zombieFbos.clear();
		}
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
}
