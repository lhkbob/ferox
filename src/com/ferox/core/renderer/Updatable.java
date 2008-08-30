package com.ferox.core.renderer;

/**
 * A simple interface for anything that should/can be updated per frame.  Both SpatialTree and StateTree 
 * implement this, so you can add them to a RenderManager so it will automatically update them when necessary.
 * @author Michael Ludwig
 *
 */
public interface Updatable {
	/**
	 * If added to a RenderManager, update() will be called once per frame, after any frame listeners, but
	 * before the passes have been rendered.
	 */
	public void update();
}
