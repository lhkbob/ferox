package com.ferox.renderer.builder;

import com.ferox.renderer.DepthCubeMap;

/**
 *
 */
public interface DepthCubeMapBuilder extends DepthMapBuilder<DepthCubeMapBuilder> {
    public DepthCubeMapBuilder side(int w);

    public DepthImageBuilder depth();

    public DepthStencilImageBuilder depthStencil();

    public static interface DepthImageBuilder
            extends SingleImageBuilder<DepthCubeMap, DepthData<DepthImageBuilder>> {

    }

    public static interface DepthStencilImageBuilder extends
                                                     SingleImageBuilder<DepthCubeMap, DepthStencilData<DepthStencilImageBuilder>> {

    }
}
