package com.ferox.renderer.impl;

import com.ferox.resource.Texture;

public abstract class TextureSurfaceDelegate {
    private final Texture[] colorTextures;
    private final Texture depthTexture;
    
    public TextureSurfaceDelegate(Texture[] colorTextures, Texture depthTexture) {
        this.colorTextures = (colorTextures == null ? new Texture[0] : colorTextures);
        this.depthTexture = depthTexture;
    }
    
    public Texture[] getColorBuffers() {
        return colorTextures;
    }
    
    public Texture getDepthBuffer() {
        return depthTexture;
    }
    
    public abstract void destroy();
    
    public abstract Context getContext();

    public abstract void setLayer(int layer, int depth);
    
    public abstract void flushLayer();
    
    public abstract void init();
    
    public abstract void preRender();
    
    public abstract void postRender(Action next);
}
