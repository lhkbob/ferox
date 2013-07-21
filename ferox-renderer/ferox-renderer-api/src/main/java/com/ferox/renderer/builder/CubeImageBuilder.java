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

import com.ferox.renderer.Sampler;

/**
 * CubeImageBuilder is the final image builder used by SamplerBuilders where the sampler type is a cube map.
 * It supports image specification for the six faces of the cube, where each face is a mipmapped, 2D image.
 * This is used by {@link TextureCubeMapBuilder}, and {@link DepthCubeMapBuilder}.
 *
 * @author Michael Ludwig
 */
public interface CubeImageBuilder<T extends Sampler, M> extends Builder<T> {
    /**
     * Get the data specification instance of type {@code M} for the positive X cube face, at the given mipmap
     * {@code level}. Other than restricting the cube face that the data is associated with, this behaves
     * identically to {@link SingleImageBuilder#mipmap(int)}.
     *
     * @param mipmap The mipmap to configure
     *
     * @return The data specification instance for the positive x face at the given mipmap
     *
     * @throws IndexOutOfBoundsException if mipmap is less than 0 or greater than the maximum mipmap level
     *                                   based on the dimensions of the sampler
     */
    public M positiveX(int mipmap);

    /**
     * Get the data specification instance of type {@code M} for the positive Y cube face, at the given mipmap
     * {@code level}. Other than restricting the cube face that the data is associated with, this behaves
     * identically to {@link SingleImageBuilder#mipmap(int)}.
     *
     * @param mipmap The mipmap to configure
     *
     * @return The data specification instance for the positive y face at the given mipmap
     *
     * @throws IndexOutOfBoundsException if mipmap is less than 0 or greater than the maximum mipmap level
     *                                   based on the dimensions of the sampler
     */
    public M positiveY(int mipmap);

    /**
     * Get the data specification instance of type {@code M} for the positive Z cube face, at the given mipmap
     * {@code level}. Other than restricting the cube face that the data is associated with, this behaves
     * identically to {@link SingleImageBuilder#mipmap(int)}.
     *
     * @param mipmap The mipmap to configure
     *
     * @return The data specification instance for the positive z face at the given mipmap
     *
     * @throws IndexOutOfBoundsException if mipmap is less than 0 or greater than the maximum mipmap level
     *                                   based on the dimensions of the sampler
     */
    public M positiveZ(int mipmap);

    /**
     * Get the data specification instance of type {@code M} for the negative X cube face, at the given mipmap
     * {@code level}. Other than restricting the cube face that the data is associated with, this behaves
     * identically to {@link SingleImageBuilder#mipmap(int)}.
     *
     * @param mipmap The mipmap to configure
     *
     * @return The data specification instance for the negative x face at the given mipmap
     *
     * @throws IndexOutOfBoundsException if mipmap is less than 0 or greater than the maximum mipmap level
     *                                   based on the dimensions of the sampler
     */
    public M negativeX(int mipmap);

    /**
     * Get the data specification instance of type {@code M} for the negative Y cube face, at the given mipmap
     * {@code level}. Other than restricting the cube face that the data is associated with, this behaves
     * identically to {@link SingleImageBuilder#mipmap(int)}.
     *
     * @param mipmap The mipmap to configure
     *
     * @return The data specification instance for the negative y face at the given mipmap
     *
     * @throws IndexOutOfBoundsException if mipmap is less than 0 or greater than the maximum mipmap level
     *                                   based on the dimensions of the sampler
     */
    public M negativeY(int mipmap);

    /**
     * Get the data specification instance of type {@code M} for the negative Z cube face, at the given mipmap
     * {@code level}. Other than restricting the cube face that the data is associated with, this behaves
     * identically to {@link SingleImageBuilder#mipmap(int)}.
     *
     * @param mipmap The mipmap to configure
     *
     * @return The data specification instance for the negative z face at the given mipmap
     *
     * @throws IndexOutOfBoundsException if mipmap is less than 0 or greater than the maximum mipmap level
     *                                   based on the dimensions of the sampler
     */
    public M negativeZ(int mipmap);
}
