package com.ferox.renderer.builder;

import com.ferox.renderer.Texture3D;

/**
 * Texture3DBuilder is a concrete sampler builder for {@link Texture3D} resources. It uses {@link
 * SingleImageBuilder} instances to specify the actual mipmap image data and build the final Texture3D.
 *
 * @author Michael Ludwig
 */
public interface Texture3DBuilder extends TextureBuilder<Texture3DBuilder> {
    /**
     * Configure the width of the 3D texture at the 0th mipmap level.
     *
     * @param width The width
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if width is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture3DBuilder width(int width);

    /**
     * Configure the height of the 3D texture at the 0th mipmap level.
     *
     * @param height The height
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if height is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture3DBuilder height(int height);

    /**
     * Configure the depth of the 3D texture at the 0th mipmap level.
     *
     * @param depth The depth
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if depth is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture3DBuilder depth(int depth);

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#R}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public SingleImageBuilder<Texture3D, BasicColorData> r();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RG}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public SingleImageBuilder<Texture3D, BasicColorData> rg();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGB}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public SingleImageBuilder<Texture3D, RGBData> rgb();

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
    public SingleImageBuilder<Texture3D, BasicColorData> bgr();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGBA}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public SingleImageBuilder<Texture3D, BasicColorData> rgba();

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
    public SingleImageBuilder<Texture3D, BasicColorData> bgra();

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
    public SingleImageBuilder<Texture3D, ARGBData> argb();
}
