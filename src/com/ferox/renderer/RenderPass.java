package com.ferox.renderer;

import com.ferox.renderer.RequiresState.RenderState;

/**
 * <p>
 * A RenderPass describes a view from which it is rendered and provides a
 * instance specific RenderQueue object that, when flushed by a Renderer, will
 * render the visible scene based on the pass's view object.
 * </p>
 * <p>
 * Implementations should provide means to set and access the visible entities
 * to be rendered.
 * </p>
 * <p>
 * RenderPass are not thread safe. If for some reason multiple renderers are
 * used, they should not be rendered with at the same time (otherwise its
 * RenderQueue could be inconsistent).
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
	 * @see #preparePass(Renderer)
	 * @param renderer The Renderer that is actively rendering on the calling
	 *            Thread
	 * @param view The View that was returned by the last call to
	 *            preparePass(renderer) for the given renderer
	 */
	@RequiresState(RenderState.RENDERING)
	public void render(Renderer renderer, View view);

	/**
	 * <p>
	 * A renderer will invoke this method when it is necessary to clear and fill
	 * the render pass's RenderQueue. This will be during a call to
	 * flushRenderer(), but before the the pass's RenderQueue will be flushed.
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
	 * RenderQueue; it should not flush the queue.
	 * </p>
	 * <p>
	 * Return the View object to use for the rendering of the pass. If null is
	 * returned, the pass will not be rendered by the Renderer. If it is not
	 * null, the View must have had its world caches updated.
	 * </p>
	 * 
	 * @param renderer The Renderer that will render this pass during its
	 *            flushRenderer() method
	 * @return The View that is to be used with rendering this pass
	 */
	public View preparePass(Renderer renderer);
}
