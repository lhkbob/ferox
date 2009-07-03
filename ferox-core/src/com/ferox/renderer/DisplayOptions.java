package com.ferox.renderer;

/**
 * <p>
 * DisplayOptions represents a set of configurable surface parameters used when
 * creating render surfaces.
 * </p>
 * <p>
 * When creating a surface, these values should be deemed as a request and the
 * renderer must satisfy them as best as possible.
 * </p>
 * <p>
 * DisplayOptions are immutable after they are created.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class DisplayOptions {
	/**
	 * The format for the color pixels of the surface. It is likely that the
	 * float options are only available for texture surfaces.
	 */
	public static enum PixelFormat {
		/**
		 * Red, green, and blue pixels will be packed together in a 5/6/5 bit
		 * scheme.
		 */
		RGB_16BIT,
		/**
		 * Red, green, and blue pixels are packed in a 8/8/8 bit scheme.
		 */
		RGB_24BIT,
		/**
		 * Red, green, blue, and alpha pixels are packed in a 8/8/8/8 scheme.
		 */
		RGBA_32BIT,
		/**
		 * Red, green and blue components are each stored as 32 bit, unclamped
		 * floating point values.
		 */
		RGB_FLOAT,
		/**
		 * Red, green, blue and alpha components are each stored as 32 bit,
		 * unclamped floating point values.
		 */
		RGBA_FLOAT,
		/**
		 * There should be no color data associated with the surface. Only valid
		 * for texture surfaces.
		 */
		NONE
	}

	/** The format for the depth component of the surface fragment. */
	public static enum DepthFormat {
		/** Use 16 bits to store depth information. */
		DEPTH_16BIT,
		/** Use 24 bits to store depth information. */
		DEPTH_24BIT,
		/** Use 32 bits to store depth information. */
		DEPTH_32BIT,
		/** There should be no depth buffer. */
		NONE
	}

	/** The format for the stencil buffer of the surface. */
	public static enum StencilFormat {
		/** Use 16 bits for each fragment. */
		STENCIL_16BIT,
		/** Use 8 bits for each fragment. */
		STENCIL_8BIT,
		/** Use 4 bits for each fragment. */
		STENCIL_4BIT,
		/** Use only 1 bit for each fragment. */
		STENCIL_1BIT,
		/** There shouldn't be any stencil buffer. */
		NONE
	}

	/**
	 * The type of fullscreen anti-aliasing to apply to the surface. It is more
	 * likely that windowed and fullscreen surfaces can be anti-aliased, however
	 * newer hardware can support aa'ed texture surfaces.
	 */
	public static enum AntiAliasMode {
		TWO_X, FOUR_X, EIGHT_X, NONE
	}

	private static final PixelFormat DEFAULT_PF = PixelFormat.RGB_24BIT;
	private static final DepthFormat DEFAULT_DF = DepthFormat.DEPTH_24BIT;
	private static final StencilFormat DEFAULT_SF = StencilFormat.NONE;
	private static final AntiAliasMode DEFAULT_AA = AntiAliasMode.NONE;

	private final PixelFormat pixelFormat;
	private final DepthFormat depthFormat;
	private final StencilFormat stencilFormat;
	private final AntiAliasMode aaMode;

	/**
	 * Create a DisplayOptions, equivalent to (RGB_24BIT, DEPTH_24BIT, NONE,
	 * NONE).
	 */
	public DisplayOptions() {
		this(null);
	}

	/**
	 * Create a DisplayOptions, equivalent to (pixels, DEPTH_24BIT, NONE, NONE).
	 * 
	 * @param pixels The PixelFormat to use, null = RGB_24BIT
	 */
	public DisplayOptions(PixelFormat pixels) {
		this(pixels, (DepthFormat) null);
	}

	/**
	 * Create a DisplayOptions, equivalent to (pixels, DEPTH_24BIT, NONE, aa).
	 * 
	 * @param pixels The PixelFormat to use, null = RGB_24BIT
	 * @param aa The AntiAliasMode to use, null = NONE
	 */
	public DisplayOptions(PixelFormat pixels, AntiAliasMode aa) {
		this(pixels, null, null, aa);
	}

	/**
	 * Create a DisplayOptions, equivalent to (pixels, depth, NONE, NONE).
	 * 
	 * @param pixels The PixelFormat to use, null = RGB_24BIT
	 * @param depth The DepthFormt to use, null = DEPTH_24BIT
	 */
	public DisplayOptions(PixelFormat pixels, DepthFormat depth) {
		this(pixels, depth, null, null);
	}

	/**
	 * Create a DisplayOptions using pixels, depth, stencil and aa.
	 * 
	 * @param pixels The PixelFormat to use, null = RGB_24BIT
	 * @param depth The DepthFormt to use, null = DEPTH_24BIT
	 * @param stencil The StencilFormat to use, null = NONE
	 * @param aa The AntiAliasMode to use, null = NONE
	 */
	public DisplayOptions(PixelFormat pixels, DepthFormat depth, StencilFormat stencil, AntiAliasMode aa) {
		pixelFormat = (pixels == null ? DEFAULT_PF : pixels);
		depthFormat = (depth == null ? DEFAULT_DF : depth);
		stencilFormat = (stencil == null ? DEFAULT_SF : stencil);
		aaMode = (aa == null ? DEFAULT_AA : aa);
	}

	/**
	 * Get the pixel format for this display options.
	 * 
	 * @return The PixelFormat of the DisplayOptions
	 */
	public PixelFormat getPixelFormat() {
		return pixelFormat;
	}

	/**
	 * Get the depth format for this display options.
	 * 
	 * @return The DepthFormat of the DisplayOptions
	 */
	public DepthFormat getDepthFormat() {
		return depthFormat;
	}

	/**
	 * Get the stencil format for this display options.
	 * 
	 * @return The StencilFormat of the DisplayOptions
	 */
	public StencilFormat getStencilFormat() {
		return stencilFormat;
	}

	/**
	 * Get the anti-alias mode for this display options.
	 * 
	 * @return The AntiAliasMode of the DisplayOptions
	 */
	public AntiAliasMode getAntiAliasing() {
		return aaMode;
	}

	@Override
	public String toString() {
		return "(DisplayOptions " + pixelFormat + " " + depthFormat 
			   + " " + stencilFormat + " " + aaMode + ")";
	}
}
