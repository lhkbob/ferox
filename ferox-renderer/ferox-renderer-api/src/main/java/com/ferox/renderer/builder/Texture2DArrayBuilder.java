package com.ferox.renderer.builder;

import com.ferox.renderer.Texture2DArray;

/**
 * Texture2DArrayBuilder is a concrete sampler builder for {@link Texture2DArray}
 * resources. It uses {@link ArrayImageBuilder} instances to specify the actual mipmap
 * image data and build the final Texture2DArray.
 *
 * @author Michael Ludwig
 */
public interface Texture2DArrayBuilder extends TextureBuilder<Texture2DArrayBuilder> {
    /**
     * Configure the width of each 2D texture at the 0th mipmap level.
     *
     * @param width The width
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if width is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture2DArrayBuilder width(int width);

    /**
     * Configure the height of each 2D texture at the 0th mipmap level.
     *
     * @param height The height
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if height is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture2DArrayBuilder height(int height);

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
    public Texture2DArrayBuilder imageCount(int length);

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#R}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture2DArray, BasicColorData> r();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RG}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture2DArray, BasicColorData> rg();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RGB}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture2DArray, RGBData> rgb();

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
    public ArrayImageBuilder<Texture2DArray, BasicColorData> bgr();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.BaseFormat#RGBA}.
     * The returned image builder can be used to specify some or all mipmap levels and
     * then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ArrayImageBuilder<Texture2DArray, BasicColorData> rgba();

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
    public ArrayImageBuilder<Texture2DArray, BasicColorData> bgra();

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
    public ArrayImageBuilder<Texture2DArray, ARGBData> argb();
}
