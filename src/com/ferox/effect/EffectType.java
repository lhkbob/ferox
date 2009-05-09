package com.ferox.effect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * The EffectType annotation specifies what an Effect modifies when its applied
 * to a RenderAtom. It also defines the Type enum, which specifies the different
 * abstract effects available for a Renderer. Although Effect implementations
 * are not final, it makes sense to declare the available types of effects.
 * </p>
 * <p>
 * There is no other, reasonable solution to avoid conflicts between effects.
 * For example, if there is an implementation that describes the color of the
 * RenderAtom, it's clear that it modifies the material color. It doesn't make
 * sense to allow an atom to be affected by that class and a Material. By having
 * both classes annotated by @EffectType(MATERIAL), it is easily understand that
 * they achieve the same end result and should not be used together.
 * </p>
 * <p>
 * All Effect implementations must have an EffectType annotation attached
 * somewhere in its class hierarchy.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EffectType {
	/**
	 * @return The Type that this Effect represents
	 */
	Type value();

	/**
	 * <p>
	 * An enum describing the possible end effects allowed by most current
	 * graphics hardware. This is highly based off of fixed-function rendering
	 * pipelines in low-level graphics apis. Many related operations have been
	 * gathered together into one effect type for simplicity. If ever new types
	 * should become available, this enum may change.
	 * </p>
	 * <p>
	 * Each Type has a boolean parameter controlling whether or not multiple
	 * Effects of that Type can be used in the same EffectSet.
	 * </p>
	 * <p>
	 * The concept of multiple effects may be confusing. For example, TEXTURE is
	 * not a Type allowing multiple Effects, but low-level graphics libraries
	 * let you use multi-texturing. This is because each texture has a specific
	 * unit attached to it; the active textures are then a list and not a set.
	 * Lights however are multiple effects because all lights applied to a
	 * RenderAtom can be represented as a set.
	 * </p>
	 */
	public static enum Type {
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

		private Type() {
			this(false);
		}

		private Type(boolean mul) {
			multiple = mul;
		}

		/**
		 * Whether or not multiple Effects of this Type can be used in the same
		 * EffectSet
		 * 
		 * @return True if multiple instances using this Type do not conflict
		 *         with each other.
		 */
		public boolean getMultipleEffects() {
			return multiple;
		}
	}
}
