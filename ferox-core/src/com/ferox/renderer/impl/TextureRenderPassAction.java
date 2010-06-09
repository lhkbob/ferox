package com.ferox.renderer.impl;

import com.ferox.renderer.RenderPass;

/**
 * TextureRenderPassAction is a {@link RenderPassAction} that when
 * invoked will also notify the AbstractTextureSurface of the
 * requested depth plane and layer before executing the RenderPass.
 * @author Michael Ludwig
 *
 */
public class TextureRenderPassAction extends RenderPassAction {
    private final int activeLayer;
    private final int activeDepth;

    /**
     * Create a new TextureRenderPassAction. It is assumed that activeLayer and
     * activeDepth are already valid.
     * 
     * @param surface
     * @param pass
     * @param activeLayer
     * @param activeDepth
     */
    public TextureRenderPassAction(AbstractTextureSurface surface, RenderPass pass, 
                                   int activeLayer, int activeDepth) {
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
