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

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.Context;
import com.ferox.renderer.impl.Sync;

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
	
	public JoglContext(JoglFramework framework, GLDrawable surface) {
		this(framework, surface, surface.createContext(framework.getShadowContext().getContext()));
	}
	
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
	
	public static JoglContext getCurrent() {
		return current.get();
	}
	
	public static Thread getThread(JoglContext context) {
		return contextThreads.get(context);
	}

	@Override
	public Renderer getRenderer() {
		return renderer;
	}
	
	public BoundObjectState getRecord() {
		return objState;
	}
	
	public FrameStatistics getCurrentStatistics() {
		return frameStats;
	}
	
	public ReentrantLock getLock() {
		return contextLock;
	}
	
	// Caller must have acquired lock via getLock()
	public void destroy() {
		if (destroyed)
			return;
		
		if (current.get() == this)
			releaseAndSwap();
		
		context.destroy();
		destroyed = true;
		log.log(Level.FINE, "JoglContext destroyed", this);
	}
	
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
			
			getGL().glFlush();
		} catch (RuntimeException t) {
			// clean up surface state
			if (!destroyed && lastSurface != null && a != lastSurface.getPostRenderAction())
				lastSurface.getPostRenderAction().perform(this, null);
			
			throw t;
		} finally {
			renderer.reset();
			
			releaseAndSwap();
			frameStats = null;
			contextLock.unlock();
		}
	}
	
	public GLDrawable getDrawable() {
		return drawable;
	}
	
	public GLContext getContext() {
		return context;
	}
	
	public GL2GL3 getGL() {
		return context.getGL().getGL2GL3();
	}
	
	public GL2 getGL2() {
		return context.getGL().getGL2();
	}
	
	public GL3 getGL3() {
		return context.getGL().getGL3();
	}
	
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
	
	private void releaseAndSwap() {
		drawable.swapBuffers();
		release();
	}
	
	private void cleanupZombieFbos() {
		int size = zombieFbos.size();
		for (int i = size - 1; i >= 0; i--) {
			zombieFbos.remove(i).destroy();
		}
	}
}
