package com.ferox.renderer.builder;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Vector4;

/**
 * TextureBuilder is the base builder for all {@link com.ferox.renderer.Texture}
 * resources. Like {@link SamplerBuilder} it is not an actual builder but provides the
 * common configuration of the resource before the image data is specified.
 *
 * @author Michael Ludwig
 */
public interface TextureBuilder<T extends TextureBuilder<T>> extends SamplerBuilder<T> {
    /**
     * Configure the anistropic filtering level used by the created texture. The value
     * must be between 0 and 1.
     *
     * @param v The anistropy level
     *
     * @return This builder
     *
     * @throws IllegalArgumentException if v is out of range
     */
    public T anisotropy(double v);

    /**
     * Configure the border color for the created texture. The HDR values are passed to
     * OpenGL where they may or may not be clamped depending on the texture format
     * selected.
     *
     * @param color The border color
     *
     * @return This builder
     *
     * @throws NullPointerException if color is null
     */
    public T borderColor(@Const ColorRGB color);

    /**
     * Configure the border color for the created texture. The values will be taken as is,
     * without performing any clamping.
     *
     * @param color The border color; x=red, y=green, z=blue, w=alpha
     *
     * @return This builder
     *
     * @throws NullPointerException if color is null
     */
    public T borderColor(@Const Vector4 color);

    /**
     * BasicColorData represents the acceptable data types shared across all color texture
     * formats. The size of the input arrays depends on the configured width and height of
     * the texture, on the mipmap selected by the image builder, and on the number of
     * components of the format.
     */
    public static interface BasicColorData {
        /**
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#UNSIGNED_NORMALIZED_BYTE}.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromUnsignedNormalized(byte[] data);

        /**
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#UNSIGNED_NORMALIZED_SHORT}.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromUnsignedNormalized(short[] data);

        /**
         * Specify the image data array, will use a data type of {@link
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
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#UNSIGNED_BYTE}. Textures created from this are
         * integer textures with unsigned data, and the appropriate uniform type must be
         * used.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromUnsigned(byte[] data);

        /**
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#UNSIGNED_SHORT}. Textures created from this are
         * integer textures with unsigned data, and the appropriate uniform type must be
         * used.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromUnsigned(short[] data);

        /**
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#UNSIGNED_INT}. Textures created from this are
         * integer textures with unsigned data, and the appropriate uniform type must be
         * used.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromUnsigned(int[] data);

        /**
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#FLOAT}. If supported, the values will not be
         * clamped to the [-1, 1] range.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void from(float[] data);

        /**
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#BYTE}. Textures created from this are integer
         * textures with signed data, and the appropriate uniform type must be used.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void from(byte[] data);

        /**
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#SHORT}. Textures created from this are integer
         * textures with signed data, and the appropriate uniform type must be used.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void from(short[] data);

        /**
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#INT}. Textures created from this are integer
         * textures with signed data, and the appropriate uniform type must be used.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void from(int[] data);

        /**
         * Specify the image data array, will use a data type of {@link
         * com.ferox.renderer.DataType#HALF_FLOAT}. If supported, the values will not be
         * clamped to the [-1, 1] range.
         *
         * @param data The data for the configured mipmap and image
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromHalfFloats(short[] data);
    }

    /**
     * RGBData extends BasicColorData to provide additional data format options for RGB
     * formatted textures.
     */
    public static interface RGBData extends BasicColorData {
        /**
         * Specify the image data array, it will use a data type of {@link
         * com.ferox.renderer.DataType#INT_BIT_FIELD}. Specifically, the 32 bits will be
         * interpreted as a the R11F_G11F_B10F packed floating point format defined by
         * OpenGL.
         * <p/>
         * The upper 11 bits are an 11-bit floating point number storing the red
         * component. The middle 11 bits are an 11-bit floating point number storing the
         * green component. The lowest 10 bits are a 10-bit floating point number storing
         * the blue component.
         *
         * @param data The data array
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromPackedFloats(int[] data);
    }

    /**
     * CompressedRGBData extends RGBData to provide additional format options when
     * compressed RGB textures can be used.
     */
    public static interface CompressedRGBData extends RGBData {
        /**
         * Specify the image data array with the assumption that the byte data is stored
         * in the DXT1 compressed format without alpha transparency. The data type will be
         * {@link com.ferox.renderer.DataType#UNSIGNED_BYTE} and the base format will
         * switch from RGB to COMPRESSED_RGB.
         *
         * @param data The data array
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromDXT1(byte[] data);
    }

    /**
     * CompressedRGBAData extends BasicColorData to provide additional format options when
     * compressed RGBA textures can be used.
     */
    public static interface CompressedRGBAData extends BasicColorData {
        /**
         * Specify the image data array with the assumption that the byte data is stored
         * in the DXT1 compressed format with alpha transparency. The data type will be
         * {@link com.ferox.renderer.DataType#UNSIGNED_BYTE} and the base format will
         * switch from RGBA to COMPRESSED_RGBA.
         *
         * @param data The data array
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromDXT1(byte[] data);

        /**
         * Specify the image data array with the assumption that the byte data is stored
         * in the DXT3 compressed format. The data type will be {@link
         * com.ferox.renderer.DataType#UNSIGNED_BYTE} and the base format will switch from
         * RGBA to COMPRESSED_RGBA.
         *
         * @param data The data array
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromDXT3(byte[] data);

        /**
         * Specify the image data array with the assumption that the byte data is stored
         * in the DXT5 compressed format. The data type will be {@link
         * com.ferox.renderer.DataType#UNSIGNED_BYTE} and the base format will switch from
         * RGBA to COMPRESSED_RGBA.
         *
         * @param data The data array
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromDXT5(byte[] data);
    }

    /**
     * ARGBData provides limited format options for data presented in ARGB order.
     */
    public static interface ARGBData {
        /**
         * Specify the image data array, it will use a data type of {@link
         * com.ferox.renderer.DataType#UNSIGNED_NORMALIZED_BYTE}.
         *
         * @param data The data array
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromUnsignedNormalized(byte[] data);

        /**
         * Specify the image data array where the ARGB components are packed into the byte
         * words of each primitive. The data type will be {@link
         * com.ferox.renderer.DataType#INT_BIT_FIELD}. The most significant word holds the
         * byte for alpha, the next holds red, then green, and the least significant word
         * holds the blue component. All bytes, once extraced, are treated as unsigned,
         * normalized bytes.
         *
         * @param data The data array
         *
         * @throws IllegalArgumentException if data's length doesn't equal the expected
         *                                  size given the dimensions and format
         * @throws NullPointerException     if data is null
         */
        public void fromPackedBytes(int[] data);
    }
}
