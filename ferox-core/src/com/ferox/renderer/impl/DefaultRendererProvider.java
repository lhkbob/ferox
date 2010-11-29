package com.ferox.renderer.impl;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.RendererProvider;

public class DefaultRendererProvider implements RendererProvider {
    private GlslRenderer glslRenderer;
    private FixedFunctionRenderer ffpRenderer;
    
    private boolean selected;
    
    public DefaultRendererProvider(GlslRenderer glsl, FixedFunctionRenderer ffp) {
        if (glsl == null && ffp == null)
            throw new NullPointerException("Both renderers cannot be null");
        
        glslRenderer = glsl;
        ffpRenderer = ffp;
        
        selected = false;
    }
    
    @Override
    public GlslRenderer getGlslRenderer() {
        if (selected && glslRenderer == null)
            throw new IllegalStateException("FixedFunctionRenderer already in use");
        
        if (glslRenderer != null) {
            selected = true;
            ffpRenderer = null;
            return glslRenderer;
        } else
            return null;
    }

    @Override
    public FixedFunctionRenderer getFixedFunctionRenderer() {
        if (selected && ffpRenderer == null)
            throw new IllegalStateException("GlslRenderer already in use");
        
        if (ffpRenderer != null) {
            selected = true;
            glslRenderer = null;
            return ffpRenderer;
        } else
            return null;
    }

    @Override
    public boolean hasGlslRenderer() {
        return glslRenderer != null;
    }

    @Override
    public boolean hasFixedFunctionRenderer() {
        return ffpRenderer != null;
    }
}
