package com.ferox.renderer.builder;

import com.ferox.renderer.Texture1DArray;

/**
 * Texture1DArrayBuilder is a concrete sampler builder for {@link Texture1DArray} resources. It uses {@link
 * ArrayImageBuilder} instances to specify the actual mipmap image data and build the final Texture1DArray.
 *
 * @author Michael Ludwig
 */
public interface Texture1DArrayBuilder extends TextureBuilder<Texture1DArrayBuilder> {
    /**
     * Configure the length in pixels of each 1D texture at the 0th mipmap level.
     *
     * @param length The length
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if length is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture1DArrayBuilder length(int length);

    /**
     * Configure the number of 2D images in the array.
     *
     * @param length The number of 2D images
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if length is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture1DArrayBuilder imageCount(int length);

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#R}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture1DArray, BasicColorData> r();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RG}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture1DArray, BasicColorData> rg();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGB}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture1DArray, RGBData> rgb();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGB}.
     * OpenGL will interpret the components in the order B, G, R instead of R, G, B within the data arrays for
     * each image. The returned image builder can be used to specify some or all mipmap levels and then build
     * the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture1DArray, BasicColorData> bgr();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGBA}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture1DArray, BasicColorData> rgba();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGBA}.
     * OpenGL will interpret the components in the order B, G, R, A instead of R, G, B, A within the data
     * arrays for each image. The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture1DArray, BasicColorData> bgra();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGBA}.
     * OpenGL will interpret the components in the order A, R, G, B instead of R, G, B, A within the data
     * arrays for each image. The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture1DArray, ARGBData> argb();
}
