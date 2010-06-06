package com.ferox.renderer.impl;

import com.ferox.renderer.RenderPass;

public class TextureRenderPassAction extends RenderPassAction {
    private final int activeLayer;
    private final int activeDepth;
    
    public TextureRenderPassAction(AbstractTextureSurface surface, RenderPass pass, int activeLayer, int activeDepth) {
        super(surface, pass);
        this.activeLayer = activeLayer;
        this.activeDepth = activeDepth;
    }
    
    @Override
    public void perform(Context context, Action next) {
        AbstractTextureSurface surface = (AbstractTextureSurface) getSurface();
        surface.setRenderLayer(activeLayer, activeDepth);
        
        super.perform(context, next);
    }
}
