package com.ferox.renderer.builder;

import com.ferox.renderer.DepthMap2D;

/**
 *
 */
public interface DepthMap2DBuilder extends DepthMapBuilder<DepthMap2DBuilder> {
    public DepthMap2DBuilder width(int w);

    public DepthMap2DBuilder height(int h);

    public SingleImageBuilder<DepthMap2D, DepthData> depth();

    public SingleImageBuilder<DepthMap2D, DepthStencilData> depthStencil();
}
