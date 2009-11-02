package com.ferox.renderer;

/**
 * <p>
 * A RenderPass is the mechanism with which content can be rendered into
 * a RenderSurface for a Framework.  A RenderPass's {@link #render(Renderer, RenderSurface)} 
 * method is invoked when appropriate by a Framework so that the Renderer's
 * render() method can be called.  It is the pass's responsibility to
 * specify Geometry's and Shader's to describe a meaningful scene.
 * </p>
 * <p>
 * Implementations should provide means to set and access the visible entities
 * to be rendered.  These modifiers should strive to be as thread-safe as possible.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface RenderPass {
	/**
	 * <p>
	 * Render the Geometry's and Shader's explicitly or implicitly described in
	 * this RenderPass.
	 * <p>
	 * Implementations can assume that this method is called appropriately by
	 * the Framework with a usable Renderer.
	 * </p>
	 * 
	 * @param renderer The Renderer that is actively rendering on the calling
	 *            Thread
	 * @param surface The RenderSurface that this RenderPass will be rendered
	 *            into
	 */
	public void render(Renderer renderer, RenderSurface surface);
}
