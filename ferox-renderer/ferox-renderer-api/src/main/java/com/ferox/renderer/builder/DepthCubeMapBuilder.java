package com.ferox.renderer.builder;

import com.ferox.renderer.DepthCubeMap;

/**
 * DepthCubeMapBuilder is a concrete sampler builder for {@link DepthCubeMap} resources.
 * It uses {@link CubeImageBuilder} instances to specify the actual mipmap image data and
 * build the final DepthCubeMap.
 *
 * @author Michael Ludwig
 */
public interface DepthCubeMapBuilder extends DepthMapBuilder<DepthCubeMapBuilder> {
    /**
     * Configure the width and height of each 2D face of the depth map at the 0th mipmap
     * level.
     *
     * @param side The side length of the cube
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if side is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public DepthCubeMapBuilder side(int side);

    /**
     * Configure the sampler to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#DEPTH}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<DepthCubeMap, DepthData> depth();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#DEPTH_STENCIL}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<DepthCubeMap, DepthStencilData> depthStencil();
}
