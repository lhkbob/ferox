package com.ferox.renderer.impl;

import java.util.concurrent.locks.ReentrantLock;

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
    private final AbstractRenderer renderer;
    protected final ReentrantLock lock;
    
    public Context(AbstractRenderer renderer, ReentrantLock surfaceLock) {
        if (renderer == null)
            throw new NullPointerException("Renderer cannot be null");
        this.renderer = renderer;
        lock = (surfaceLock == null ? new ReentrantLock() : surfaceLock);
        frameStats = new ThreadLocal<FrameStatistics>();
    }
    
    /**
     * Return the unique Renderer instance associated with this Context.
     * 
     * @return This Context's Renderer
     */
    public AbstractRenderer getRenderer() {
        return renderer;
    }

    /**
     * Invoke the given Runnable within a valid lock, and with its underlying
     * OpenGL context current. The lock should be released when this is
     * completed. If necessary this can invoke the Runnable on a different
     * Thread, and should block until it's completed.
     * 
     * @param run The Runnable to invoke
     */
    public void runWithLock(Runnable run) {
        lock.lock();
        try {
            makeCurrent();
            run.run();
            release();
        } finally {
            lock.unlock();
        }
    }
    
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
     * @return The Context that is current on the calling Thread, or null if no
     *         Context is current
     */
    public static Context getCurrent() {
        return current.get();
    }
    
    protected void makeCurrent() {
        current.set(this);
    }
    
    protected void release() {
        if (current.get() == this)
            current.set(null);
    }
}
