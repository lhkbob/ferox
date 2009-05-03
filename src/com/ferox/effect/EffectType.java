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
 * they achieve the same effect and shouldn't be used together.
 * </p>
 * <p>
 * All Effect implementations must have an EffectType annotation somewhere in
 * its class hierarchy.
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
	 * @return All Types that an Effect implementation represents.
	 */
	Type[] value();
	
	/**
	 * <p>
	 * An enum describing all low-level modifications to a RenderAtom. Highly
	 * related parameters (such as diffuse vs. specular colors) have been merged
	 * into one Type for clarity.
	 * </p>
	 * <p>
	 * The Type values are intended to be all-inclusive of the types of
	 * modifications allowed by low-level graphics system. If ever new options
	 * become available, or something in existence isn't described by a Type,
	 * then this might change.
	 * </p>
	 * <p>
	 * Each Type has a boolean parameter controlling whether or not multiple
	 * Effects of that Type can be used in the same EffectSet. When declaring
	 * the Types used by an Effect, you should not mix single and multiple
	 * Types, as the Effect will then only be used as if it were a single Type.
	 * </p>
	 * <p>
	 * The concept of multiple effects may be confusing. For example,
	 * TEXTURE_ENV and TEXTURE_COORD_GEN are not multiple effects, but low-level
	 * graphics libraries let you use multi-texturing. This is because each
	 * texture also has a specific unit attached to it; the active textures are
	 * then a list and not a set. Lights however are multiple effects because
	 * all lights applied to a RenderAtom can be represented as a set.
	 * </p>
	 */
	public static enum Type {
		/** Controls pixel visibility based on alpha values. */
		ALPHA_TEST,
		/**
		 * Controls final pixel color by blending it with previously rendered
		 * pixels.
		 */
		BLENDING,
		/** Controls pixel visibility based on the depth of the pixel. */
		DEPTH_TEST,
		/** Controls the writing of depth values into a RenderSurface. */
		DEPTH_WRITE,
		/** Controls the application of programmable shaders. */
		SHADER,
		/** Controls the parameters of the global lighting effects. */
		GLOBAL_LIGHTING,
		/** Controls the parameters of individual lights. */
		LIGHTS(true),
		/** Controls the coloring and type of fog. */
		FOG,
		/** Controls the material color of a polygon used when rendering. */
		MATERIAL,
		/** Controls the texture environment used to modify the color of pixels. */
		TEXTURE_ENV,
		/**
		 * Controls the automatic generation of texture coordinates for
		 * RenderAtoms.
		 */
		TEXTURE_COORD_GEN,
		/** Controls the sizing of rendered points. */
		POINT_SIZE,
		/**
		 * Controls how points are rendered, such as smoothing or point sprites.
		 */
		POINT_RENDER,
		/** Controls the size of rendered lines. */
		LINE_SIZE,
		/** Controls how lines are rendered, such as smoothing or stippling. */
		LINE_RENDER,
		/**
		 * Controls the different facing aspects of rendered polygons, such as
		 * winding and draw mode.
		 */
		POLYGON_FACING,
		/**
		 * Controls how polygons are rendered, such as smoothing, stippling and
		 * depth offsetting.
		 */
		POLYGON_RENDER,
		/** Controls the pixel visibility based on the stencil buffer. */
		STENCIL_TEST,
		/** Controls the writing and updates to the stencil buffer. */
		STENCIL_WRITE,
		/** Controls the writing of color values. */
		COLOR_WRITE;

		private boolean multiple;

		private Type() {
			this(false);
		}

		private Type(boolean mul) {
			this.multiple = mul;
		}

		/**
		 * Whether or not multiple Effects of this Type can be used in the same
		 * EffectSet
		 * 
		 * @return True if multiple instances using this Type do not conflict
		 *         with each other.
		 */
		public boolean getMultipleEffects() {
			return this.multiple;
		}
	}
}
