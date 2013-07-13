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
