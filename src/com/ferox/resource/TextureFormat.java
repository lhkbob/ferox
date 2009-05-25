package com.ferox.resource;

import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * Describes all of the supported texture formats. Some of the formats are only
 * available on newer hardware, such as RGBA_FLOAT. In cases such as this the
 * Renderer is allowed to change the type of texture, in which case the image
 * should be flagged as DIRTY.
 * </p>
 * <p>
 * The name of the format describes the layout of the data, and possibly the
 * primitive type required.
 * </p>
 * <p>
 * <b>Named formatting:</b>
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
 * <b>EffectType conversion: </b><br>
 * For now, all textures are internally represented as floating point textures
 * with color values in the range [0.0, 1.0]. Non-fp types are scaled to the
 * range [0.0, 1.0] by dividing the UNSIGNED component value by (2^N - 1), where
 * N is the number of bits used to represent that component.
 * </p>
 * <p>
 * For formats of XYZ, this is the bitcount of the primitive type. <br>
 * For XYZ_123, the packed number of bits per component is used (ex. red and
 * blue values in RGB_565 are divided by 31 and green values are divided by 63).
 * </p>
 * <p>
 * Formats using data with type FLOAT will be clamped to [0.0, 1.0] with one
 * exception: <br>
 * If the format is XYZ_FLOAT, the renderer must attempt to leverage unclamped
 * floating-point textures. This is only available on newer hardware. If it's
 * not available, the renderer may decide to use normal clamped float values.
 * </p>
 * <p>
 * <b>An important note about formats:</b> the unclamped formats are only
 * available if a RenderCapabilities returns true in its
 * getUnclampedTextureSupport(). <br>
 * Similarly, the DXT_n options are only available if
 * getS3TCTextureCompression() is true. <br>
 * The DEPTH format is only usable in Textur1D, Texture2D and TextureRectangle.
 * TextureSurfaces created for the other target types will not have a depth
 * image of the same target (since that's not available, a 2d image will be used
 * instead).
 * </p>
 * 
 * @author Michael Ludwig
 */
public enum TextureFormat {
	RGBA(null, 4, true), RGBA_4444(DataType.UNSIGNED_SHORT, 1, true, true),
	RGBA_8888(DataType.UNSIGNED_INT, 1, true, true), RGBA_5551(
		DataType.UNSIGNED_SHORT, 1, true, true), RGBA_FLOAT(DataType.FLOAT, 4,
		true),

	RGBA_DXT1(DataType.UNSIGNED_BYTE, -1, true), RGBA_DXT3(
		DataType.UNSIGNED_BYTE, -1, true), RGBA_DXT5(DataType.UNSIGNED_BYTE,
		-1, true),

	BGRA(null, 4, true), BGRA_4444(DataType.UNSIGNED_SHORT, 1, true, true),
	BGRA_8888(DataType.UNSIGNED_INT, 1, true, true), BGRA_5551(
		DataType.UNSIGNED_SHORT, 1, true, true),

	ARGB_4444(DataType.UNSIGNED_SHORT, 1, true, true), ARGB_1555(
		DataType.UNSIGNED_SHORT, 1, true, true), ARGB_8888(
		DataType.UNSIGNED_INT, 1, true, true),

	ABGR_4444(DataType.UNSIGNED_SHORT, 1, true, true), ABGR_1555(
		DataType.UNSIGNED_SHORT, 1, true, true), ABGR_8888(
		DataType.UNSIGNED_INT, 1, true, true),

	RGB(null, 3, false), RGB_565(DataType.UNSIGNED_SHORT, 1, false, true),
	RGB_FLOAT(DataType.FLOAT, 3, false), RGB_DXT1(DataType.UNSIGNED_BYTE, -1,
		false),

	BGR(null, 3, false), BGR_565(DataType.UNSIGNED_SHORT, 1, false, true),

	LUMINANCE_ALPHA(null, 2, true), LUMINANCE(null, 1, false), ALPHA(null, 1,
		true),

	LUMINANCE_ALPHA_FLOAT(DataType.FLOAT, 2, true), LUMINANCE_FLOAT(
		DataType.FLOAT, 1, false), ALPHA_FLOAT(DataType.FLOAT, 1, true),

	DEPTH(null, 1, false);

	private DataType type;
	private boolean hasAlpha, isPacked;
	private int pPerC;

	private TextureFormat(DataType type, int pPerC, boolean alpha) {
		this(type, pPerC, alpha, false);
	}

	private TextureFormat(DataType type, int pPerC, boolean alpha,
		boolean packed) {
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
	 * Return the data type that is required by the TextureFormat. If null is
	 * returned, then any of the UNSIGNED_X types are allowed, as well as FLOAT.
	 * 
	 * @return The required DataType of texture data for this format, may be
	 *         null
	 */
	public DataType getSupportedType() {
		return type;
	}

	/**
	 * Whether or not the given type is supported by this format. Returns false
	 * if type is null.
	 * 
	 * @see #getSupportedType()
	 * @param type The DataType to check for support by this format
	 * @return True if this format can be used with type
	 */
	public boolean isTypeValid(DataType type) {
		if (type == null)
			return false;

		if (this.type == null)
			return type == DataType.FLOAT || type.isUnsigned();
		else
			return type == this.type;
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
	 * values are 1 or 2, which are also allowed).
	 * </p>
	 * 
	 * @param width The width of the texture image
	 * @param height The height of the texture image
	 * @param depth The depth of the texture image
	 * @return The number of primitives required to hold all texture data for a
	 *         texture of the given dimensions with this format
	 */
	public int getBufferSize(int width, int height, int depth) {
		if (width <= 0 || height <= 0 || depth <= 0)
			return -1;
		if (isCompressed() && (width % 4 != 0 || height % 4 != 0))
			// compression needs to have multiple of 4 dimensions
			if (width != 1 && width != 2 && height != 1 && height != 2)
				return -1;

		if (isCompressed())
			return (int) ((this == RGBA_DXT1 || this == RGB_DXT1 ? 8 : 16)
				* Math.ceil(width / 4f) * Math.ceil(height / 4f));
		else
			return width * height * depth * getPrimitivesPerColor();
	}
}
