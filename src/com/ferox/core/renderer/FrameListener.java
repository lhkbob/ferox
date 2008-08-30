package com.ferox.core.renderer;


/**
 * When added to a RenderManager instance, a FrameListener will have its startFrame() method called at the
 * start of each frame, and its endFrame() method called at the end of each frame.  You can use to modify
 * scene and state elements before they are updated, and if you're an advanced user to interact with the 
 * RenderContext (or possibly lower).  If manipulating openGL state directly, unexpected results could occur
 * if those changes conflict with programmed state management.
 * @author Michael Ludwig
 *
 */
public interface FrameListener {
	/**
	 * Called when the given manager is ending a frame. Called after all rendering, but before any end frame
	 * tasks are executed.
	 */
	public void endFrame(RenderManager manager);
	
	/**
	 * Called when the given manager is starting a frame. Called before any State or SpatialTree's are updated
	 * and before any passes are rendered. Called after any InitializationListeners just added and after any
	 * before frame tasks.
	 */
	public void startFrame(RenderManager renderManager);
}
