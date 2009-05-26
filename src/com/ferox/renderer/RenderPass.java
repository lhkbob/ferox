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
 * <p>
 * RenderPass are not thread safe. If for some reason multiple Frameworks are
 * used with the same RenderPass, they should not be rendered with at the same
 * time. If they are the timelines of preparePass() and render() may overlap and
 * inconsistencies could result.
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
	 * preparing is required just once.
	 * </p>
	 * <p>
	 * This method is responsible for invoking renderAtom(atom) as necessary on
	 * renderer. A convenient way of achieving this is to use a RenderQueue.
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
	 * @param view The View that was returned by the last call to preparePass()
	 *            by the Framework
	 */
	public void render(Renderer renderer, View view);

	/**
	 * <p>
	 * A Framework will invoke this method when it is necessary to prepare the
	 * RenderPass for rendering. This will be during the renderFrame() method,
	 * but it may be before the resource managers are invoked, and on a separate
	 * thread from the actual rendering.
	 * </p>
	 * <p>
	 * Implementations should document how much preparation is actually done.
	 * For example, render passes that use SceneElements could update the scene
	 * element as well, or it could assume that the scene was updated by the
	 * application programmer.
	 * </p>
	 * <p>
	 * If the RenderPass implementation relies on a RenderQueue for rendering
	 * atoms, this method is responsible for clearing and filling the
	 * RenderQueue; the queue should then be flushed in the render() method.
	 * </p>
	 * <p>
	 * Return the View object to use for the rendering of the pass. If null is
	 * returned, the pass will not be rendered by the Framework. If it is not
	 * null, the View must have had its world caches updated.
	 * </p>
	 * 
	 * @return The View that is to be used with rendering this pass
	 */
	public View preparePass();
}
