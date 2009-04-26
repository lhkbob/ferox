package com.ferox.effect;

import com.ferox.renderer.Renderer;

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
public interface Effect {
	/**
	 * Get the renderer specific data that has been assigned to this Effect.
	 * This object should not be modified unless it's by the Renderer that
	 * created it.
	 * 
	 * Undefined behavior occurs if it's changed.
	 * 
	 * @param renderer Renderer to fetch data for, will not be null
	 * @return The previously assigned data for the renderer, or null
	 */
	public Object getRenderData(Renderer renderer);

	/**
	 * Assign the renderer specific data for this object. This should not be
	 * called directly, it is to be used by renderers to attach implementation
	 * specific information needed for successful operation.
	 * 
	 * Undefined behavior occurs if this is set by something other than the
	 * Renderer.
	 * 
	 * @param renderer Renderer to assign data to
	 * @param data Object to return from getRenderData
	 */
	public void setRenderData(Renderer renderer, Object data);

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
