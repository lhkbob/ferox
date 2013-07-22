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

/**
 * ImageData is the image data provider used by SamplerBuilders where the sampler type has only a single
 * image. The dimensionality of the image can be one, two, or three dimensional but there will only be a
 * single block of texel data. This is used by {@link Texture1DBuilder}, {@link Texture2DBuilder}, {@link
 * Texture3DBuilder}, and {@link DepthMap2DBuilder}.
 *
 * @author Michael Ludwig
 */
public interface ImageData<M> {
    /**
     * Get a data specification instance of type {@code M} for the given mipmap {@code level}. The returned
     * instance, depending on the exact parameter type, will expose data methods that receive primitive arrays
     * and define how they are interpreted.
     * <p/>
     * However invoked, the input array is associated with the given mipmap level for when the final texture
     * is constructed. The length of the array must be consistent with the dimensions and texel format
     * configured by the SamplerBuilder.
     * <p/>
     * When multiple mipmap levels are specified for the same texture, they must all use the same primitive
     * data type, i.e. you cannot mix calls to {@link TextureBuilder.BasicColorData#fromUnsigned(byte[])} and
     * {@link TextureBuilder.BasicColorData#fromUnsignedNormalized(byte[])}.
     * <p/>
     * Samplers do not need to have every mipmap level specified. Any level that does not have its data
     * provided will have an undefined state on the GPU.  The base and max mipmap levels of the sampler will
     * be configured to minimally cover the defined images. Any unspecified images outside that range will not
     * be allocated or sampled on the GPU.
     * <p/>
     * When creating an image for use with a TextureSurface, the data type must still be selected even though
     * there's no image data to provide. The proper way to do this is to invoke one of the {@code fromX()}
     * methods and pass in a null array.
     *
     * @param level The mipmap level whose image data will be specified with the returned instance
     *
     * @return A data specifier for the mipmap level
     *
     * @throws IndexOutOfBoundsException if level is less than 0 or greater than the maximum allowed mipmap
     *                                   level for the dimensions of the texture
     */
    public M mipmap(int level);
}
