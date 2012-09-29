package com.ferox.resource;

import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * Describes all of the supported texture formats. Some of the formats are only
 * available on newer hardware, such as RGBA_FLOAT or DEPTH_STENCIL. In cases
 * such as this the Framework might mark the resource as UNSUPPORTED.
 * <p>
 * The name of the format describes the layout of the data, and possibly the
 * primitive type required.
 * <p>
 * <b>Name formatting:</b>
 * <ul>
 * <li>XYZ: Formats such as this list their components in order of increasing
 * index in the data array. Each component uses one primitive value (ex. RGBA
 * uses a total of four primitives). All unsigned discrete-valued types are
 * supported, as well as FLOAT.</li>
 * <li>XYZ_123: Formats have component ordering just as above, but all
 * components are packed into a single primitive element. The type of primitive
 * is returned by getSupportedType(). The component packing is described by the
 * list of numbers following XYZ_ (ex. RGBA_8888 has all four values packed into
 * a 32 bit unsigned integer. Bits 31-24 are red, 23-16 are green, 15-8 are
 * blue, 7-0 are alpha).</li>
 * <li>XYZ_DXT?: The format represents texture data in a specific DXT
 * compression algorithm. At the moment, only DXT1, DXT3 and DXT5 formats are
 * supported for RGB and RGBA textures. The data type must be UNSIGNED_BYTE and
 * have dimensions a multiple of 4.</li>
 * <li>XYZ_FLOAT: Formats with this are treated identically to the equivalent
 * XYZ with a required type of FLOAT, except that when stored on the graphics
 * cards, the floating point values are NOT clamped.</li>
 * </ul>
 * </p>
 * <p>
 * Formats using data with type FLOAT will be clamped to [0.0, 1.0] with one
 * exception: If the format is XYZ_FLOAT, the renderer must attempt to leverage
 * unclamped 32-bit floating-point textures. This is only available on newer
 * hardware. If it's not available, the renderer may decide to use normal
 * clamped float values.
 * <p>
 * <b>An important note about formats:</b> the unclamped formats are only
 * available if a RenderCapabilities returns true in its
 * getUnclampedTextureSupport(). Similarly, the DXT_n options are only available
 * if getS3TCTextureCompression() is true. The compressed and DEPTH-based
 * TextureFormats might be supported on a limited set of texture targets.
 * 
 * @author Michael Ludwig
 */
public enum TextureFormat {
    RGBA(null, 4, 4, true),
    RGBA_4444(DataType.UNSIGNED_SHORT, 1, 4, true, true),
    RGBA_8888(DataType.UNSIGNED_INT, 1, 4, true, true),
    RGBA_5551(DataType.UNSIGNED_SHORT, 1, 4, true, true),
    RGBA_FLOAT(DataType.FLOAT, 4, 4, true),

    RGBA_DXT1(DataType.UNSIGNED_BYTE, -1, 4, true),
    RGBA_DXT3(DataType.UNSIGNED_BYTE, -1, 4, true),
    RGBA_DXT5(DataType.UNSIGNED_BYTE, -1, 4, true),

    BGRA(null, 4, 4, true),
    BGRA_4444(DataType.UNSIGNED_SHORT, 1, 4, true, true),
    BGRA_8888(DataType.UNSIGNED_INT, 1, 4, true, true),
    BGRA_5551(DataType.UNSIGNED_SHORT, 1, 4, true, true),

    ARGB_4444(DataType.UNSIGNED_SHORT, 1, 4, true, true),
    ARGB_1555(DataType.UNSIGNED_SHORT, 1, 4, true, true),
    ARGB_8888(DataType.UNSIGNED_INT, 1, 4, true, true),

    ABGR_4444(DataType.UNSIGNED_SHORT, 1, 4, true, true),
    ABGR_1555(DataType.UNSIGNED_SHORT, 1, 4, true, true),
    ABGR_8888(DataType.UNSIGNED_INT, 1, 4, true, true),

    RGB(null, 3, 3, false),
    RGB_565(DataType.UNSIGNED_SHORT, 1, 3, false, true),
    RGB_FLOAT(DataType.FLOAT, 3, 3, false),
    RGB_DXT1(DataType.UNSIGNED_BYTE, -1, 3, false),

    BGR(null, 3, 3, false),
    BGR_565(DataType.UNSIGNED_SHORT, 1, 3, false, true),

    R(null, 1, 1, false),
    R_FLOAT(DataType.FLOAT, 1, 1, false),
    RG(null, 2, 2, false),
    RG_FLOAT(DataType.FLOAT, 2, 2, false),

    DEPTH(DataType.UNSIGNED_INT, 1, 1, false),
    DEPTH_FLOAT(DataType.FLOAT, 1, 1, false),
    DEPTH_STENCIL(DataType.UNSIGNED_INT, 1, 1, false);

    private DataType type;
    private boolean hasAlpha, isPacked;
    private int pPerC, numC;

    private TextureFormat(DataType type, int pPerC, int numC, boolean alpha) {
        this(type, pPerC, numC, alpha, false);
    }

    private TextureFormat(DataType type, int pPerC, int numC, boolean alpha, boolean packed) {
        this.type = type;
        this.pPerC = pPerC;
        hasAlpha = alpha;
        isPacked = packed;
    }

    /**
     * Return true if this format has its color components packed into a single
     * primitive.
     * 
     * @return Whether or not colors are packed into one primitive
     */
    public boolean isPackedFormat() {
        return isPacked;
    }

    /**
     * Return the number of components representing the color. An RGB color
     * would have 3 components and an RGBA color would have 4, etc.
     * 
     * @return The number of components in this format
     */
    public int getNumComponents() {
        return numC;
    }

    /**
     * Get the number of primitive elements per each color element. Returns -1
     * if the format is client compressed, since there is no meaningful
     * primitive/component value.
     * 
     * @return The number of primitives used to hold an entire color
     */
    public int getPrimitivesPerColor() {
        return pPerC;
    }

    /**
     * Whether or not this texture has image data that is client compressed.
     * 
     * @return Whether or not the texture data is compressed
     */
    public boolean isCompressed() {
        return pPerC <= 0;
    }

    /**
     * Whether or not this texture has image data with alpha values.
     * 
     * @return Whether or not the format stores alpha information
     */
    public boolean hasAlpha() {
        return hasAlpha;
    }

    /**
     * Return the DataType that is required by the TextureFormat. If null is
     * returned, then any DataType is allowed.
     * 
     * @return The required DataType of texture data for this format, may be
     *         null
     */
    public DataType getSupportedType() {
        return type;
    }

    /**
     * Whether or not the DataType is supported by this format.
     * 
     * @see #getSupportedType()
     * @param type The Buffer class meant to hold data in this format
     * @return True if this type can be used with type
     * @throws NullPointerException if type is null
     */
    public boolean isTypeValid(DataType type) {
        if (type == null) {
            throw new NullPointerException("Type cannot be null");
        }

        if (this.type == null) {
            return true;
        } else {
            return this.type.equals(type);
        }
    }

    /**
     * <p>
     * Compute the size of a texture, in primitive elements, for this format and
     * the given dimensions. For one-dimensional or two-dimensional textures
     * that don't need a dimension, a value of 1 should be used.
     * </p>
     * <p>
     * For formats that are compressed, the depth value is ignored because they
     * are currently only supported for two-dimensional textures.
     * </p>
     * <p>
     * Returns -1 if any of the dimensions are <= 0, or if width and height
     * aren't multiples of 4 for compressed textures (the exceptions are if the
     * values are 1 or 2, which are also allowed). If depth is not 1 for
     * compressed textures, -1 is returned.
     * </p>
     * 
     * @param width The width of the texture image
     * @param height The height of the texture image
     * @param depth The depth of the texture image
     * @return The number of primitives required to hold all texture data for a
     *         texture of the given dimensions with this format
     */
    public int getBufferSize(int width, int height, int depth) {
        if (width <= 0 || height <= 0 || depth <= 0) {
            return -1;
        }
        if (isCompressed() && (width % 4 != 0 || height % 4 != 0)) {
            // compression needs to have multiple of 4 dimensions
            if (width != 1 && width != 2 && height != 1 && height != 2) {
                return -1;
            }
        }
        if (isCompressed() && depth != 1) {
            return -1;
        }

        if (isCompressed()) {
            return (int) ((this == RGBA_DXT1 || this == RGB_DXT1 ? 8 : 16) * Math.ceil(width / 4f) * Math.ceil(height / 4f));
        } else {
            return width * height * depth * getPrimitivesPerColor();
        }
    }
}
