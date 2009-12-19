package com.ferox.renderer.impl;

import com.ferox.renderer.RenderSurface;

/**
 * Action represents some performable action that needs to be invoked on a
 * OpenGL capable Thread. It is expected that the Framework implementation
 * properly prepares OpenGL contexts so that when the Action's
 * {@link #perform(Context, Action)} method is invoked, it's RenderSurface is
 * current on the Thread.
 * 
 * @author Michael Ludwig
 */
public abstract class Action {
	private final RenderSurface surface;

	/**
	 * Create a new Action that's associated with the given RenderSurface. If
	 * the surface is null, then this Action can be invoked on any valid OpenGL
	 * context.
	 * 
	 * @param surface The RenderSurface that must be current when the Action is
	 *            peformed
	 */
	public Action(RenderSurface surface) {
		this.surface = surface;
	}

	/**
	 * Return the RenderSurface that this Action is attached to, or null if no
	 * specific RenderSurface is required.
	 * 
	 * @return This Action's RenderSurface
	 */
	public RenderSurface getRenderSurface() {
		return surface;
	}

	/**
	 * Prepare this Action for being invoked. This is called by the Framework
	 * before it invokes any actions, and provides a mechanism to reject the
	 * Action if it's no longer valid. The current implementation returns true
	 * as long as the associated RenderSurface has not been destroyed.
	 * 
	 * @return True if this Action should still be performed
	 */
	public boolean prepare() {
		return surface == null || !surface.isDestroyed();
	}

	/**
	 * Invoke this Action so it performs low-level graphics work (generally).
	 * The Context specified should be the Context that's current on the calling
	 * Thread, and next is the Action that will be performed next (or null if
	 * there are no more Actions).
	 * 
	 * @param context The current Context
	 * @param next The next Action to be invoked
	 */
	public abstract void perform(Context context, Action next);
}
