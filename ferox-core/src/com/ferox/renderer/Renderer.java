package com.ferox.renderer;

import com.ferox.math.Color4f;
import com.ferox.resource.Geometry;

/**
 * <p>
 * A Renderer is an interface that allows actual rendering to be performed. It
 * is intended that Framework implementations internally create a Renderer that
 * they use as needed when implementing render().
 * </p>
 * <p>
 * Internally it may be required by a Framework for Renderer code to be
 * executed on a separate thread from the Framework's.  Because of this Renderer instances should
 * not be held onto externally, but should only be used in method calls that
 * take them as arguments (e.g. in RenderPass).
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Renderer {
	/**
	 * <p>
	 * BlendFactor describes a function that outputs scaling factors for each
	 * component of a color, where the color is either the source or destination
	 * pixel color used by {@link BlendFunction}. The BlendFactor outputs for
	 * scaling factors, where each corresponds to red, green, blue, or alpha.
	 * </p>
	 * <p>
	 * In the following descriptions of the enums, the following variables are
	 * used:
	 * <ul>
	 * <li>(Rs, Gs, Bs, As) = RGBA values for the source/incoming pixel</li>
	 * <li>(Rd, Gd, Bd, Ad) = RGBA values for the destination/previous pixel</li>
	 * <li>(Rc, Gc, Bc, Ac) = RGBA values of the configured blending color</li>
	 * </ul>
	 * </p>
	 */
	public static enum BlendFactor {
		/** factor = (0, 0, 0, 0) */
		ZERO, 
		/** factor = (1, 1, 1, 1) */
		ONE, 
		/** factor = (Rs, Gs, Bs, As) */
		SRC_COLOR, 
		/** factor = (1 - Rs, 1 - Gs, 1 - Bs, 1 - As) */
		ONE_MINUS_SRC_COLOR, 
		/** factor = (As, As, As, As) */
		SRC_ALPHA,
		/** factor = (1 - As, 1 - As, 1 - As, 1 - As) */
		ONE_MINUS_SRC_ALPHA, 
		/** 
		 * f = min(As, 1 - Ad)
		 * factor = (f, f, f, 1)
		 * Can only be used as a source BlendFactor.
		 */
		SRC_ALPHA_SATURATE,
		/** factor = (Rd, Gd, Bd, Ad) */
		DST_COLOR, 
		/** factor = (1 - Rd, 1 - Gd, 1 - Bd, 1 - Ad) */
		ONE_MINUS_DST_COLOR,
		/** factor = (Ad, Ad, Ad, Ad) */
		DST_ALPHA,
		/** factor = (1 - Ad, 1 - Ad, 1 - Ad, 1 - Ad) */
		ONE_MINUS_DST_ALPHA, 
		/** factor = (Rc, Gc, Bc, Ac) */
		CONSTANT_COLOR, 
		/** factor = (Ac, Ac, Ac, Ac) */
		CONSTANT_ALPHA,
		/** factor = (1 - Rc, 1 - Gc, 1 - Bc, 1 - Ac) */
		ONE_MINUS_CONSTANT_COLOR, 
		/** factor = (1 - Ac, 1 - Ac, 1 - Ac, 1 - Ac) */
		ONE_MINUS_CONSTANT_ALPHA
	}
	
	/**
	 * When blending is enabled, incoming pixels are combined with the previous
	 * pixels based on the configured BlendFunction and {@link BlendFactor}'s.
	 * In the function enums listed below, the following variables are used:
	 * <ul>
	 * <li>sC = Incoming pixel color</li>
	 * <li>dC = Previous pixel color</li>
	 * <li>fC = Computed final color</li>
	 * <li>bS = Blending factor for incoming color, based off of the source's
	 * configured BlendFactor</li>
	 * <li>bD = Blending factor for the previous color, based off of the
	 * destinations's configured BlendFactor</li>
	 * </ul>
	 * All operations are done component-wise across the color.
	 */
	public static enum BlendFunction {
		/**
		 * ADD blends the two colors together by adding their scaled components
		 * together:<br>
		 * fC = sC * bS + dC + bD
		 */
		ADD, 
		/**
		 * SUBTRACT blends the two colors by subtracting the scaled source color
		 * from the scaled destination:<br>
		 * fC = sC * bS - dC * bD
		 */
		SUBTRACT, 
		/**
		 * REVERSE_SUBTRACT blends the two colors by subtracting the scaled
		 * destination from the scaled source:<br>
		 * fC = dC * bD - sC * bS
		 */
		REVERSE_SUBTRACT, 
		/**
		 * MIN computes the final color, where each component is the smallest
		 * value between the source and destination. The blending factors are
		 * ignored:<br>
		 * fC = min(sC, dC)
		 */
		MIN,
		/** 
		 * MIN computes the final color, where each component is the largest
		 * value between the source and destination. The blending factors are
		 * ignored:<br>
		 * fC = ax(sC, dC) */
		MAX
	}
	
	/**
	 * <p>
	 * Comparison is an enum that represents a function that maps from two
	 * inputs to boolean values. It is often used for various pixel tests, where
	 * if it returns true the pixel continues down the pipeline, else the pixel
	 * is not rendered.
	 * <p>
	 * The two inputs always have the form of pixel value, and reference value.
	 * The pixel value is then compared to the reference value using the given
	 * Comparison function.  Sometimes the reference value is defined as a constant,
	 * and other times it's the previous pixel value.  In the case of NEVER and
	 * ALWAYS, the inputs are ignored.
	 * </p>
	 */
	public static enum Comparison {
		/** Returns true if the pixel value is equal to the reference value. */
		EQUAL, 
		/** Returns true if the pixel value is greater than the reference value. */
		GREATER, 
		/** Returns true if the pixel value is less than the reference value. */
		LESS, 
		/**
		 * Returns true if the pixel value is greater than or equal to the
		 * reference value.
		 */
		GEQUAL, 
		/**
		 * Returns true if the pixel value is less than or equal to the
		 * reference value.
		 */
		LEQUAL,
		/** Returns true if the pixel value is not equal to the reference value. */
		NOT_EQUAL, 
		/** Always returns false, regardless of the inputs. */
		NEVER, 
		/** Always returns true, regardless of the inputs. */
		ALWAYS
	}

	/**
	 * DrawStyle is an enum that represents the different ways that a polygon
	 * can be rendered. The Renderer interface exposes separate configuration
	 * points for front-facing and back-facing polygons. The facing of a polygon
	 * is determined by the counter-clockwise ordering of its vertices.
	 */
	public static enum DrawStyle {
		/** Polygons are rendered as solid regions. */
		SOLID, 
		/** Polygons are rendered as line segments around its edges. */
		LINE, 
		/** Polygons are rendered as points on its vertices. */
		POINT, 
		/** Polygons are not rendered, this effectively culls the polygon. */
		NONE
	}

	/**
	 * <p>
	 * When the stencil test is enabled, and the RenderSurface has a stencil
	 * buffer, the stencil test can be performed. When the test is active, there
	 * are three stages where incoming pixels can affect the values within the
	 * stencil buffer.
	 * </p>
	 * <p>
	 * The way a pixel affects the buffer is determined by the configured
	 * StencilOp at each of the following stages: failing the stencil test,
	 * failing the depth test, and passing the depth test. These tests are
	 * listed in the order that they are performed, so only one stencil update
	 * operation will occur for a given pixel.
	 * </p>
	 * <p>
	 * Keep in mind that the precision of a stencil buffer is wholly dependent
	 * on the number of bits within a RenderSurface's stencil buffer (which is
	 * usually limited to 2 to 8).
	 * </p>
	 */
	public static enum StencilOp {
		/** Keeps the stencil's value the same */
		KEEP, 
		/** Sets the stencil's value to 0 */
		ZERO, 
		/** Replaces the stencil's value with the reference value */
		REPLACE, 
		/** Add one to the stencil's current value, clamping it to the max */
		INCREMENT,
		/** Subtract one from the stencil's current value, clamping it to 0 */
		DECREMENT, 
		/** Bitwise invert the stencil's value */
		INVERT, 
		/** Add one to the stencil's value, wrapping around to 0 */
		INCREMENT_WRAP,
		/** Subtract one from the stencil's value, wrapping around to the max value */
		DECREMENT_WRAP
	}
	
	public void setStencilUpdateOps(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass);
	
	public void setStencilUpdateOpsFront(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass);
	
	public void setStencilUpdateOpsBack(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass);
	
	public void setStencilTest(Comparison test, int refValue, int testMask);
	
	public void setStencilTestFront(Comparison test, int refValue, int testMask);
	
	public void setStencilTestBack(Comparison test, int refValue, int testMask);
	
	public void setStencilTestEnabled(boolean enable);
	
	public void setStencilMask(int mask);
	
	public void setStencilMask(int front, int back);

	public void setDepthTest(Comparison test);
	
	public void setDepthWriteMask(boolean mask);
	
	public void setDepthOffsets(float factor, float units);
	
	public void setDepthOffsetsEnabled(boolean enable);
	
	public void setDrawStyle(DrawStyle style);
	
	public void setDrawStyle(DrawStyle front, DrawStyle back);

	/**
	 * <p>
	 * Set the color masks for each of the four available color components. If a
	 * color component's corresponding boolean is set to true, that component
	 * can be written into when storing a color value into the framebuffer. If
	 * it is false, that component will not be written into.
	 * </p>
	 * <p>
	 * When a color mask of (true, true, true, true) is used, the entire color
	 * will be written. A mask of (false, false, false, false) will write no
	 * color, even if the pixel passes all tests; however, in this case the
	 * pixel's depth could still be written and the stencil buffer could still
	 * be updated.
	 * </p>
	 * <p>
	 * When a component is disabled, it does not mean that a 0 is written for
	 * that component, but that the previous component at the pixel is
	 * preserved.
	 * </p>
	 * 
	 * @param red True if red color values can be written
	 * @param green True if green color values can be written
	 * @param blue True if blue color values can be written
	 * @param alpha True if alpha color values can be written
	 */
	public void setColorMask(boolean red, boolean green, boolean blue, boolean alpha);

	/**
	 * Set whether or not blending is enabled. Having blending enabled will
	 * likely decrease performance but it is necessary for proper transparent
	 * rendering. When blending is enabled, incoming pixels will be blended with
	 * the destination pixel based on the configured {@link BlendFunction} and
	 * {@link BlendFactor}s.
	 * 
	 * @param enable True if blending is enabled
	 */
	public void setBlendingEnabled(boolean enable);

	/**
	 * Set the 'constant' blend color used by certain {@link BlendFactor} enum
	 * values.
	 * 
	 * @param color The new blend color to use
	 * @throws NullPointerException if color is null
	 */
	public void setBlendColor(Color4f color);

	/**
	 * Set the {@link BlendFunction} and source and destination
	 * {@link BlendFactor}s for both RGB and alpha values.
	 * 
	 * @see #setBlendModeAlpha(BlendFunction, BlendFactor, BlendFactor)
	 * @see #setBlendModeRgb(BlendFunction, BlendFactor, BlendFactor)
	 * @param function The BlendFunction to use for both RGB and alpha values
	 * @param src The BlendFactor applied to source/incoming pixels
	 * @param dst The BlendFactor applied to destination/prior pixels
	 * @throws NullPointerException if function, src, or dst are null
	 * @throws IllegalArgumentException if dst is
	 *             {@link BlendFactor#SRC_ALPHA_SATURATE}
	 */
	public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst);

	/**
	 * Set the BlendFunction and source and destination BlendFactors that will
	 * be used to blend the RGB values together. This does not modify the
	 * blending functions used for alpha values, which can be blended separately
	 * from the RGB components.
	 * 
	 * @see #setBlendModeAlpha(BlendFunction, BlendFactor, BlendFactor)
	 * @param function The BlendFunction to use for RGB blending
	 * @param src The BlendFactor applied to source RGB values of the incoming
	 *            pixel
	 * @param dst The BlendFactor applied to the destination RGB values of the
	 *            prior pixel
	 * @throws NullPointerException if function, src, or dst are null
	 * @throws IllegalArgumentException if dst is
	 *             {@link BlendFactor#SRC_ALPHA_SATURATE}
	 */
	public void setBlendModeRgb(BlendFunction function, BlendFactor src, BlendFactor dst);

	/**
	 * Set the BlendFunction and source and destination BlendFactors that will
	 * be used to blend the alpha values together. This does not modify the
	 * blending functions used with RGB components, which are blended separately
	 * from alpha values.
	 * 
	 * @see #setBlendModeRgb(BlendFunction, BlendFactor, BlendFactor)
	 * @param function The BlendFunction to use for RGB blending
	 * @param src The BlendFactor applied to the source alpha value
	 * @param dst The BlendFactor applied to the destination alpha value
	 * @throws NullPointerException if function, src, or dst are null
	 * @throws IllegalArgumentException if dst is
	 *             {@link BlendFactor#SRC_ALPHA_SATURATE}
	 */
	public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst);
	
	public int render(Geometry g);
	
	public void reset();
}
