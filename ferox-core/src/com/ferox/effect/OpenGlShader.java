package com.ferox.effect;

import com.ferox.effect.BlendMode.BlendFunction;
import com.ferox.effect.BlendMode.BlendFactor;
import com.ferox.effect.StencilTest.StencilUpdateOperation;
import com.ferox.math.Color4f;

/**
 * <p>
 * OpenGlShader is a base Shader implementation that models itself after the
 * capabilities of an OpenGl system. It provides access to state that is
 * included in both GL 3.0 and earlier, and the later versions 3.1+. It's
 * intended as a concrete base for Shaders targeted at a given set of OpenGL
 * versions.
 * </p>
 * <p>
 * At the moment there are only two complete sub-classes:
 * {@link FixedFunctionShader}, which is targeted at GL 3.0 and earlier systems
 * without access to the programmable shader pipeline; and {@link GlslShader},
 * which is targeted at 3.1+ and forward compatible 2.0+ systems that only use
 * the programmable shader.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The fully defined subclass of OpenGlShader
 */
@SuppressWarnings("unchecked")
public abstract class OpenGlShader<T extends OpenGlShader<T>> implements Shader {
	/**
	 * DrawStyle is an enum that specifies how a rendered polygon is actually
	 * drawn into a RenderSurface. If it's SOLID, the polygon is filled entirely
	 * as would be expected. If it's LINE or POINT, the polygon's edges or
	 * vertices are the only pixels that are rendered, using the current
	 * configuration for line or point styles. If it's NONE, then the polygon is
	 * culled and not drawn at all.
	 */
	public static enum DrawStyle {
		SOLID, LINE, POINT, NONE
	}
	
	// blending
	private final BlendMode blendRgb;
	private final BlendMode blendAlpha;
	
	private boolean enableBlending;
	private final Color4f blendColor;
	
	// polygon/line/point styles
	private DrawStyle frontStyle;
	private DrawStyle backStyle;
	
	// depth offsets
	private boolean offsetEnabled;
	private float offsetFactor;
	private float offsetUnits;
	
	// depth masking
	private Comparison depthTest;
	private boolean depthWriteMask;
	
	// stencil masking
	private final StencilTest stencilFront;
	private final StencilTest stencilBack;
	private boolean enableStencilTest;
	
	private int stencilMaskFront;
	private int stencilMaskBack;
	
	// color masking
	private boolean colorMaskRed;
	private boolean colorMaskGreen;
	private boolean colorMaskBlue;
	private boolean colorMaskAlpha;

	/**
	 * Create a new OpenGlShader that is shapes are rendered without blending,
	 * or the stencil test, a default depth test, no depth offsets, and with
	 * only front faces.
	 */
	public OpenGlShader() {
		blendAlpha = new BlendMode();
		blendRgb = new BlendMode();
		
		stencilFront = new StencilTest();
		stencilBack = new StencilTest();
		
		blendColor = new Color4f();
		
		setDrawStyle(DrawStyle.SOLID, DrawStyle.NONE);
		
		setDepthTest(Comparison.LEQUAL);
		setDepthWriteEnabled(true);
		
		setDepthOffsets(0f, 0f);
		setDepthOffsetEnabled(false);
		
		setColorChannelsEnabled(true, true, true, true);
		
		setStencilWriteMask(~0);
		setStencilTestEnabled(false);
	}
	
	/**
	 * Get the offset factor to be applied to polygons, lines, and points
	 * rendered with this Shader, if depth offsets are enabled.
	 * 
	 * @return The offset factor to use
	 */
	public float getDepthOffsetFactor() {
		return offsetFactor;
	}

	/**
	 * Get the offset units used when computing the final depth offset for
	 * rendered primitives. This works in conjunction with the offset factor,
	 * and only applies if depth offsets are enabled.
	 * 
	 * @return The offset units to use
	 */
	public float getDepthOffsetUnits() {
		return offsetUnits;
	}

	/**
	 * Set the offset factor and offset units to use with the depth offset. The
	 * exact effect of the factor and units are dependent on the graphics card
	 * and OpenGL drivers used on the current machine. These values will have no
	 * effect unless depth offsets are enabled with
	 * {@link #setDepthOffsetEnabled(boolean)}.
	 * 
	 * @param factor The new offset factor to use
	 * @param units The new offset units to use
	 * @return This Shader
	 */
	public T setDepthOffsets(float factor, float units) {
		offsetFactor = factor;
		offsetUnits = units;
		
		return (T) this;
	}

	/**
	 * Return whether or not depth offsets are enabled. If this returns true
	 * then the offset factor and units will modify the final depth value of a
	 * rendered primitive. This can be used to create billboarded polygons that
	 * don't z-fight with previously rendered surfaces.
	 * 
	 * @return True if depth offsets are enabled
	 */
	public boolean getDepthOffsetEnabled() {
		return offsetEnabled;
	}

	/**
	 * Set whether or not depth offsets are enabled. See
	 * {@link #getOffsetEnabled()}, {@link #getOffsetFactor()}, and
	 * {@link #getOffsetUnits()} for a description of what depth offsets
	 * accomplish.
	 * 
	 * @param enabled True if depth offsets should be enabled
	 * @return This Shader
	 */
	public T setDepthOffsetEnabled(boolean enabled) {
		offsetEnabled = enabled;
		return (T) this;
	}

	/**
	 * Return whether or not pixel blending is enabled. If this is true then the
	 * configured BlendModes for rgb and alpha values will be used to compute
	 * the final pixel color rendered.
	 * 
	 * @return Whether or not blending is enabled
	 */
	public boolean isBlendingEnabled() {
		return enableBlending;
	}

	/**
	 * Set whether or not blending is enabled.
	 * 
	 * @param enabled True if the rgb and alpha BlendModes should be used
	 * @return This Shader
	 */
	public T setBlendingEnabled(boolean enabled) {
		enableBlending = enabled;
		return (T) this;
	}

	/**
	 * Convenience function to set both the rgb and alpha BlendModes to use the
	 * given BlendFunction and BlendFactors for source and destination colors.
	 * 
	 * @param eq The new BlendFunction for both BlendModes
	 * @param src The new source BlendFactor for both BlendModes
	 * @param dst The new destination BlendFactor for both BlendModes
	 * @return This Shader
	 */
	public T setBlendMode(BlendFunction eq, BlendFactor src, BlendFactor dst) {
		blendRgb.setBlendMode(eq, src, dst);
		blendAlpha.setBlendMode(eq, src, dst);
		return (T) this;
	}

	/**
	 * Return the BlendMode instance used by this OpenGlShader to describe
	 * blending for the RGB components. Any changes to this instance will be
	 * used by this OpenGlShader.
	 * 
	 * @return BlendMode used for rgb values
	 */
	public BlendMode getBlendModeRgb() {
		return blendRgb;
	}
	
	/**
	 * Return the BlendMode instance used by this OpenGlShader to describe
	 * blending for the alpha component. Any changes to this instance will be
	 * used by this OpenGlShader.
	 * 
	 * @return BlendMode used for alpha values
	 */
	public BlendMode getBlendModeAlpha() {
		return blendAlpha;
	}

	/**
	 * Copy color into the blend color used in this OpenGlShader when one of its
	 * BlendModes uses a BlendFactor of CONSTANT_COLOR, CONSTANT_ALPHA, or
	 * ONE_MINUS_CONSTANT_COLOR, or ONE_MINUS_CONSTANT_ALPHA.
	 * 
	 * @param color New blend constant color
	 * @return This Shader
	 */
	public T setBlendColor(Color4f color) {
		if (color == null)
			color = new Color4f();
		blendColor.set(color);
		return (T) this;
	}

	/**
	 * Return the blend color used when blending is enabled and one of the
	 * BlendFactors refers to the CONSTANT_x color/alpha.
	 * 
	 * @return The blend color
	 */
	public Color4f getBlendColor() {
		return blendColor;
	}

	/**
	 * <p>
	 * Set the DrawStyle to use for front faces and back faces of polygons
	 * rendered with this OpenGlShader. Front or back facing of a polygon is
	 * determined by the counter-clockwise winding of vertices.
	 * </p>
	 * <p>
	 * If front is null, SOLID is used. If back is null, NONE is used.
	 * </p>
	 * 
	 * @param front The DrawStyle to use for front facing polygons
	 * @param back The DrawStyle to use for back facing polygons
	 * @return This Shader
	 */
	public T setDrawStyle(DrawStyle front, DrawStyle back) {
		frontStyle = (front != null ? front : DrawStyle.SOLID);
		backStyle = (back != null ? back : DrawStyle.NONE);
		
		return (T) this;
	}

	/**
	 * Return the DrawStyle used for front-facing polygons.
	 * 
	 * @return The front DrawStyle
	 */
	public DrawStyle getDrawStyleFront() {
		return frontStyle;
	}

	/**
	 * Return the DrawStyle used for back-facing polygons.
	 * 
	 * @return The back DrawStyle
	 */
	public DrawStyle getDrawStyleBack() {
		return backStyle;
	}

	/**
	 * Set the Comparison to use when performing depth testing. Unlike other
	 * tests, depth testing will always be performed.
	 * 
	 * @param depthTest The new depthTest, null uses LEQUAL
	 * @return This Shader
	 */
	public T setDepthTest(Comparison depthTest) {
		if (depthTest == null)
			depthTest = Comparison.LEQUAL;
		this.depthTest = depthTest;
		
		return (T) this;
	}

	/**
	 * Return the depth test Comparison used for this OpenGlShader.
	 * 
	 * @return The depth test
	 */
	public Comparison getDepthTest() {
		return depthTest;
	}

	/**
	 * Set whether or not depth values will be written into the depth buffer
	 * after it's been determined that a pixel should be rendered into the
	 * buffer. If disabled, this does not prevent the pixel's color from being
	 * rendered.
	 * 
	 * @param enable True if depth writing is allowed
	 * @return This Shader
	 */
	public T setDepthWriteEnabled(boolean enable) {
		depthWriteMask = enable;
		return (T) this;
	}

	/**
	 * Return whether or not depth writing is enabled.
	 * 
	 * @return True if depth writing is allowed
	 */
	public boolean isDepthWriteEnabled() {
		return depthWriteMask;
	}

	/**
	 * <p>
	 * Set the bitwise mask that's applied to stencil values before they're
	 * written into the stencil buffer. When writing a value, only bits within
	 * the value that correspond to a 1 in the mask are written to the mask,
	 * ignoring any high bits that are above the precision of the stencil
	 * buffer. Thus a mask of 0 disables stencil buffer writes.
	 * </p>
	 * <p>
	 * Because the stencil test is performed (potentially) differently across
	 * front and back facing polygons, this mask only modifies values from
	 * front-facing polygons.
	 * </p>
	 * 
	 * @param mask The new mask for stencil values, for front facing polygons
	 * @return This Shader
	 */
	public T setStencilWriteMaskFront(int mask) {
		stencilMaskFront = mask;
		return (T) this;
	}

	/**
	 * Return the mask applied to incoming values written to the stencil buffer,
	 * from front-facing polygons.
	 * 
	 * @return The front stencil write mask
	 */
	public int getStencilWriteMaskFront() {
		return stencilMaskFront;
	}
	
	/**
	 * <p>
	 * Set the bitwise mask that's applied to stencil values before they're
	 * written into the stencil buffer. When writing a value, only bits within
	 * the value that correspond to a 1 in the mask are written to the mask,
	 * ignoring any high bits that are above the precision of the stencil
	 * buffer. Thus a mask of 0 disables stencil buffer writes.
	 * </p>
	 * <p>
	 * Because the stencil test is performed (potentially) differently across
	 * front and back facing polygons, this mask only modifies values from
	 * back-facing polygons.
	 * </p>
	 * 
	 * @param mask The new mask for stencil values, for back facing polygons
	 * @return This Shader
	 */
	public T setStencilWriteMaskBack(int mask) {
		stencilMaskBack = mask;
		return (T) this;
	}
	
	/**
	 * Return the mask applied to incoming values written to the stencil buffer,
	 * from back-facing polygons.
	 * 
	 * @return The back stencil write mask
	 */
	public int getStencilWriteMaskBack() {
		return stencilMaskBack;
	}

	/**
	 * Set both the front and back stencil masks to the given mask value.
	 * 
	 * @see #setStencilWriteMaskBack(int)
	 * @see #setStencilWriteMaskFront(int)
	 * @param mask The write mask for both front and back faces
	 * @return This Shader
	 */
	public T setStencilWriteMask(int mask) {
		stencilMaskFront = mask;
		stencilMaskBack = mask;
		return (T) this;
	}

	/**
	 * Convenience function to set the stencil test, reference value, and
	 * function mask for both the front and back stencil tests.
	 * 
	 * @param test The Comparison to use for front/back stencil tests
	 * @param ref The reference value for front/back
	 * @param mask The function mask for front/back
	 * @return This Shader
	 */
	public T setStencilTest(Comparison test, int ref, int mask) {
		stencilFront.setTest(test);
		stencilFront.setReference(ref);
		stencilFront.setFunctionMask(mask);
		
		stencilBack.setTest(test);
		stencilBack.setReference(ref);
		stencilBack.setFunctionMask(mask);
		
		return (T) this;
	}

	/**
	 * Convenience function to set the StencilUpdateOperations f both the front
	 * and back stencil tests.
	 * 
	 * @param stencilFail The operation used when the stencil test fails
	 * @param depthFail The operation used when the depth test fails
	 * @param depthPass The operation used when the depth pass fails
	 * @return This Shader
	 */
	public T setStencilUpdateOperations(StencilUpdateOperation stencilFail, StencilUpdateOperation depthFail, 
										StencilUpdateOperation depthPass) {
		stencilFront.setStencilFailOperation(stencilFail);
		stencilFront.setDepthFailOperation(depthFail);
		stencilFront.setDepthPassOperation(depthPass);
		
		stencilBack.setStencilFailOperation(stencilFail);
		stencilBack.setDepthFailOperation(depthFail);
		stencilBack.setDepthPassOperation(depthPass);
		
		return (T) this;
	}

	/**
	 * Return the StencilTest instance that holds the stencil test state for
	 * front-facing polygons.
	 * 
	 * @return The StencilTest controlling front-facing polygons
	 */
	public StencilTest getStencilTestFront() {
		return stencilFront;
	}
	
	/**
	 * Return the StencilTest instance that holds the stencil test state for
	 * back-facing polygons.
	 * 
	 * @return The StencilTest controlling back-facing polygons
	 */
	public StencilTest getStencilTestBack() {
		return stencilBack;
	}

	/**
	 * Return whether or not the stencil test operation is enabled. If this
	 * returns true, then the StencilTests used for front and back facing
	 * polygons will be enabled and it will perform updates to the stencil
	 * buffer, and discard pixels as per the test rules.
	 * 
	 * @return True if the stencil test is enabled
	 */
	public boolean isStencilTestEnabled() {
		return enableStencilTest;
	}

	/**
	 * Set whether or not the stencil test is enabled. If a RenderSurface has no
	 * stencil buffer, it is always as if the stencil test is disabled.
	 * 
	 * @param enable True if stencil tests should be used.
	 * @return This Shader
	 */
	public T setStencilTestEnabled(boolean enable) {
		enableStencilTest = enable;
		return (T) this;
	}

	/**
	 * Return whether or not the red component of a fragment is allowed to be
	 * written into the color buffer. If this is false, the green, blue and alpha
	 * components may still be written. When the channel is disabled, it does
	 * not write a default component value, but instead preserves the previous
	 * value contained in that component.
	 * 
	 * @return Whether or not the red channel can be written into
	 */
	public boolean isRedChannelEnabled() {
		return colorMaskRed;
	}

	/**
	 * Set whether or not a fragment's red component will be written into the
	 * color buffer.
	 * 
	 * @param enable True if red should be written
	 * @return This Shader
	 */
	public T setRedChannelEnabled(boolean enable) {
		this.colorMaskRed = enable;
		return (T) this;
	}

	/**
	 * Return whether or not the green component of a fragment is allowed to be
	 * written into the color buffer. If this is false, the red, blue, and alpha
	 * components may still be written. When the channel is disabled, it does
	 * not write a default component value, but instead preserves the previous
	 * value contained in that component.
	 * 
	 * @return Whether or not the green channel can be written into
	 */
	public boolean isGreenChannelEnabled() {
		return colorMaskGreen;
	}
	
	/**
	 * Set whether or not a fragment's green component will be written into the
	 * color buffer.
	 * 
	 * @param enable True if green should be written
	 * @return This Shader
	 */
	public T setGreenChannelEnabled(boolean enable) {
		this.colorMaskGreen = enable;
		return (T) this;
	}

	/**
	 * Return whether or not the blue component of a fragment is allowed to be
	 * written into the color buffer. If this is false, the red, green, and alpha
	 * components may still be written. When the channel is disabled, it does
	 * not write a default component value, but instead preserves the previous
	 * value contained in that component.
	 * 
	 * @return Whether or not the blue channel can be written into
	 */
	public boolean isBlueChannelEnabled() {
		return colorMaskBlue;
	}

	/**
	 * Set whether or not a fragment's blue component will be written into the
	 * color buffer.
	 * 
	 * @param enable True if blue should be written
	 * @return This Shader
	 */
	public T setBlueChannelEnabled(boolean enable) {
		this.colorMaskBlue = enable;
		return (T) this;
	}

	/**
	 * Return whether or not the alpha component of a fragment is allowed to be
	 * written into the color buffer. If this is false, the red, green and blue
	 * components may still be written. When the channel is disabled, it does
	 * not write a default component value, but instead preserves the previous
	 * value contained in that component.
	 * 
	 * @return Whether or not the alpha channel can be written into
	 */
	public boolean isAlphaChannelEnabled() {
		return colorMaskAlpha;
	}

	/**
	 * Set whether or not a fragment's alpha component will be written into the
	 * color buffer.
	 * 
	 * @param enable True if alpha should be written
	 * @return This Shader
	 */
	public T setAlphaChannelEnabled(boolean enable) {
		this.colorMaskAlpha = enable;
		return (T) this;
	}

	/**
	 * Convenience function to specify which color channels are enabled for
	 * writing, in one method call. If all values are false, then no color is
	 * written. If all values are true, then the entire color is written and the
	 * entire previous color is overwritten.
	 * 
	 * @param red Whether or not the red channel is enabled
	 * @param green Whether or not the green channel is enabled
	 * @param blue Whether or not the blue channel is enabled
	 * @param alpha Whether or not the alpha channel is enabled
	 * @return This Shader
	 */
	public T setColorChannelsEnabled(boolean red, boolean green, boolean blue, boolean alpha) {
		return setRedChannelEnabled(red).
			   setGreenChannelEnabled(green).
			   setBlueChannelEnabled(blue).
			   setAlphaChannelEnabled(alpha);
	}
}
