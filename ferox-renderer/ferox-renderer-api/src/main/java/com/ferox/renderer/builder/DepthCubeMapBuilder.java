package com.ferox.renderer.builder;

import com.ferox.renderer.DepthCubeMap;

/**
 *
 */
public interface DepthCubeMapBuilder extends DepthMapBuilder<DepthCubeMapBuilder> {
    public DepthCubeMapBuilder side(int w);

    public CubeImageBuilder<DepthCubeMap, DepthData> depth();

    public CubeImageBuilder<DepthCubeMap, DepthStencilData> depthStencil();
}
