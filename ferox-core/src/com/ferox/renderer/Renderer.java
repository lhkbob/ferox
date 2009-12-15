package com.ferox.renderer;

import com.ferox.math.Color4f;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource.Status;

/**
 * <p>
 * Renderer is interface that enables access to low-level rendering operations.
 * It is intended to be used by {@link RenderPass}s, which are implemented to
 * render meaningful content into a RenderSurface. The link between a Renderer
 * and a RenderSurface is not visible, and is managed by Framework
 * implementations.
 * </p>
 * <p>
 * The Renderer is a state machine that controls how rendered Geometry's are
 * finally displayed. The available state is modeled after OpenGL and represents
 * the state that's common to both pre-3.0 versions and 3.1+ versions. By
 * itself, the Renderer does not contain enough state to fully describe how a
 * Geometry is rendered. The sub-interfaces {@link FixedFunctionRenderer} and
 * {@link GlslRenderer} add additional state that target the fixed functionality
 * available in older OpenGL systems, and the programmable pipeline enabled by
 * the GLSL language.
 * </p>
 * <p>
 * RenderPasses are not expected to work correctly on any type of Renderer, but
 * should instead target either the FixedFunctionRenderer or the GlslRenderer
 * and document which is required. Then programmers should use a Framework which
 * supports the required Renderer type.
 * </p>
 * <p>
 * Internally it may be required by a Framework for Renderer code to be executed
 * on a separate thread from the Framework's. Because of this Renderer instances
 * should not be held onto externally, but should only be used in method calls
 * that take them as arguments (e.g. in RenderPass).
 * </p>
 * <p>
 * At the start of each RenderPass, or when {@link #reset()} is called manually,
 * the Renderer's state is restored to the defaults described below. Sub-types
 * of Renderer will expose more state that will also be restored to their
 * defaults, too. The defaults are:
 * <ul>
 * <li>The blend color is (0, 0, 0, 0)</li>
 * <li>The BlendFunction for RGB and alpha values is both
 * {@link BlendFunction#ADD}</li>
 * <li>The BlendFactors for source RGB and alpha values are set to
 * {@link BlendFactor#ONE}</li>
 * <li>The BlendFactors for destination RGB and alpha values are set to
 * {@link BlendFactor#ZERO}</li>
 * <li>The color mask is (true, true, true, true)</li>
 * <li>The depth offset factor and units are both 0, and the depth offsets are
 * disabled</li>
 * <li>The depth test is set to {@link Comparison#LESS}</li>
 * <li>The depth write mask is set to true</li>
 * <li>The front DrawStyle is {@link DrawStyle#SOLID} and the back DrawStyle is
 * {@link DrawStyle#NONE}</li>
 * <li>The stencil mask for front and back stencils is set to all 1s (or ~0)</li>
 * <li>The stencil test is disabled</li>
 * <li>The stencil test is configured to use {@link Comparison#ALWAYS}, with a
 * reference value of 0 and a test mask of all 1s for both front and back facing
 * stencil tests</li>
 * <li>The stencil update operations for front and back tests all use
 * {@link StencilOp#KEEP}</li>
 * </ul>
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
		 * f = min(As, 1 - Ad) factor = (f, f, f, 1) Can only be used as a
		 * source BlendFactor.
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
		 * fC = ax(sC, dC)
		 */
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
	 * Comparison function. Sometimes the reference value is defined as a
	 * constant, and other times it's the previous pixel value. In the case of
	 * NEVER and ALWAYS, the inputs are ignored.
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
		/**
		 * Subtract one from the stencil's value, wrapping around to the max
		 * value
		 */
		DECREMENT_WRAP
	}

	/**
	 * <p>
	 * When the stencil test is enabled, there are three stages where the
	 * stencil buffer can be modified for a rendered pixel. For a single pixel's
	 * lifetime, only one modification will occur in one of the three times:
	 * <ol>
	 * <li>When the stencil test fails</li>
	 * <li>When the depth test fails</li>
	 * <li>When the depth test passes</li>
	 * </ol>
	 * When one of these three stages triggers, the stencil buffer is modified
	 * based on the configured {@link StencilOp} for that stage. As with the
	 * stencil test, front-facing polygons use one set of StencilOps and
	 * back-facing polygons use another set.
	 * </p>
	 * <p>
	 * This method configures both the front and back set of StencilOps to use
	 * the same set of three. Use
	 * {@link #setStencilUpdateOpsFront(StencilOp, StencilOp, StencilOp)} and
	 * {@link #setStencilUpdateOpsBack(StencilOp, StencilOp, StencilOp)} to
	 * configure the different sets independently.
	 * </p>
	 * 
	 * @param stencilFail The StencilOp applied when the stencil test fails
	 * @param depthFail The StencilOp applied when the depth test fails
	 * @param depthPass The StencilOp applied when the depth test passes
	 */
	public void setStencilUpdateOps(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass);

	/**
	 * This method sets the StencilOps that are applied with front-facing
	 * polygons. See
	 * {@link #setStencilUpdateOps(StencilOp, StencilOp, StencilOp)} for a
	 * description for details on when the updates are applied.
	 * 
	 * @param stencilFail The StencilOp applied when the stencil test fails for
	 *            front-facing polygons
	 * @param depthFail The StencilOp applied when the depth test fails for
	 *            front-facing polygons
	 * @param depthPass The StencilOp applied when the depth test passes for
	 *            front-facing polygons
	 */
	public void setStencilUpdateOpsFront(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass);

	/**
	 * This method sets the StencilOps that are applied with back-facing
	 * polygons. See
	 * {@link #setStencilUpdateOps(StencilOp, StencilOp, StencilOp)} for a
	 * description for details on when the updates are applied.
	 * 
	 * @param stencilFail The StencilOp applied when the stencil test fails for
	 *            back-facing polygons
	 * @param depthFail The StencilOp applied when the depth test fails for
	 *            back-facing polygons
	 * @param depthPass The StencilOp applied when the depth test passes for
	 *            back-facing polygons
	 */
	public void setStencilUpdateOpsBack(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass);

	/**
	 * <p>
	 * Set the Comparison and reference values used when performing the stencil
	 * test. This test will only be performed when it is enabled with
	 * {@link #setStencilTestEnabled(boolean)}. When the test is enabled, the
	 * current stencil value is AND'ed with <tt>testMask</tt> and then compared
	 * to <tt>(refValue & testMask)</tt>. Thus <tt>testMask</tt> acts as a
	 * bitwise mask to both the current stencil value and the assigned reference
	 * value.
	 * </p>
	 * <p>
	 * There are two different stencil test configurations available: one for
	 * front-facing polygons and one for back-facing polygons. Points and lines
	 * are considered to be front-facing polygons. Polygons rendered as points
	 * or lines, however, will be assigned the front or back test based on the
	 * CCW ordering of the polygon. This method configures both the front and
	 * the back stencil tests to use the same Comparison, reference value and
	 * test mask. The methods {@link #setStencilTestFront(Comparison, int, int)}
	 * and {@link #setStencilTestBack(Comparison, int, int)} can be used to
	 * control a specific stencil test.
	 * </p>
	 * 
	 * @param test The Comparison to use for stencil testing on all primitives
	 * @param refValue The reference value that the stencil buffer is compared
	 *            against
	 * @param testMask The bit mask AND'ed with the stencil buffer and refValue
	 *            before performing the comparison
	 * @throws NullPointerException if test is null
	 */
	public void setStencilTest(Comparison test, int refValue, int testMask);

	/**
	 * This method sets the stencil test for just front-facing polygons as
	 * described in {@link #setStencilTest(Comparison, int, int)}.
	 * 
	 * @param test The Comparison to use for stencil testing of front-facing
	 *            polygons
	 * @param refValue The reference value that the stencil buffer is compared
	 *            against
	 * @param testMask The bit mask AND'ed with the stencil buffer and refValue
	 *            before performing the comparison
	 * @throws NullPointerException if test is null
	 */
	public void setStencilTestFront(Comparison test, int refValue, int testMask);

	/**
	 * This method sets the stencil test for just back-facing polygons as
	 * described in {@link #setStencilTest(Comparison, int, int)}.
	 * 
	 * @param test The Comparison to use for stencil testing of back-facing
	 *            polygons
	 * @param refValue The reference value that the stencil buffer is compared
	 *            against
	 * @param testMask The bit mask AND'ed with the stencil buffer and refValue
	 *            before performing the comparison
	 * @throws NullPointerException if test is null
	 */
	public void setStencilTestBack(Comparison test, int refValue, int testMask);

	/**
	 * <p>
	 * Set whether or not the stencil test is enabled or disabled. When the
	 * stencil test is enabled, the stencil test that's been configured by
	 * {@link #setStencilTest(Comparison, int, int)} is performed for each
	 * rendered pixel. Depending on the result of that test and the subsequent
	 * depth test, the various stencil update operations will be performed and
	 * the stencil buffer will be updated.
	 * </p>
	 * <p>
	 * When rendering polygons, there are two different tests and set of update
	 * operations, one for front-facing polygons and one for back-facing
	 * polygons. Polygons rendered as points or lines are treated equivalently
	 * to filled polygons, however actual points and lines will always use the
	 * front set of stencil test state.
	 * </p>
	 * <p>
	 * When the stencil test is disabled, no stencil buffer related actions
	 * occur when rendering (e.g. no stencil update operations are performed,
	 * the test always 'passes' and the write masks are effectively ignored
	 * since nothing is written).
	 * </p>
	 * 
	 * @param enable True if the stencil test is enabled
	 */
	public void setStencilTestEnabled(boolean enable);

	/**
	 * Convenience function to set the front and back stencil masks to the same
	 * value. See {@link #setStencilWriteMask(int, int)} for more details on the
	 * stencil mask.
	 * 
	 * @param mask The stencil mask for front and back polygons
	 */
	public void setStencilWriteMask(int mask);

	/**
	 * Set the stencil masks that are applied to stencil values as they're
	 * written into the stencil buffer. The front mask is used when writing
	 * stencil values from polygons that are front-facing. Similarly, the back
	 * mask is used when writing stencil values from back polygons.</p>
	 * <p>
	 * When determining the final value that's written into the stencil buffer,
	 * only bits that are 1 within the appropriate mask have the corresponding
	 * bits from stencil value written. Thus a mask of all 1s writes the entire
	 * stencil value, and a mask of all 0s causes no value to be written.
	 * Although it resembles a bitwise AND, it is slightly different.
	 * </p>
	 * <p>
	 * Also, since stencil buffers often have extremely low numbers of bits
	 * (between 2 and 8), only the lowest N bits of each mask are used, where N
	 * is the resolution of the stencil buffer.
	 * </p>
	 * 
	 * @param front The stencil write mask applied to incoming stencil values
	 *            from front-facing polygons
	 * @param back The stencil write mask applied to incoming stencil values
	 *            from back-facing polygons
	 */
	public void setStencilWriteMask(int front, int back);

	/**
	 * <p>
	 * Set the Comparison used when comparing incoming pixels' depth values to
	 * the depth value stored in previously at that pixel's location in the
	 * depth buffer. The depth test can be used to correctly render intersecting
	 * 3D shapes, etc.
	 * </p>
	 * <p>
	 * Depth values are stored in a range of 0 to 1. By default RenderSurfaces
	 * clear their depth buffers to 1, which is why the default depth test
	 * comparison is LESS.
	 * </p>
	 * 
	 * @param test The Comparison to use for depth testing
	 * @throws NullPointerException if test is null
	 */
	public void setDepthTest(Comparison test);

	/**
	 * Set whether or not depth values can be written into the depth buffer once
	 * it has been determined that a pixel should be rendered. If <tt>mask</tt>
	 * is true then depth values for rendered pixels will be placed in the depth
	 * buffer, otherwise no depth is written. This depth mask is independent of
	 * any mask that's applied to the color components or stencil values that
	 * might be written for a pixel.
	 * 
	 * @param mask True if depth writing is enabled
	 */
	public void setDepthWriteMask(boolean mask);

	/**
	 * <p>
	 * Set the depth offset configuration values. The depth offset added to each
	 * pixel's depth is computed as follows:<br>
	 * <code>
	 * offset = factor * m + units * r.
	 * </code> <tt>offset</tt> is the value added to the pixel's window depth;
	 * <tt>factor</tt> and <tt>units</tt> are the values specified in this
	 * method; <tt>m</tt> is the maximum depth slope of the polygon that
	 * contained the rendered pixel; <tt>r</tt> is the minimum difference
	 * between depth values such that they are distinct once stored in the depth
	 * buffer.
	 * </p>
	 * <p>
	 * <tt>m</tt> is computed as the length of the vector <dz/dx, dz/dy>.
	 * <tt>r</tt> is implementation dependent since it depends on the size and
	 * format of the depth buffer in use for a RenderSurface.
	 * </p>
	 * 
	 * @param factor The scale factor applied to a polygon's max depth slope
	 * @param units The scale factor applied to the epsilon value of the depth
	 *            buffer
	 */
	public void setDepthOffsets(float factor, float units);

	/**
	 * <p>
	 * Enable or disable depth offsets. When depth offsets are enabled, all
	 * pixels have a depth value that is slightly offset from the original
	 * computed depth based on the projected vertex coordinate. The specific
	 * depth offset applied to each pixel depends on the depth buffer resolution
	 * and format, as well as the configured depth offset factor and units.
	 * </p>
	 * <p>
	 * The depth offset only applies to polygons, although for these purposes
	 * polygons rendered with a DrawStyle of POINT or LINE count as polygons.
	 * </p>
	 * 
	 * @see #setDepthOffsets(float, float)
	 * @param enable True if depth offsets should be enabled
	 */
	public void setDepthOffsetsEnabled(boolean enable);

	/**
	 * Convenience method to set the DrawStyle for both front and back-facing
	 * polygons to the same style.
	 * 
	 * @param style The new DrawStyle for both front and back polygons
	 * @throws NullPointerException if style is null
	 */
	public void setDrawStyle(DrawStyle style);

	/**
	 * <p>
	 * Set the DrawStyle to be used for front-facing polygons and back-facing
	 * polygons. The DrawStyle front specifies how front-facing polygons are
	 * rendered, and back specifies how back-facing polygons are rendered.
	 * </p>
	 * <p>
	 * The facing of a polygon is determined by the counter-clockwise ordering
	 * of its vertices. When a polygon is rendered, if it's vertices are
	 * specified in counter-clockwise order then the polygon 'faces' the viewer.
	 * If they're presented in clockwise order the polygon faces away from the
	 * viewer.
	 * </p>
	 * <p>
	 * Although it's possible to have both front and back set to
	 * {@link DrawStyle#NONE}, it generally will make little sense, since every
	 * polygon would then be culled.
	 * </p>
	 * 
	 * @see DrawStyle
	 * @param front The DrawStyle for front-facing polygons
	 * @param back The DrawStyle for back-facing polygons
	 * @throws NullPointerException if front or back are null
	 */
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
	public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha);

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

	/**
	 * <p>
	 * Render the given Geometry with the current state configuration of the
	 * Renderer. If the Geometry has any pending updates or hasn't been used by
	 * the Framework before, it will be updated as described in
	 * {@link Framework#update(com.ferox.resource.Resource, boolean)} before
	 * proceeding with the rendering as normal. If, after the Geometry is
	 * ensured to be up-to-date, and its status is not {@link Status#READY},
	 * then nothing will be rendered and 0 will be returned by this method.
	 * </p>
	 * <p>
	 * If the Geometry is ready for use, then it will be rendered onto the
	 * active RenderSurface. The mapping from vertex attributes in the Geometry
	 * to the coordinates rendered by the Renderer are determined by
	 * sub-interface configuration. In FixedFunctionRenderer, the attribute name
	 * for vertices, normals, and texture coordinates must be specified. In
	 * GlslRenderer, the attribute names are linked to GLSL shader attributes
	 * and then the GLSL shader computes vertices, etc. from these attributes.
	 * </p>
	 * <p>
	 * Based on the attribute configurations, a Geometry not be rendered if it
	 * does not contain a vertex attribute of a required name. See the Renderer
	 * subtypes for more information.
	 * </p>
	 * 
	 * @param g The Geometry to be rendered
	 * @return The number of polygons that were rendered, or 0 if the Geometry
	 *         was not rendered
	 * @throws NullPointerException if g is null
	 * @throws RenderException if this Renderer was interrupted by the Framework
	 *             so that it can be destroyed in a timely manner
	 */
	public int render(Geometry g);

	/**
	 * Manually reset all of the OpenGL-related state in this Renderer to the
	 * defaults described in the interface-level documentation. It is not
	 * necessary to invoke this method at the start of a RenderPass because the
	 * Framework will have already restored everything to the defaults for each
	 * pass invocation.
	 */
	public void reset();

	/**
	 * <p>
	 * Clear the framebuffers of the current RenderSurface, as described in
	 * {@link Framework#queue(RenderSurface, RenderPass, boolean, boolean, boolean, Color4f, float, int)}
	 * . There is one primary difference, however. If the color buffer is
	 * cleared, the color value written into the buffer is affected by the
	 * current color mask state. If the depth buffer is cleared, the depth value
	 * written is affected by the current depth mask state. If the stencil
	 * buffer is cleared, the stencil value written is affected by the current
	 * stencil mask configured for front-facing polygons.
	 * </p>
	 * <p>
	 * RenderPasses are not required to invoke this method since buffer clearing
	 * can be scheduled when a RenderPass and RenderSurface are scheduled. This
	 * is provided in case complex graphical effects require different buffer
	 * clearing behavior in the middle of a RenderPass operation.
	 * </p>
	 * 
	 * @param clearColor True if the color buffer is cleared
	 * @param clearDepth True if the depth buffer is cleared
	 * @param clearStencil True if the stencil buffer is cleared
	 * @param color The color that the color buffer is cleared to
	 * @param depth The depth value that depth buffer is cleared to, in [0, 1]
	 * @param stencil The stencil value that the stencil buffer is cleared to
	 * @throws NullPointerException if color is null when clearColor is true
	 * @throws IllegalArgumentException if depth is not in [0, 1] when
	 *             clearDepth is true
	 */
	public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, Color4f color, float depth, int stencil);
}
