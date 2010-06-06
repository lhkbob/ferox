package com.ferox.renderer.impl;

import com.ferox.renderer.Framework;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;

public abstract class AbstractTextureSurface extends AbstractSurface implements TextureSurface {
    private int activeLayer;
    private int activeDepth;
    
    private int renderLayer;
    private int renderDepth;
    private boolean passRendered;
    
    private final TextureSurfaceDelegate delegate;
    private final TextureSurfaceOptions options;
    
    public AbstractTextureSurface(Framework framework, TextureSurfaceOptions options) {
        super(framework);
        
    }

    /**
     * Notify the AbstractTextureSurface of the layer and depth it is to render
     * the next pass into. Validation is performed on the layer and depth
     * arguments, but it is assumed that this is invoked within the paired
     * invocations of the pre/post actions of the AbstractSurface.
     * 
     * @param layer The layer to render into
     * @param depth The depth to render into
     * @throws IllegalArgumentException if layer or depth are invalid
     */
    public void setRenderLayer(int layer, int depth) {
        if (layer < 0 || layer >= getNumLayers())
            throw new IllegalArgumentException("Invalid layer");
        if (depth < 0 || depth >= getDepth())
            throw new IllegalArgumentException("Invalid depth");
        
        if (layer != renderLayer || depth != renderDepth) {
            if (passRendered)
                delegate.flushLayer();
            delegate.setLayer(layer, depth);
        }
        
        renderLayer = layer;
        renderDepth = depth;
        passRendered = true;
    }
    
    @Override
    public Context getContext() {
        return delegate.getContext();
    }
    
    @Override
    protected void init() {
        delegate.init();
    }

    @Override
    protected void postRender(Action next) {
        if (passRendered)
            delegate.flushLayer();
        delegate.postRender(next);
        passRendered = false;
    }

    @Override
    protected void preRender() {
        delegate.preRender();
    }
    
    @Override
    protected void destroyImpl() {
        delegate.destroy();
    }
    
    @Override
    public TextureSurfaceOptions getOptions() {
        return options;
    }

    @Override
    public int getActiveDepthPlane() {
        return activeDepth;
    }

    @Override
    public int getActiveLayer() {
        return activeLayer;
    }

    @Override
    public Texture getColorBuffer(int buffer) {
        return delegate.getColorBuffers()[buffer];
    }

    @Override
    public Texture getDepthBuffer() {
        return delegate.getDepthBuffer();
    }

    @Override
    public int getNumColorBuffers() {
        return delegate.getColorBuffers().length;
    }

    @Override
    public Target getTarget() {
        return options.getTarget();
    }

    @Override
    public void setActiveDepthPlane(int depth) {
        if (depth < 0 || depth >= getDepth())
            throw new IllegalArgumentException("Active depth is invalid: " + depth);
        activeDepth = depth;
    }

    @Override
    public void setActiveLayer(int layer) {
        if (layer < 0 || layer >= getNumLayers())
            throw new IllegalArgumentException("Active layer is invalid: " + layer);
        activeLayer = layer;
    }

    @Override
    public int getHeight() {
        return options.getHeight();
    }

    @Override
    public int getWidth() {
        return options.getWidth();
    }
    
    @Override
    public int getDepth() {
        return options.getDepth();
    }

    @Override
    public boolean hasColorBuffer() {
        return delegate.getColorBuffers().length > 0;
    }

    @Override
    public boolean hasDepthBuffer() {
        return delegate.getDepthBuffer() != null;
    }

    @Override
    public boolean hasStencilBuffer() {
        return false;
    }

    @Override
    public int getNumLayers() {
        if (getTarget() == Target.T_CUBEMAP)
            return 6;
        else
            return 1;
    }
}
