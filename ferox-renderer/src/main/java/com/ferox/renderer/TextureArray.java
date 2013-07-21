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
package com.ferox.renderer;

/**
 * The TextureArray interface is a decorating interface for specific texture types that are arrays of images.
 * All texture arrays have common properties regardless of the underlying texture type of each image. They
 * expose a render target for each image, in addition to the default render target that refers to the first or
 * 0th image. Texture arrays also support one additional texture coordinate besides however many supported by
 * the base texture type, which is used to designate the specific image in the array.
 * <p/>
 * Texture types that implement TextureArray should extend from the texture interface type of each image
 * within the array. As an example, {@link Texture2DArray} extends from {@link Texture2D}. This allows the 2D
 * texture array to be used anywhere in the code that expects a two-dimensional image.
 *
 * @author Michael Ludwig
 */
public interface TextureArray {
    /**
     * @return The number of images contained in the array
     */
    public int getImageCount();

    /**
     * Get the render target that will render directly into the specific image. {@code image} must be between
     * 0 and {@code getImageCount() - 1}.
     *
     * @param image The image to render into
     *
     * @return The render target for the image
     *
     * @throws IndexOutOfBoundsException if image is less than 0 or greater than the number of render targets
     */
    public Sampler.RenderTarget getRenderTarget(int image);
}
