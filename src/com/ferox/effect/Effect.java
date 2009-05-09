package com.ferox.effect;

import com.ferox.effect.EffectType.Type;

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
 * with the EffectType annotation. If they're not annotated, they can't be used
 * in an EffectSet.
 * 
 * @see EffectType </p>
 * @author Michael Ludwig
 */
public interface Effect {
	/**
	 * Return the Type of this Effect, this must match the Type specified in the
	 * Effect's attached EffectType annotation
	 * 
	 * @return The Type of this Effect
	 */
	public Type getType();

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
