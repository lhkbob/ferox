package com.ferox.renderer.impl;

import com.ferox.renderer.RenderCapabilities;

/**
 * OpenGLContext is a wrapper around an OpenGL context that has been
 * created by some low-level OpenGL wrapper for Java (such as JOGL or LWJGL). An
 * OpenGL context can be current on a single thread at a time, and a context
 * must be current in order to perform graphics operations. This adapter
 * provides operations to make the context current and to release it. These
 * methods should not be used directly because contexts are carefully organized
 * by the {@link ContextManager}.
 * 
 * @author Michael Ludwig
 */
public abstract class OpenGLContext {
    private final RendererProvider rendererProvider;

    /**
     * Create a new OpenGLContext that will use the given RendererProvider.
     * 
     * @param provider The RendererProvider to use when the context has been
     *            activated
     * @throws NullPointerException if provider is null
     */
    public OpenGLContext(RendererProvider provider) {
        if (provider == null) {
            throw new NullPointerException("RendererProvider cannot be null");
        }
        rendererProvider = provider;
    }

    /**
     * @return The RendererProvider for this context
     */
    public RendererProvider getRendererProvider() {
        return rendererProvider;
    }

    /**
     * <p>
     * Determine the RenderCapabilities of the current context. Since a
     * Framework will have multiple contexts, these capabilities should be the
     * same for created contexts running in the same process since the hardware
     * will be the same.
     * </p>
     * <p>
     * Implementations can assume that the context is current on the calling
     * thread.
     * </p>
     * 
     * @return A new RenderCapabilities
     */
    public abstract RenderCapabilities getRenderCapabilities();

    /**
     * Destroy this context. If the context is not shared with any other
     * un-destroyed context, any graphics resources that would be shared can be
     * cleaned as well. This must be called in a thread-safe environment (such
     * as {@link AbstractSurface#destroyImpl()}) and the context should not be
     * current on any thread.
     */
    public abstract void destroy();

    /**
     * Make the context current on the calling thread. This must be called in a
     * thread-safe environment, and generally
     * {@link ContextManager#ensureContext()} should be used instead. It is
     * assumed that the context is not current on any other thread, and there is
     * no other context already current on this thread.
     */
    public abstract void makeCurrent();

    /**
     * Release this context from the calling thread. It is assumed that the
     * context is current on this thread.This must be called in a thread-safe
     * environment and should usually be left up to {@link ContextManager} to
     * manage (so use {@link ContextManager#forceRelease(AbstractSurface)}
     * instead).
     */
    public abstract void release();
}
