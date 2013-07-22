/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.builder;

import com.ferox.renderer.Texture1D;

/**
 * Texture1DBuilder is a concrete sampler builder for {@link Texture1D} resources. It uses {@link ImageData}
 * instances to specify the actual mipmap image data and build the final Texture1D.
 *
 * @author Michael Ludwig
 */
public interface Texture1DBuilder extends TextureBuilder<Texture1DBuilder>, Builder<Texture1D> {
    /**
     * Configure the length in pixels of the texture at the 0th mipmap level.
     *
     * @param length The length
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if length is less than 1
     * @throws IllegalStateException    if an image builder has already been selected
     */
    public Texture1DBuilder length(int length);

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#R}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ImageData<? extends BasicColorData> r();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RG}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ImageData<? extends BasicColorData> rg();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGB}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ImageData<? extends RGBData> rgb();

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
    public ImageData<? extends BasicColorData> bgr();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGBA}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ImageData<? extends BasicColorData> rgba();

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
    public ImageData<? extends BasicColorData> bgra();

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
    public ImageData<? extends ARGBData> argb();
}
