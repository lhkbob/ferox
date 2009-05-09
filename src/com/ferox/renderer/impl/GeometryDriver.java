package com.ferox.renderer.impl;

import com.ferox.resource.Geometry;

/**
 * <p>
 * A GeometryDriver extends the capabilities of the ResourceDriver to render
 * geometry. The methods provided here, unlike those in ResourceDriver, are
 * allowed to modify the state record of the current surface and are guaranteed
 * to be called with a current surface (and not just a current context).
 * </p>
 * <p>
 * Because GeometryDriver is a ResourceDriver, it is responsible for updating
 * and cleaning the Geometries that it would also render. It is recommended that
 * drivers store information in a Handle, but it is unnecessary to return a
 * valid handle id.
 * </p>
 * <p>
 * Drivers can assumed that a low-level context is made current on the calling
 * thread and that low-level graphics calls are allowed.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface GeometryDriver extends ResourceDriver {
	/**
	 * <p>
	 * Render the specified geometry onto the current surface. The geometry will
	 * not have a status of CLEANED or ERROR (it will have been updated first).
	 * The ResourceData, data, is geom's associated ResourceData instance.
	 * </p>
	 * <p>
	 * This method is allowed to leave the state record modified in an attempt
	 * to make later render calls faster. See reset() for when these changes
	 * must be reverted to normal.
	 * </p>
	 * 
	 * @see #reset()
	 * @param geom The Geometry that must be rendered
	 * @param data The geometry's associated ResourceData
	 * @return The number of polygon primitives that were rendered for geom
	 */
	public int render(Geometry geom, ResourceData data);

	/**
	 * <p>
	 * Reset any changes made to the current surface's stat record because of
	 * prior render() calls, so that other drivers may render geometry without
	 * state conflicts.
	 * </p>
	 * This method is responsible for resetting in
	 * <p>
	 * This method is called when the next rendered Geometry is not handled by
	 * this driver, and at the end of the rendering to the current surface
	 * (assuming that this GeometryDriver was the last used driver, of course).
	 * </p>
	 */
	public void reset();
}
