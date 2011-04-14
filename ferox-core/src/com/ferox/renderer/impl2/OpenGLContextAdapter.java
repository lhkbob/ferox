package com.ferox.renderer.impl2;

import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.RenderCapabilities;

public abstract class OpenGLContextAdapter implements Context {
    private final GlslRenderer glslRenderer;
    private final FixedFunctionRenderer ffpRenderer;
    
    // FIXME: If the renderer's need the context for GL access it won't work
    // to make them arguments to this adapter
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
    
    public abstract RenderCapabilities getRenderCapabilities();

    public abstract void destroy();
    
    public abstract void makeCurrent();
    
    public abstract void release();
}
