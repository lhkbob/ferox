package com.ferox.renderer;

/**
 * <p>
 * A RenderPass describes a view from which it is rendered and provides an
 * abstract method for rendering content into surfaces for a Framework, will
 * render the visible scene based on the pass's view object.
 * </p>
 * <p>
 * Implementations should provide means to set and access the visible entities
 * to be rendered.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface RenderPass {
	/**
	 * <p>
	 * Render the prepared pass. The preparation and rendering is split into
	 * separate phases because a RenderPass may be used in multiple
	 * RenderSurfaces, requiring that it is rendered more than once, but
	 * preparing is required just once. This method is responsible for invoking
	 * renderAtom(atom) as necessary on renderer.
	 * </p>
	 * <p>
	 * Implementations can assume that this method is called appropriately and
	 * after a method call to preparePass(renderer). Also, renderer and view can
	 * be assumed to be non-null. The specified view will be applied, so that
	 * atoms rendered via renderer.renderAtom() will be correcly rendered from
	 * that view point and projection.
	 * </p>
	 * 
	 * @see #preparePass()
	 * @param renderer The Renderer that is actively rendering on the calling
	 *            Thread
	 */
	public void render(Renderer renderer);

	public View getView();
}
