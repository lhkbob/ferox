package com.ferox.renderer.impl.jogl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.glu.GLU;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.Context;
import com.ferox.renderer.impl.RenderInterruptedException;
import com.ferox.renderer.impl.Sync;

/**
 * <p>
 * JoglContext is a complete implementation of Context for use with the
 * {@link JoglFramework}. Each JoglContext is associated with a single
 * GLDrawable. These GLDrawables are created by RenderSurfaces or by the
 * JoglFramework to facilitate context sharing.
 * </p>
 * <p>
 * A JoglContext should not be created directly
 * </p>
 * 
 * @author Michael Ludwig
 */
public class JoglContext implements Context {
	private static final Logger log = Logger.getLogger(JoglFramework.class.getPackage().getName());
	
	private static final ThreadLocal<JoglContext> current = new ThreadLocal<JoglContext>();
	private static final Map<JoglContext, Thread> contextThreads = new IdentityHashMap<JoglContext, Thread>();
	
	private final Renderer renderer;
	private final GLContext context;
	private final GLDrawable drawable;
	
	private final BoundObjectState objState;
	private final List<FramebufferObject> zombieFbos;
	
	private volatile boolean destroyed;
	private boolean initialized;
	private final ReentrantLock contextLock;
	
	private FrameStatistics frameStats;

	/**
	 * Create a new JoglContext that is intended for the given JoglFramework and
	 * is tied to the given GLDrawable. A new GLContext is created for the
	 * surface that is shared with the framework's shadow context. If surface is
	 * a GLAutoDrawable, it should have its GLContext updated after the
	 * JoglContext is created.
	 * 
	 * @param framework The JoglFramework this JoglContext is used with
	 * @param surface The GLDrawable that this JoglContext wraps
	 * @throws NullPointerException if framework or surface are null
	 */
	public JoglContext(JoglFramework framework, GLDrawable surface) {
		this(framework, surface, surface.createContext(framework.getShadowContext().getContext()));
	}

	/**
	 * Protected constructor that allows the the specification of a GLContext.
	 * It is assumed that the GLContext is associated to the GLDrawable. This
	 * constructor is intended for shadow context creation when there is no
	 * other context to share with yet.
	 * 
	 * @param framework The Framework that this is for
	 * @param surface The GLDrawable wrapped by this JoglContext
	 * @param context The explicit GLContext to use for the surface
	 * @throws NullPointerException if any arguments are null
	 */
	protected JoglContext(JoglFramework framework, GLDrawable surface, GLContext context) {
		if (surface == null || framework == null || context == null)
			throw new NullPointerException("Cannot specify a null Framework, GLDrawable, or GLContext");
		renderer = framework.createRenderer(this);
		drawable = surface;
		destroyed = false;
		initialized = false;
		
		int ffp = framework.getCapabilities().getMaxFixedPipelineTextures();
		int frag = framework.getCapabilities().getMaxFragmentShaderTextures();
		int vert = framework.getCapabilities().getMaxVertexShaderTextures();
		
		int maxTextures = Math.max(ffp, Math.max(frag, vert));
		objState = new BoundObjectState(maxTextures);

		contextLock = new ReentrantLock();
		zombieFbos = Collections.synchronizedList(new ArrayList<FramebufferObject>());

		this.context = context;
		log.log(Level.FINE, "JoglContext created for " + surface.getClass().getSimpleName(), this);
	}

	/**
	 * Return the JoglContext that is current on the calling thread. If no
	 * JoglContext is current, then null is returned. When a non-null
	 * JoglContext is returned, it is acceptable to invoke GL methods.
	 * 
	 * @return The current JoglContext
	 */
	public static JoglContext getCurrent() {
		return current.get();
	}

	/**
	 * Return the Thread that the given context is current on. If the context is
	 * not current, then a null Thread is returned. It is assumed that context
	 * is not null.
	 * 
	 * @param context The JoglContext to check
	 * @return The Thread that context is current on
	 */
	public static Thread getThread(JoglContext context) {
		return contextThreads.get(context);
	}

	@Override
	public Renderer getRenderer() {
		return renderer;
	}
	
	/**
	 * @return The BoundObjectState used by this JoglContext
	 */
	public BoundObjectState getRecord() {
		return objState;
	}

	/**
	 * @return The FrameStatistics that are currently being used to track a
	 *         frame that's in the progress of rendering
	 */
	public FrameStatistics getCurrentStatistics() {
		return frameStats;
	}
	
	/**
	 * @return The lock that must be acquired before the JoglContext can be
	 *         destroyed
	 */
	public ReentrantLock getLock() {
		return contextLock;
	}

	/**
	 * Destroy this JoglContext. This does nothing if the context has already
	 * been destroyed. If the context is current, then it is released. The
	 * caller must have already acquired the context's lock via
	 * {@link #getLock()}.
	 */
	public void destroy() {
		if (destroyed)
			return;
		
		if (current.get() == this) {
			drawable.swapBuffers();
			release();
		}
		
		context.destroy();
		destroyed = true;
		log.log(Level.FINE, "JoglContext destroyed", this);
	}

	/**
	 * Invoke the run() method of the given Sync while this JoglContext is
	 * current. The context will be released after this is invoked.
	 * 
	 * @param sync The Sync to run
	 * @throws NullPointerException if sync is null
	 */
	public void runSync(Sync<?> sync) {
		try {
			contextLock.lock();
			makeCurrent();
			sync.run();
		} finally {
			release();
			contextLock.unlock();
		}
	}

	/**
	 * Render all of the Actions that are present in batch, and keep track of
	 * statistics within stats. The FrameStatistics should only have its mesh
	 * counts updated, timing is handled by the JoglRenderManager. It can be
	 * assumed that all Actions within this batch are for the surface attached
	 * to this JoglContext, or have a null RenderSurface.
	 * 
	 * @param batch The batch of Actions to render
	 * @param stats The FrameStatistics used track Geometry counts
	 */
	public void render(List<Action> batch, FrameStatistics stats) {
		Action a = null;
		JoglRenderSurface lastSurface = null;
		
		try {
			contextLock.lock();
			frameStats = stats;
			makeCurrent();
			
			int size = batch.size();
			for (int i = 0; i < size && !destroyed; i++) {
				a = batch.get(i);
				if (a.getRenderSurface() != null)
					lastSurface = (JoglRenderSurface) a.getRenderSurface();
				
				// perform each action
				synchronized(lastSurface.getLock()) {
					a.perform(this, (i < size - 1 ? batch.get(i + 1) : null));
				}
				if (Thread.interrupted())
					throw new RenderInterruptedException();
			}
			
			GL2GL3 gl = getGL();
			gl.glFlush();
			int error = gl.glGetError();
			if (error != 0)
				throw new RenderException("OpenGL error reported while rendering: " + error + " " + new GLU().gluErrorString(error));
		} catch (RuntimeException t) {
			// clean up surface state
			if (!destroyed && lastSurface != null && a != lastSurface.getPostRenderAction())
				lastSurface.getPostRenderAction().perform(this, null);
			
			throw t;
		} finally {
			// we assume here that something drastic hasn't happened, like a card melt
			renderer.reset();
			drawable.swapBuffers();
			release();
			
			frameStats = null;
			contextLock.unlock();
		}
	}
	
	/**
	 * @return The GLDrawable attached to this JoglContext
	 */
	public GLDrawable getDrawable() {
		return drawable;
	}
	
	/**
	 * @return The actual GLContext wrapped by this JoglContext
	 */
	public GLContext getContext() {
		return context;
	}
	
	/**
	 * @return A GL2GL3 instance associated with this JoglContext
	 */
	public GL2GL3 getGL() {
		return context.getGL().getGL2GL3();
	}

	/**
	 * @return A GL2 instance for this context. This assumes that GL2 is
	 *         supported by the framework's profile
	 */
	public GL2 getGL2() {
		return context.getGL().getGL2();
	}

	/**
	 * @return A GL3 instance for this context. This assumes that GL3 is
	 *         supported by the framework's profile
	 */
	public GL3 getGL3() {
		return context.getGL().getGL3();
	}

	/**
	 * Notify the context that a FramebufferObject should be destroyed on this
	 * context the next time it's made current. Assumes that the fbo is not
	 * null, hasn't been destroyed, and will no longer be used.
	 * 
	 * @param fbo The fbo to destroy eventually
	 */
	void notifyFboZombie(FramebufferObject fbo) {
		zombieFbos.add(fbo);
	}
	
	private void makeCurrent() {
		int res = context.makeCurrent();
		if (res == GLContext.CONTEXT_NOT_CURRENT)
			throw new RenderException("Unable to make GLContext current");
		
		current.set(this);
		contextThreads.put(this, Thread.currentThread());
		
		if (!initialized) {
			initialized = true;
			GL2GL3 gl = getGL();
			
			// make some state assumptions valid
			gl.glEnable(GL.GL_DEPTH_TEST);
			gl.glEnable(GL.GL_CULL_FACE);
			
			if (gl.isGL2()) {
				// additional state for fixed-pipeline
				GL2 gl2 = gl.getGL2();
				gl2.glColorMaterial(GL.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE);
				gl2.glEnable(GL2.GL_COLOR_MATERIAL);
			}
		}
		
		// destroy any fbos that have been destroyed
		cleanupZombieFbos();
		
		log.log(Level.FINER, "Context made current", this);
	}
	
	private void release() {
		context.release();
		current.set(null);
		contextThreads.remove(this);
		
		log.log(Level.FINER, "Context released", this);
	}
	
	private void cleanupZombieFbos() {
		int size = zombieFbos.size();
		for (int i = size - 1; i >= 0; i--) {
			zombieFbos.remove(i).destroy();
		}
	}
}
