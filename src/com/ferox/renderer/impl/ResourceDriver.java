package com.ferox.renderer.impl;

import com.ferox.resource.Resource;

/** ResourceDrivers provide low-level implementations of the
 * two primary operations that can be performed on resources:
 * updating and cleaning.
 * 
 * A Resource can only have three existing states from the AbstractRenderer's
 * perspective: destroyed, ready-to-go, or error'ed.  There is no need
 * for resource drivers to worry about pending operations or multiple contexts.
 * It should be assumed that the AbstractRenderers using the drivers
 * are able to share all data across multiple contexts (e.g. a texture handle
 * created on one context can be used on another surface of the same renderer).
 * 
 * Also, when a resource is clean-ed up, it should be assumed that the
 * cleaning cleans up everything for all surfaces.  It is the SurfaceFactory's
 * responsibility to make sure that these assumptions are met when it creates
 * new surfaces.
 * 
 * Drivers can assumed that a low-level context is made current on the calling
 * thread and that low-level graphics calls are allowed.  Implementations must
 * provide a means by which to get at this information.
 * 
 * @author Michael Ludwig
 *
 */
public interface ResourceDriver {
	/** Perform the low-level operations necessary to create a representation of the 
	 * resource on the graphics card.  If fullUpdate is true, then the resource's dirty
	 * descriptor must be ignored (and treated as it were null).  Resource drivers must
	 * respect the assumptions pertaining to null BufferData objects.
	 * 
	 * It can be assumed that resource and data are not null, and that data is the object
	 * returned by resource's getResourceData() method.  Use data.isGeometry() to determine
	 * if the resource is a geometry.  The given resource will have a class associated with
	 * the driver, as determined by the driver factory that provided it.
	 * 
	 * The driver is responsible for updating or creating the data's handle, and for
	 * setting the correct status code of OK, DIRTY or ERROR, as well as a corresponding
	 * status message. 
	 * 
	 * After update() returns, any state record of the current context should be left 
	 * unmodified. */
	public void update(Resource resource, ResourceData data, boolean fullUpdate);
	
	/** Perform the low-level operations to cleanup anything that was constructed
	 * by the paired call to update() earlier.  
	 * 
	 * In addition to the assumptions given in update(), it can be assumed that the
	 * resource has not be cleaned up before, and that it has a status of OK, 
	 * DIRTY or ERROR.
	 * 
	 * Like update(), the state record must be unmodified after a call to this method. */
	public void cleanUp(Resource resource, ResourceData data);
}
