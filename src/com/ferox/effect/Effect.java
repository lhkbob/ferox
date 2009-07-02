package com.ferox.effect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Designates that a class that describes or effects how a RenderAtom is
 * rendered. This could be things such as material color, lighting, or
 * texturing. Those all effect the coloring of the rendered geometry. Other
 * options, such as depth test, stencil test, or alpha testing, affect what
 * pixels are actually rendered to with a geometry.
 * </p>
 * <p>
 * Renderers are not required to support all implementations of Effect, although
 * it is recommended that they support all default implementations in the
 * com.ferox.effect package.
 * </p>
 * <p>
 * To prevent Effects of overlapping responsibilities being used at the same
 * time while rendering, all Effect implementations must annotate themselves
 * with the Type annotation. If they're not annotated, they can't be used in an
 * EffectSet.
 * 
 * @see Type </p>
 * @author Michael Ludwig
 */
public interface Effect {
	/**
	 * Return the EffectType of this Effect, this must match the EffectType
	 * specified in the Effect's attached Type annotation
	 * 
	 * @return The EffectType of this Effect
	 */
	public EffectType getType();

	/**
	 * <p>
	 * The Type annotation specifies what an Effect modifies when its applied to
	 * a RenderAtom. It also defines the EffectType enum, which specifies the
	 * different abstract effects available for a Framework. Although Effect
	 * implementations are not final, it makes sense to declare the available
	 * types of effects.
	 * </p>
	 * <p>
	 * There is no other, reasonable solution to avoid conflicts between
	 * effects. For example, if there is an implementation that describes the
	 * color of the RenderAtom, it's clear that it modifies the material color.
	 * It doesn't make sense to allow an atom to be affected by that class and a
	 * Material. By having both classes annotated by @Type(MATERIAL), it is
	 * easily understand that they achieve the same end result and should not be
	 * used together.
	 * </p>
	 * <p>
	 * All Effect implementations must have an Type annotation attached
	 * somewhere in its class hierarchy.
	 * </p>
	 * 
	 * @author Michael Ludwig
	 */
	@Inherited
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Type {
		/**
		 * @return The EffectType that this Effect represents
		 */
		EffectType value();
	}

	/**
	 * A common enum to describe the quality state effects when rendering.
	 * DONT_CARE allows implementation to choose.
	 */
	public static enum Quality {
		FAST, BEST, DONT_CARE
	}

	/**
	 * A common enum used by some states to describe different pixel comparison
	 * tests, such as alpha or depth testing.
	 */
	public static enum PixelTest {
		EQUAL, GREATER, LESS, GEQUAL, LEQUAL, NOT_EQUAL, NEVER, ALWAYS
	}
}
