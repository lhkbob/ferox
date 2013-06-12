package com.ferox.renderer.builder;

import com.ferox.renderer.DepthMap2D;

/**
 *
 */
public interface DepthMap2DBuilder extends DepthMapBuilder<DepthMap2DBuilder> {
    public DepthMap2DBuilder width(int w);

    public DepthMap2DBuilder height(int h);

    public DepthImageBuilder depth();

    public DepthStencilImageBuilder depthStencil();

    public static interface DepthImageBuilder
            extends SingleImageBuilder<DepthMap2D, DepthData<DepthImageBuilder>> {

    }

    public static interface DepthStencilImageBuilder extends
                                                     SingleImageBuilder<DepthMap2D, DepthStencilData<DepthStencilImageBuilder>> {

    }
}
