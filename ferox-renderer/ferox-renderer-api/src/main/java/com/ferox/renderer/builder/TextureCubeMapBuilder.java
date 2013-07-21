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

import com.ferox.renderer.TextureCubeMap;

/**
 * TextureCubeMapBuilder is a concrete sampler builder for {@link TextureCubeMap} resources. It uses {@link
 * CubeImageBuilder} instances to specify the actual mipmap image data and build the final TextureCubeMap.
 *
 * @author Michael Ludwig
 */
public interface TextureCubeMapBuilder extends TextureBuilder<TextureCubeMapBuilder> {
    /**
     * Configure the width and height of each 2D face of the texture at the 0th mipmap level.
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
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#R}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, BasicColorData> r();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RG}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, BasicColorData> rg();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGB} or
     * {@link com.ferox.renderer.Sampler.TexelFormat#COMPRESSED_RGB}. The returned image builder can be used
     * to specify some or all mipmap levels and then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, CompressedRGBData> rgb();

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
    public CubeImageBuilder<TextureCubeMap, BasicColorData> bgr();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#RGBA} or
     * {@link com.ferox.renderer.Sampler.TexelFormat#COMPRESSED_RGBA}. The returned image builder can be used
     * to specify some or all mipmap levels and then build the final image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public CubeImageBuilder<TextureCubeMap, CompressedRGBAData> rgba();

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
    public CubeImageBuilder<TextureCubeMap, BasicColorData> bgra();

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
    public CubeImageBuilder<TextureCubeMap, ARGBData> argb();
}
