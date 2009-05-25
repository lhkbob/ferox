package com.ferox.effect;

/**
 * <p>
 * An enum describing the possible end effects allowed by most current
 * graphics hardware. This is highly based off of fixed-function rendering
 * pipelines in low-level graphics apis. Many related operations have been
 * gathered together into one effect type for simplicity. If ever new types
 * should become available, this enum may change.
 * </p>
 * <p>
 * Each EffectType has a boolean parameter controlling whether or not multiple
 * Effects of that EffectType can be used in the same EffectSet.
 * </p>
 * <p>
 * The concept of multiple effects may be confusing. For example, TEXTURE is
 * not a EffectType allowing multiple Effects, but low-level graphics libraries
 * let you use multi-texturing. This is because each texture has a specific
 * unit attached to it; the active textures are then a list and not a set.
 * Lights however are multiple effects because all lights applied to a
 * RenderAtom can be represented as a set.
 * </p>
 */
public enum EffectType {
	/**
	 * Controls aspects related to a pixel's alpha value and its visibility
	 */
	ALPHA,
	/**
	 * Controls final pixel color by blending it with previously rendered
	 * pixels.
	 */
	BLEND,
	/**
	 * Controls aspects of depth testing and the depth buffer
	 */
	DEPTH,
	/**
	 * Controls the application of programmable shaders.
	 */
	SHADER,
	/**
	 * Controls the parameters of the global lighting effects.
	 */
	GLOBAL_LIGHTING,
	/**
	 * Controls the parameters of individual lights.
	 */
	LIGHT(true),
	/**
	 * Controls the coloring and type of fog.
	 */
	FOG,
	/**
	 * Controls the material color of a polygon used when rendering.
	 */
	MATERIAL,
	/**
	 * Controls the texture environment used to modify the color of pixels.
	 */
	TEXTURE,
	/**
	 * Controls all aspects of the rendering style of points.
	 */
	POINT,
	/**
	 * Controls all aspects of the rendering style of lines.
	 */
	LINE,
	/**
	 * Controls the different facing aspects of rendered polygons, such as
	 * winding and draw mode.
	 */
	POLYGON,
	/**
	 * Controls stencil testing and writing to a stencil buffer.
	 */
	STENCIL,
	/**
	 * Controls the masking of color values written to the color buffer.
	 */
	COLOR_MASK;

	private boolean multiple;

	private EffectType() {
		this(false);
	}

	private EffectType(boolean mul) {
		multiple = mul;
	}

	/**
	 * Whether or not multiple Effects of this EffectType can be used in the same
	 * EffectSet
	 * 
	 * @return True if multiple instances using this EffectType do not conflict
	 *         with each other.
	 */
	public boolean getMultipleEffects() {
		return multiple;
	}
}