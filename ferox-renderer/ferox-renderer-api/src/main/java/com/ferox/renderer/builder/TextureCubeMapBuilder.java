package com.ferox.renderer.builder;

import com.ferox.renderer.TextureCubeMap;

/**
 * TextureCubeMapBuilder is a concrete sampler builder for {@link TextureCubeMap}
 * resources. It uses {@link CubeImageBuilder} instances to specify the actual mipmap
 * image data and build the final TextureCubeMap.
 *
 * @author Michael Ludwig
 */
public interface TextureCubeMapBuilder extends TextureBuilder<TextureCubeMapBuilder> {
    /**
     * Configure the width and height of each 2D face of the texture at the 0th mipmap
     * level.
     *
     * @param side The side length of the cube
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if side is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public TextureCubeMapBuilder side(int side);

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#R}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, BasicColorData> r();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RG}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, BasicColorData> rg();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RGB}
     * or {@link com.ferox.renderer.Sampler.BaseFormat#COMPRESSED_RGB}. The returned image
     * builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, CompressedRGBData> rgb();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RGB}.
     * OpenGL will interpret the components in the order B, G, R instead of R, G, B within
     * the data arrays for each image. The returned image builder can be used to specify
     * some or all mipmap levels and then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, BasicColorData> bgr();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RGBA}
     * or {@link com.ferox.renderer.Sampler.BaseFormat#COMPRESSED_RGBA}. The returned
     * image builder can be used to specify some or all mipmap levels and then build the
     * final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, CompressedRGBAData> rgba();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RGBA}.
     * OpenGL will interpret the components in the order B, G, R, A instead of R, G, B, A
     * within the data arrays for each image. The returned image builder can be used to
     * specify some or all mipmap levels and then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, BasicColorData> bgra();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RGBA}.
     * OpenGL will interpret the components in the order A, R, G, B instead of R, G, B, A
     * within the data arrays for each image. The returned image builder can be used to
     * specify some or all mipmap levels and then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, ARGBData> argb();
}
