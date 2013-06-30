package com.ferox.renderer.builder;

import com.ferox.renderer.DepthMap2D;

/**
 * DepthMap2DBuilder is a concrete sampler builder for {@link DepthMap2D} resources. It
 * uses {@link SingleImageBuilder} instances to specify the actual mipmap image data and
 * build the final DepthMap2D.
 *
 * @author Michael Ludwig
 */
public interface DepthMap2DBuilder extends DepthMapBuilder<DepthMap2DBuilder> {
    /**
     * Configure the width of the 2D depth map at the 0th mipmap level.
     *
     * @param width The width
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if width is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public DepthMap2DBuilder width(int width);

    /**
     * Configure the height of the 2D depth map at the 0th mipmap level.
     *
     * @param height The height
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if height is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public DepthMap2DBuilder height(int height);

    /**
     * Configure the sampler to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#DEPTH}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public SingleImageBuilder<DepthMap2D, DepthData> depth();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#DEPTH_STENCIL}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public SingleImageBuilder<DepthMap2D, DepthStencilData> depthStencil();
}
