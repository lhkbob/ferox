package com.ferox.renderer.builder;

import com.ferox.renderer.Sampler;

/**
 * SamplerBuilder is the base builder for all samplers. Sampler building is slightly more
 * complicated than the other resources because a sampler may take multiple images.  To
 * support this and still have type safety based on the selected texture format, the main
 * sampler builder instance returned by the Framework is not an actual {@link Builder}.
 * <p/>
 * Instead, it should be configured with the sampler configuration, with a final call to
 * one of the format selecting methods that return an actual Builder. The returned image
 * Builder will have methods to set which mipmap, cube face, etc. to specify. Once the
 * image Builder is chosen, the configuration methods on the sampler builder cannot be
 * invoked.
 * <p/>
 * Here is an example for a Texture2D:
 * <pre>
 *     // First get the sampler builder and configure it to an image builder for
 *     // a 128x128 RGB texture
 *     SingleImageBuilder&lt;Texture2D, CompressedRGBData&gt; img =
 * framework.newTexture2D()
 *              .width(128)
 *              .height(128)
 *              .rgb();
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
 *     // Get the final texture
 *     Texture2D tex = img.build();
 * </pre>
 *
 * @author Michael Ludwig
 * @see DepthMapBuilder
 * @see TextureBuilder
 */
public interface SamplerBuilder<B extends SamplerBuilder<B>> {
    /**
     * Configure the sampler to interpolate between its texels smoothly. If not called,
     * the sampler will use nearest-neighbor sampling.
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
