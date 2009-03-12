package com.ferox.renderer;

/** 
 * DisplayOptions represents a set of configurable surface parameters
 * used when creating render surfaces. 
 * 
 * When creating a surface, these values should be deemed as a request
 * and the renderer must satisfy them as best as possible.
 * 
 * DisplayOptions are immutable after they are created.
 * 
 * @author Michael Ludwig
 *
 */
public final class DisplayOptions {
	/** The format for the color pixels of the surface. 
	 * It is likely that the float options are only available
	 * for texture surfaces. */
	public static enum PixelFormat {
		RGB_16BIT, /** Red, green, and blue pixels will be packed together in a 5/6/5 bit scheme. */
		RGB_24BIT, /** Red, green, and blue pixels are packed in a 8/8/8 bit scheme. */
		RGBA_32BIT, /** Red, green, blue, and alpha pixels are packed in a 8/8/8/8 scheme. */
		RGB_FLOAT, /** Red, green and blue components are each stored as 32 bit, unclamped floating point values. */
		RGBA_FLOAT, /** Red, green, blue and alpha components are each stored as 32 bit, unclamped floating point values. */
		NONE /** There should be no color data associated with the surface.  Only valid for texture surfaces. */
	}
	
	/** The format for the depth component of the surface fragment. */
	public static enum DepthFormat {
		DEPTH_16BIT, /** Use 16 bits to store depth information. */
		DEPTH_24BIT, /** Use 24 bits to store depth information. */
		DEPTH_32BIT, /** Use 32 bits to store depth information. */
		NONE /** There should be no depth buffer. */
	}
	
	/** The format for the stencil buffer fo the surface. */
	public static enum StencilFormat {
		STENCIL_16BIT, /** Use 16 bits for each fragment. */
		STENCIL_8BIT, /** Use 8 bits for each fragment. */
		STENCIL_4BIT, /** Use 4 bits for each fragment. */
		STENCIL_1BIT, /** Use only 1 bit for each fragment. */
		NONE /** There shouldn't be any stencil buffer. */
	}
	
	/** The type of fullscreen anti-aliasing to apply to the surface.
	 * It is more likely that windowed and fullscreen surfaces can be
	 * anti-aliased, however newer hardware can support aa'ed texture surfaces. */
	public static enum AntiAliasMode {
		TWO_X, FOUR_X, EIGHT_X, NONE
	}
	
	private static final PixelFormat DEFAULT_PF = PixelFormat.RGB_24BIT;
	private static final DepthFormat DEFAULT_DF = DepthFormat.DEPTH_24BIT;
	private static final StencilFormat DEFAULT_SF = StencilFormat.NONE;
	private static final AntiAliasMode DEFAULT_AA = AntiAliasMode.NONE;
	
	private PixelFormat pixelFormat;
	private DepthFormat depthFormat;
	private StencilFormat stencilFormat;
	private AntiAliasMode aaMode;
	
	/** Create a DisplayOptions, equivalent to (RGB_24BIT, DEPTH_24BIT, NONE, NONE). */
	public DisplayOptions() {
		this(null);
	}
	
	/** Create a DisplayOptions, equivalent to (pixels, DEPTH_24BIT, NONE, NONE). */
	public DisplayOptions(PixelFormat pixels) {
		this(pixels, (DepthFormat) null);
	}
	
	/** Create a DisplayOptions, equivalent to (pixels, DEPTH_24BIT, NONE, aa). */
	public DisplayOptions(PixelFormat pixels, AntiAliasMode aa) {
		this(pixels, null, null, aa);
	}
	
	/** Create a DisplayOptions, equivalent to (pixels, depth, NONE, NONE). */
	public DisplayOptions(PixelFormat pixels, DepthFormat depth) {
		this(pixels, depth, null, null);
	}
	
	/** Create a DisplayOptions using pixels, depth, stencil and aa. 
	 * If pixels is null, RGB_24BIT is used.  If depth is null, DEPTH_24BIT is used.
	 * If stencil or aa are null, NONE is used. */
	public DisplayOptions(PixelFormat pixels, DepthFormat depth, StencilFormat stencil, AntiAliasMode aa) {
		this.pixelFormat = (pixels == null ? DEFAULT_PF : pixels);
		this.depthFormat = (depth == null ? DEFAULT_DF : depth);
		this.stencilFormat = (stencil == null ? DEFAULT_SF : stencil);
		this.aaMode = (aa == null ? DEFAULT_AA : aa);
	}

	/** Get the pixel format for this display options. */
	public PixelFormat getPixelFormat() {
		return this.pixelFormat;
	}
	
	/** Get the depth format for this display options. */
	public DepthFormat getDepthFormat() {
		return this.depthFormat;
	}
	
	/** Get the stencil format for this display options. */
	public StencilFormat getStencilFormat() {
		return this.stencilFormat;
	}
	
	/** Get the anti-alias mode for this display options. */
	public AntiAliasMode getAntiAliasing() {
		return this.aaMode;
	}
	
	@Override
	public String toString() {
		return this.pixelFormat + " " + this.depthFormat + " " + this.stencilFormat + " " + this.aaMode;
	}
}
