package com.ferox.renderer.impl;

import java.util.concurrent.locks.ReentrantLock;

import com.ferox.renderer.FrameStatistics;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.RendererProvider;

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
    private final AbstractGlslRenderer glsl;
    private final AbstractFixedFunctionRenderer ffp;
    protected final ReentrantLock lock;
    
    public Context(AbstractGlslRenderer glsl, AbstractFixedFunctionRenderer ffp, ReentrantLock surfaceLock) {
        if (glsl == null && ffp == null)
            throw new NullPointerException("Must provide at least one renderer");
        this.glsl = glsl;
        this.ffp = ffp;
        
        lock = (surfaceLock == null ? new ReentrantLock() : surfaceLock);
        frameStats = new ThreadLocal<FrameStatistics>();
    }
    
    /**
     * Convenience function to invoke {@link Renderer#reset()} on this Context's
     * renderers.
     */
    public void resetRenderers() {
        if (ffp != null)
            ffp.reset();
        if (glsl != null)
            glsl.reset();
    }

    /**
     * Convenience function to invoke
     * {@link AbstractRenderer#setSurfaceSize(int, int)} on this Context's
     * renderers.
     * 
     * @param width The width of the surface
     * @param height The height of the surface
     */
    public void setSurfaceSize(int width, int height) {
        if (ffp != null)
            ffp.setSurfaceSize(width, height);
        if (glsl != null)
            glsl.setSurfaceSize(width, height);
    }

    /**
     * Return a new RendererProvider wrapping this Context's renderers.
     * 
     * @return A RendererProvider ready to pass into a RenderPass
     */
    public RendererProvider getRendererProvider() {
        return new DefaultRendererProvider(glsl, ffp);
    }

    /**
     * Return the unique GlslRenderer instance associated with this Context.
     * 
     * @return This Context's GlslRenderer
     */
    public AbstractGlslRenderer getGlslRenderer() {
        return glsl;
    }
    
    /**
     * Return the unique FixedFunctionRenderer instance associated with this Context.
     * 
     * @return This Context's FixedFunctionRenderer
     */
    public AbstractFixedFunctionRenderer getFixedFunctionRenderer() {
        return ffp;
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
