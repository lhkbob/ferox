package com.ferox.core.renderer;


/**
 * A ReshapeListener is used to listen to reshape events issued by the underlying context of any RenderManager
 * that this listener is added to.  These events are issued when the context size has been changed.
 * @author Michael Ludwig
 *
 */
public interface ReshapeListener {
	/**
	 * Called when the context size of the given manager is reshaped to newWidth and newHeight.
	 */
	public void onReshape(RenderManager manager, int newWidth, int newHeight, int oldWidth, int oldHeight);
}
