package com.ferox.renderer;

public interface RendererProvider {
    public GlslRenderer getGlslRenderer();
    
    public FixedFunctionRenderer getFixedFunctionRenderer();
    
    public boolean hasGlslRenderer();
    
    public boolean hasFixedFunctionRenderer();
}
