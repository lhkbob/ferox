package com.ferox.renderer.impl;

import com.ferox.resource.Geometry;

/** A GeometryDriver extends the capabilities of
 * the ResourceDriver to render geometry.  The methods provided
 * here, unlike those in ResourceDriver, are allowed to modify
 * the state record of the current surface and are guaranteed to
 * be called with a current surface (and not just a current context).
 * 
 * GeometryDrivers are still responsible for updating and cleaning
 * up geometries.  Also, geometry drivers are still expected to make
 * use of the resource data's set/getHandles.  In the case of a geometry,
 * they can store information necessary for the driver, but it is unnecessary
 * to return a valid handle id.
 * 
 * Drivers can assumed that a low-level context is made current on the calling
 * thread and that low-level graphics calls are allowed.  Implementations must
 * provide a means by which to get at this information.
 * 
 * @author Michael Ludwig
 *
 */
public interface GeometryDriver extends ResourceDriver {
	/** Render the specified geometry onto the current surface. It can be assumed
	 * that geometry and are non-null and paired.  The geometry will not have a
	 * status of CLEANED or ERROR (it will have been updated first). The data 
	 * object will have isGeometry() return true.
	 * 
	 * This method is allowed to leave the state record modified in an attempt
	 * to make later render calls faster. See reset() for when these changes
	 * must be reverted to normal.
	 * 
	 * Return the number of polygon primitives that were rendered for the geometry. */
	public int render(Geometry geom, ResourceData data);
	
	/** Notify the driver that the next geometry to be processed is being
	 * handled by a different driver.  This method is responsible for resetting
	 * in changes to the surface's record because of render() calls, so that other
	 * drivers may render things without conflict.  This method is also called
	 * at the end of every surface rendering. */
	public void reset();
}
