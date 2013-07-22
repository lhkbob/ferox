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

import com.ferox.renderer.DepthMap2D;

/**
 * DepthMap2DBuilder is a concrete sampler builder for {@link DepthMap2D} resources. It uses {@link ImageData}
 * instances to specify the actual mipmap image data and build the final DepthMap2D.
 *
 * @author Michael Ludwig
 */
public interface DepthMap2DBuilder extends DepthMapBuilder<DepthMap2DBuilder>, Builder<DepthMap2D> {
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
     * Configure the sampler to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#DEPTH}. The
     * returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ImageData<? extends DepthData> depth();

    /**
     * Configure the texture to use a base format of {@link com.ferox.renderer.Sampler.TexelFormat#DEPTH_STENCIL}.
     * The returned image builder can be used to specify some or all mipmap levels and then build the final
     * image.
     *
     * @return The final image builder
     *
     * @throws IllegalStateException if another image builder was already returned
     */
    public ImageData<? extends DepthStencilData> depthStencil();
}
