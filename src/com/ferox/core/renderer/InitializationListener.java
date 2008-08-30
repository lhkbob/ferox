package com.ferox.core.renderer;


/**
 * When added to a RenderManager instance, InitializationListeners are called once each time the underlying
 * RenderContext must re-initialize itself.  They can be useful to perform texture loads and creation, especially
 * if the texture data isn't kept in memory.  A listener added to a RenderManager which has already been initialized 
 * will be called once at the next frame, right after the before frame tasks are executed.
 * @author Michael Ludwig
 *
 */
public interface InitializationListener {
	/**
	 * Called once by a RenderManager instance once per context life cycle.  Generally, if a canvas or component is
	 * removed from screen, that will start another context life cycle, and this method will be called again.
	 */
	public void onInit(RenderManager manager);
}
