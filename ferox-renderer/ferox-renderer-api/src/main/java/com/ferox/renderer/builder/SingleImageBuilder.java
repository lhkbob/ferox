package com.ferox.renderer.builder;

import com.ferox.renderer.Sampler;

/**
 * SingleImageBuilder is the final image builder used by SamplerBuilders where the sampler type has only a
 * single image. The dimensionality of the image can be one, two, or three dimensional but there will only be
 * a single block of texel data. This is used by {@link Texture1DBuilder}, {@link Texture2DBuilder}, {@link
 * Texture3DBuilder}, and {@link DepthMap2DBuilder}.
 *
 * @author Michael Ludwig
 */
public interface SingleImageBuilder<T extends Sampler, M> extends Builder<T> {
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
