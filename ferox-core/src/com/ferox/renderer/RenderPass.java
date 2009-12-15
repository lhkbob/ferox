package com.ferox.renderer;

/**
 * <p>
 * A RenderPass is the mechanism with which content can be rendered into a
 * RenderSurface for a Framework. A RenderPass's
 * {@link #render(Renderer, RenderSurface)} method is invoked when appropriate
 * by a Framework so that the Renderer's render() method can be called. It is
 * the pass's responsibility to specify Geometry's to describe a meaningful
 * scene.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface RenderPass {
	/**
	 * <p>
	 * Render the graphical content that's described by this RenderPass. The
	 * RenderPass is responsible for modifying the Renderer state as needed and
	 * then invoking {@link Renderer#render(com.ferox.resource.Geometry)} as
	 * needed. Each time this pass is rendered, the Renderer has its state
	 * restored to its default.
	 * </p>
	 * <p>
	 * This should only be called by the Framework when the Renderer is allowed
	 * to be used. The Renderer should not be stored outside of this method
	 * implementation, because the Renderer is likely a light-weight object tied
	 * to a specific Thread or RenderSurface used by a Framework.
	 * </p>
	 * 
	 * @param renderer The Renderer that is actively rendering on the calling
	 *            Thread
	 * @param surface The RenderSurface that this RenderPass will be rendered
	 *            into
	 */
	public void render(Renderer renderer, RenderSurface surface);
}
