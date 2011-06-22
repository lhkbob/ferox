package com.ferox.renderer.pass;

import com.ferox.renderer.GlslRenderer;

public abstract class GlslRenderPass implements RenderPass<GlslRenderer> {
    @Override
    public Class<GlslRenderer> getRendererType() {
        return GlslRenderer.class;
    }
}
