package com.ferox.resource.builder;

import com.ferox.resource.DepthCubeMap;

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
