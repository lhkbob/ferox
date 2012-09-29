package com.ferox.renderer.impl;

import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.RenderCapabilities;

/**
 * RendererProvider provides Renderer implementations for {@link OpenGLContext
 * OpenGLContexts}. They do not need to worry about the selection of a single
 * renderer, as is the case with {@link Context}. This logic is handled by the
 * actual Context implementation used by {@link HardwareAccessLayerImpl}.
 * 
 * @author Michael Ludwig
 */
public interface RendererProvider {
    /**
     * Return the FixedFunctionRenderer to use. This does not need to worry
     * about whether or not a GlslRenderer has already been requested. This
     * should always return the same instance per context.
     * 
     * @param caps The current RenderCapabilities
     * @return The FixedFunctionRenderer to use, or null
     * @throws NullPointerException if caps is null
     */
    public FixedFunctionRenderer getFixedFunctionRenderer(RenderCapabilities caps);

    /**
     * Return the GlslRenderer to use. This does not need to worry about whether
     * or not a FixedFunctionRenderer has already been requested. This should
     * always return the same instance per context.
     * 
     * @param caps The current RenderCapabilities
     * @return The GlslRenderer to use, or null
     * @throws NullPointerException if caps is null
     */
    public GlslRenderer getGlslRenderer(RenderCapabilities caps);
}
