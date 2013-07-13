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
