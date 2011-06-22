package com.ferox.renderer.pass;

import com.ferox.renderer.FixedFunctionRenderer;

public abstract class FixedFunctionRenderPass implements RenderPass<FixedFunctionRenderer> {
    @Override
    public Class<FixedFunctionRenderer> getRendererType() {
        return FixedFunctionRenderer.class;
    }
}
