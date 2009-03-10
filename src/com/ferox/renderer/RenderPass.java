package com.ferox.renderer;

import com.ferox.state.State.Role;

/** A RenderPass describes a view from which it is rendered
 * and provides a instance specific RenderQueue object that,
 * when flushed by a Renderer, will render the visible scene
 * based on the pass's view object.
 * 
 * Implementations should provide means to set and access
 * the visible entities to be rendered.
 * 
 * @author Michael Ludwig
 *
 */
public interface RenderPass {
	/** Get the unique RenderQueue for this RenderPass.  Renderers may assume this
	 * uniqueness, so any RenderPass that breaks this contract may have undefined 
	 * behavior. 
	 * 
	 * This method should not return null, or the renderer will throw an exception. */
	public RenderQueue getRenderQueue();
	
	/** A renderer will invoke this method when it is necessary to clear and fill
	 * the render pass's RenderQueue.  This will be during a call to flushRenderer(),
	 * but before the the pass's RenderQueue will be flushed.
	 * 
	 * Implementations should document how much preparation is actually done.
	 * For example, render passes that use SceneElements could update the scene element
	 * as well, or it could assume that the scene was updated by the application programmer.
	 * 
	 * preparePass() is responsible for clearing and filling the pass's RenderQueue.
	 * This method must not flush the RenderQueue object.
	 * 
	 * Return the View object to use for the rendering of the pass.  If null is returned,
	 * the pass will not be rendered by the Renderer.  If it is not null, the View
	 * must have had its world caches updated. */
	public View preparePass(Renderer renderer);
	
	/** Determine if the given state type is ignored when rendering. 
	 * 
	 * If this method returns true, then any appearances that are rendered that have
	 * a state of the given role will ignore that attached state and use the default
	 * state.  
	 * 
	 * This method is identical to isRoleMasked() in Renderer, except that if Renderer.isStateMasked()
	 * returns true, but RenderPass's doesn't, then the state type is still masked. */
	public boolean isRoleMasked(Role type);
}
