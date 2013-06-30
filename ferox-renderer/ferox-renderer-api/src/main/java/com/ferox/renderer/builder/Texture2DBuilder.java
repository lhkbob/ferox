package com.ferox.renderer.builder;

import com.ferox.renderer.Texture2D;

/**
 * Texture2DBuilder is a concrete sampler builder for {@link Texture2D} resources. It uses
 * {@link SingleImageBuilder} instances to specify the actual mipmap image data and build
 * the final Texture2D.
 *
 * @author Michael Ludwig
 */
public interface Texture2DBuilder extends TextureBuilder<Texture2DBuilder> {
    /**
     * Configure the width of the 2D texture at the 0th mipmap level.
     *
     * @param width The width
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if width is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture2DBuilder width(int width);

    /**
     * Configure the height of the 2D texture at the 0th mipmap level.
     *
     * @param height The height
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if height is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture2DBuilder height(int height);

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#R}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public SingleImageBuilder<Texture2D, BasicColorData> r();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RG}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public SingleImageBuilder<Texture2D, BasicColorData> rg();

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
    public SingleImageBuilder<Texture2D, CompressedRGBData> rgb();

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
    public SingleImageBuilder<Texture2D, BasicColorData> bgr();

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
    public SingleImageBuilder<Texture2D, CompressedRGBAData> rgba();

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
    public SingleImageBuilder<Texture2D, BasicColorData> bgra();

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
    public SingleImageBuilder<Texture2D, ARGBData> argb();
}
