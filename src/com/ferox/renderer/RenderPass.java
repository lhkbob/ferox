package com.ferox.renderer;

/**
 * A RenderPass describes a view from which it is rendered and provides a
 * instance specific RenderQueue object that, when flushed by a Renderer, will
 * render the visible scene based on the pass's view object.
 * 
 * Implementations should provide means to set and access the visible entities
 * to be rendered.
 * 
 * RenderPass are not thread safe. If for some reason multiple renderers are
 * used, they should not be rendered with at the same time (otherwise its
 * RenderQueue could be inconsistent).
 * 
 * @author Michael Ludwig
 * 
 */
public interface RenderPass {
	/**
	 * Get the unique RenderQueue for this RenderPass. Renderers may assume this
	 * uniqueness, so any RenderPass that breaks this contract may have
	 * undefined behavior.
	 * 
	 * This method should not return null, or the renderer will throw an
	 * exception.
	 * 
	 * @return The unique RenderQueue associated with this pass
	 */
	public RenderQueue getRenderQueue();

	/**
	 * A renderer will invoke this method when it is necessary to clear and fill
	 * the render pass's RenderQueue. This will be during a call to
	 * flushRenderer(), but before the the pass's RenderQueue will be flushed.
	 * 
	 * Implementations should document how much preparation is actually done.
	 * For example, render passes that use SceneElements could update the scene
	 * element as well, or it could assume that the scene was updated by the
	 * application programmer.
	 * 
	 * preparePass() is responsible for clearing and filling the pass's
	 * RenderQueue. This method must not flush the RenderQueue object.
	 * 
	 * Return the View object to use for the rendering of the pass. If null is
	 * returned, the pass will not be rendered by the Renderer. If it is not
	 * null, the View must have had its world caches updated.
	 * 
	 * @param renderer The Renderer that will render this pass during its
	 *            flushRenderer() method
	 * @return The View that is to be used with rendering this pass
	 */
	public View preparePass(Renderer renderer);
}
