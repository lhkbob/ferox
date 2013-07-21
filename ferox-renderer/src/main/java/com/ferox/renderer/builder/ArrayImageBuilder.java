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
import com.ferox.renderer.TextureArray;

/**
 * SingleImageBuilder is the final image builder used by SamplerBuilders where the sampler type also extends
 * {@link TextureArray}. The dimensionality of the images can be one, or two dimensional and there is a
 * variable number texel data blocks. This is used by {@link Texture1DArrayBuilder}, and {@link
 * Texture2DArrayBuilder}.
 *
 * @author Michael Ludwig
 */
public interface ArrayImageBuilder<T extends Sampler, M> extends Builder<T> {
    /**
     * Get the data specification instance of type {@code M} for the image at {@code index} within the array,
     * at the given mipmap {@code level}. Other than restricting which image in the array is modified, this
     * behaves identically to {@link SingleImageBuilder#mipmap(int)}.
     *
     * @param index  The image in the array
     * @param mipmap The mipmap of the indexed image to configure
     *
     * @return The data specification instance for the indexed image at the given mipmap
     *
     * @throws IndexOutOfBoundsException if mipmap is less than 0 or greater than the maximum mipmap level
     *                                   based on the dimensions of the sampler, or if index is outside the
     *                                   range of defined images for the sampler
     */
    public M mipmap(int index, int mipmap);
}
