package com.ferox.renderer.impl;

import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.RenderCapabilities;

/**
 * OpenGLContextAdapter is a wrapper around an OpenGL context that has been
 * created by some low-level OpenGL wrapper for Java (such as JOGL or LWJGL). An
 * OpenGL context can be current on a single thread at a time, and a context
 * must be current in order to perform graphics operations. This adapter
 * provides operations to make the context current and to release it. These
 * methods should not be used directly because contexts are carefully organized
 * by the {@link ContextManager}.
 * 
 * @author Michael Ludwig
 */
public abstract class OpenGLContextAdapter implements Context {
    private final GlslRenderer glslRenderer;
    private final FixedFunctionRenderer ffpRenderer;

    /**
     * Create a new OpenGLContextAdapter that will use the given fixed function
     * and glsl renderers. It is acceptable to provide both or only one of the
     * fixed function or glsl renderers. Both arguments cannot be null, at least
     * one renderer must be available for the context.
     * 
     * @param ffpRenderer The FixedFunctionRenderer implementation
     * @param glslRenderer The GlslRenderer implementation
     * @throws NullPointerException if both ffpRenderer and glslRenderer are
     *             null
     */
    public OpenGLContextAdapter(FixedFunctionRenderer ffpRenderer, GlslRenderer glslRenderer) {
        if (ffpRenderer == null && glslRenderer == null)
            throw new NullPointerException("Must provide at least one non-null renderer");
        this.glslRenderer = glslRenderer;
        this.ffpRenderer = ffpRenderer;
    }
    
    @Override
    public GlslRenderer getGlslRenderer() {
        return glslRenderer;
    }

    @Override
    public FixedFunctionRenderer getFixedFunctionRenderer() {
        return ffpRenderer;
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
