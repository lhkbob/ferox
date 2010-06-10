package com.ferox.renderer.impl;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Renderer;

/**
 * Context represents an OpenGL context. When a context is current, OpenGL calls
 * can be made on the calling Thread. It is left up to implementations to
 * specify this behavior and manage it properly. The only assertion that Context
 * makes is that it has a single {@link Renderer} that can be used for
 * RenderPasses.
 * 
 * @author Michael Ludwig
 */
public abstract class Context {
    private static final ThreadLocal<Context> current = new ThreadLocal<Context>();
    
    private final ThreadLocal<FrameStatistics> frameStats;
    private volatile Thread currentThread;
    
    public Context() {
        frameStats = new ThreadLocal<FrameStatistics>();
    }
    
	/**
	 * Return the unique Renderer instance associated with this Context.
	 * 
	 * @return This Context's Renderer
	 */
	public abstract AbstractRenderer getRenderer();

    /**
     * Invoke the given Runnable within a valid lock, and with its underlying
     * OpenGL context current. The lock should be released when this is
     * completed. If necessary this can invoke the Runnable on a different
     * Thread, and should block until it's completed.
     * 
     * @param run The Runnable to invoke
     */
	public abstract void runWithLock(Runnable run);
	
	/**
     * @return A ThreadLocal FrameStatistics instance last assigned via
     *         {@link #setFrameStatistics(FrameStatistics)}
     */
	public FrameStatistics getFrameStatistics() {
	    return frameStats.get();
	}

    /**
     * Assign <tt>stats</tt> to this Context's thread local current
     * FrameStatistics, for use by Renderer implementations to update geometry
     * counts. The RenderManager is responsible for actually measuring the
     * timing, however.
     * 
     * @param stats The new FrameStatistics, may be null
     */
	public void setFrameStatistics(FrameStatistics stats) {
	    frameStats.set(stats);
	}
	
	/**
     * @return The Thread this Context is current on, or null if it is not
     *         active
     */
	public Thread getContextThread() {
	    return currentThread;
	}
	
	/**
     * @return The Context that is current on the calling Thread, or null if no
     *         Context is current
     */
	public static Context getCurrent() {
	    return current.get();
	}
	
	protected void notifyCurrent() {
	    current.set(this);
	    currentThread = Thread.currentThread();
	}
	
	protected void notifyRelease() {
	    currentThread = null;
	    current.set(null);
	}
}
