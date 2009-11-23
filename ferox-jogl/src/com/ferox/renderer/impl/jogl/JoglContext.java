package com.ferox.renderer.impl.jogl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;

import com.ferox.math.Color4f;
import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.Context;
import com.ferox.renderer.impl.jogl.state.JoglStateRecord;

public class JoglContext implements Context {
	private static final ThreadLocal<JoglContext> current = new ThreadLocal<JoglContext>();
	private static final Map<JoglContext, Thread> contextThreads = new IdentityHashMap<JoglContext, Thread>();
	
	private final Renderer renderer;
	private final GLContext context;
	private final GLDrawable drawable;
	
	private final ReentrantLock contextLock;
	
	// FIXME: add a state record - must resolve state across gl profiles
	private final List<FramebufferObject> zombieFbos;
	
	private volatile boolean destroyed;
	
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

		contextLock = new ReentrantLock();
		zombieFbos = Collections.synchronizedList(new ArrayList<FramebufferObject>());

		this.context = context;
	}
	
	public static JoglContext getCurrent() {
		return current.get();
	}
	
	public static Thread getThread(JoglContext context) {
		return contextThreads.get(context);
	}
	
	@Override
	public void clearSurface(boolean clearColor, boolean clearDepth, boolean clearStencil, 
							 Color4f color, float depth, int stencil) {
		if (destroyed)
			return;
		// TODO implement once state record is added
		
	}

	@Override
	public Renderer getRenderer() {
		return renderer;
	}
	
	public JoglStateRecord getRecord() {
		
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
	}
	
	public void render(List<Action> batch, FrameStatistics stats) {
		Action a = null;
		JoglRenderSurface lastSurface = null;
		
		try {
			contextLock.lock();
			frameStats = stats;
			makeCurrent();
			
			// destroy any fbos that have been destroyed
			cleanupZombieFbos();
			
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
			
		} catch (RuntimeException t) {
			// clean up surface state
			if (!destroyed && lastSurface != null && a != lastSurface.getPostRenderAction())
				lastSurface.getPostRenderAction().perform(this, null);
			
			throw t;
		} finally {
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
	}
	
	private void releaseAndSwap() {
		context.release(); // FIXME: what happens if context isn't current?
		drawable.swapBuffers();
		
		current.set(null);
		contextThreads.remove(this);
	}
	
	private void cleanupZombieFbos() {
		int size = zombieFbos.size();
		for (int i = size - 1; i >= 0; i--) {
			zombieFbos.remove(i).destroy();
		}
	}
}
