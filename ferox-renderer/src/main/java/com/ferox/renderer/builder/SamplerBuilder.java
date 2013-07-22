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
 * SamplerBuilder is the base builder for all samplers. Sampler building is slightly more complicated than the
 * other resources because a sampler may take multiple images.  To support this and still have type safety
 * based on the selected texture format, the main sampler builder instance returned by the Framework is not an
 * actual {@link Builder}.
 * <p/>
 * Instead, it should be configured with the sampler configuration, with a final call to one of the format
 * selecting methods that return an actual Builder. The returned image Builder will have methods to set which
 * mipmap, cube face, etc. to specify. Once the image Builder is chosen, the configuration methods on the
 * sampler builder cannot be invoked.
 * <p/>
 * Here is an example for a Texture2D:
 * <pre>
 *     // First get the sampler builder and configure it to an image builder for
 *     // a 128x128 RGB texture
 *     Texture2DBuilder texBuilder = framework.newTexture2D()
 *              .width(128)
 *              .height(128);
 *     ImageData&lt;? extends BasicColorData%gt; img = texBuilder.rgb();
 *
 *     // Use the image builder to set all mipmaps with appropriate dimensions
 *     // using the FLOAT data-type. All mipmaps must agree.
 *     img.mipmap(2).from(new float[128*128*3]);
 *     img.mipmap(3).from(new float[64*64*3]);
 *     img.mipmap(4).from(new float[32*32*3]);
 *     img.mipmap(5).from(new float[16*16*3]);
 *     img.mipmap(6).from(new float[8*8*3]);
 *     img.mipmap(7).from(new float[4*4*3]);
 *     img.mipmap(8).from(new float[2*2*3]);
 *     img.mipmap(9).from(new float[1*1*3]);
 *
 *     // Get the final texture and configure a few more options
 *     Texture2D tex = texBuilder.interpolated().borderColor(new Vector4(0, 0, 0, 1)).build();
 * </pre>
 *
 * @author Michael Ludwig
 * @see DepthMapBuilder
 * @see TextureBuilder
 */
public interface SamplerBuilder<B extends SamplerBuilder<B>> {
    /**
     * Configure the sampler to interpolate between its texels smoothly. If not called, the sampler will use
     * nearest-neighbor sampling.
     *
     * @return This builder
     */
    public B interpolated();

    /**
     * Configure the wrap mode used by the created sampler.
     *
     * @param wrap The wrap mode
     *
     * @return This builder
     *
     * @throws NullPointerException if wrap is null
     */
    public B wrap(Sampler.WrapMode wrap);
}
