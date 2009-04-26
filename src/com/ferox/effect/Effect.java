package com.ferox.effect;

import com.ferox.renderer.RendererAware;

/**
 * Designates that a class that describes or effects how a RenderAtom is
 * rendered. This could be things such as material color, lighting, or
 * texturing. Those all effect the coloring of the rendered geometry. Other
 * options, such as depth test, stencil test, or alpha testing, affect what
 * pixels are actually rendered to with a geometry.
 * 
 * Renderers are not required to support all implementations of Effect, although
 * it is recommended that they support all default implementations in the
 * com.ferox.effect package.
 * 
 * Implementations should strive to not overlap responsibilities or roles in how
 * a geometry is modified. A Renderer may not be able to reliably support both
 * implementations. In other cases, it may be quite convenient, such as with
 * Textures and MultiTextures.
 * 
 * @author Michael Ludwig
 * 
 */
public interface Effect extends RendererAware {
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
