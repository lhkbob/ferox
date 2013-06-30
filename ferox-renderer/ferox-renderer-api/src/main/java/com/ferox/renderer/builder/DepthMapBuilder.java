package com.ferox.renderer.builder;

import com.ferox.renderer.Renderer;

/**
 * DepthMapBuilder is the base builder for all {@link com.ferox.renderer.DepthMap}
 * resources. Like {@link SamplerBuilder} it is not an actual builder but provides the
 * common configuration of the resource before the image data is specified.
 *
 * @author Michael Ludwig
 */
public interface DepthMapBuilder<B extends DepthMapBuilder<B>> extends SamplerBuilder<B> {
    /**
     * Configure the depth map to be sampled using the given depth comparison.  If the
     * enum value is null, then depth comparison is disabled.  When sampled using depth
     * comparison, shaders must use shadow variant of the sampler uniform type.  Depth
     * comparison compares the stored depth value with the texture coordinate's component
     * just past the dimension of the texture. If the comparison passes then it evaluates
     * to 1, and if it fails it is 0
     * <p/>
     * When no comparison is in place, the depth values are reported as floating point
     * values in the red channel of the color.
     *
     * @param compare The depth comparison, or null to disable
     *
     * @return This builder
     */
    public B depthComparison(Renderer.Comparison compare);

    /**
     * Configure the depth value to sample when border texels are sampled.
     *
     * @param depth The border depth value
     *
     * @return This builder
     */
    public B borderDepth(double depth);

    /**
     * DepthData represents the common depth data formats shared by the concrete depth map
     * classes.
     */
    public static interface DepthData {
        /**
         * Specify the image depth data, will use a data type of {@link
         * com.ferox.renderer.DataType#FLOAT}.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void from(float[] data);

        /**
         * Specify the image depth data, will use a data type of {@link
         * com.ferox.renderer.DataType#UNSIGNED_NORMALIZED_INT}.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromUnsignedNormalized(int[] data);

        /**
         * Specify the image depth data, will use a data type of {@link
         * com.ferox.renderer.DataType#UNSIGNED_NORMALIZED_SHORT}.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromUnsignedNormalized(short[] data);
    }

    /**
     * DepthStencilData represents the shared data formats for depth map classes that will
     * store combined depth and stencil data.
     */
    public static interface DepthStencilData {
        /**
         * Specify the image depth and stencil data, will use a data type of {@link
         * com.ferox.renderer.DataType#INT_BIT_FIELD}. The most significant 3 bytes (24
         * bits) hold the depth value stored as a 24 bit unsigned normalized float. The
         * least significant byte holds the stencil mask.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromBits(int[] data);
    }
}
