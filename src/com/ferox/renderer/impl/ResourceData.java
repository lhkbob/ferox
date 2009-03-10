package com.ferox.renderer.impl;

import com.ferox.resource.Resource.Status;

/** Each Resource object that is used by an AbstractRenderer
 * has an associated ResourceData object.  This class provides
 * some internal bookkeeping for the renderer and allows Resource
 * Drivers to store there own information associated with a 
 * Resource instance.
 * 
 * Both Geometries and Resources have an associated ResourceData.
 * If the associated object implements Geometry, then isGeometry()
 * returns true and the resource driver used is also a GeometryDriver.
 * 
 * @author Michael Ludwig
 *
 */
public final class ResourceData {
	/** Every ResourceData object will have a custom Handle object that
	 * is created/updated by the resource driver has its update() method called. */
	public static interface Handle {
		/** Return the integer handle that is usable for low-level graphics calls.
		 * This integer is only valid if it has a status of OK or DIRTY and it is
		 * not a Geometry.  It is recommended that drivers relying on this should
		 * call getHandle() in AbstractRenderer, which checks these conditions. 
		 * 
		 * Geometries will still have a handle object, because the drivers need some
		 * means of tracking status.  However, the handle implementations must return
		 * -1 in this case. */
		public int getId();
	}
	
	private Handle handle;
	private final boolean isGeometry;
	
	private Status status;
	private String statusMessage;
	
	// used directly by AbstractRenderer to manage resources
	final AbstractRenderer renderer;
	final ResourceDriver driver;
	
	/** Construct a new ResourceData for the given AbstractRenderer that will
	 * use the specified driver. */
	public ResourceData(AbstractRenderer renderer, ResourceDriver driver) throws NullPointerException {
		if (renderer == null || driver == null)
			throw new NullPointerException("Constructor arguments cannot be null: " + renderer + " " + driver);
		
		this.renderer = renderer;
		this.driver = driver;
		this.isGeometry = this.driver instanceof GeometryDriver;

		this.setHandle(null);
		this.setStatus(null);
		this.setStatusMessage("");
	}
	
	/** Return true if this resource is associated with a Geometry instead of a plain
	 * Resource.  If this is true, then getHandle() will return the driver set handle,
	 * except that the handle's id will be -1. */
	public boolean isGeometry() {
		return this.isGeometry;
	}
	
	/** Return the driver set data.  This is the instance last passed into setHandle(),
	 * or null if the resource hasn't been used by the driver before (or since it's last cleaning). */
	public Handle getHandle() {
		return this.handle;
	}

	/** Set the driver data to be used for the associated resource's driver.  Handle can be implemented
	 * to provide driver specific information for managing the resource.  Drivers must ensure that
	 * a Geometry handle returns -1 since there is no real handle to point to.  
	 * 
	 * Only the driver that actually updates and cleansup the resource should call this method. */
	public void setHandle(Handle handle) {
		this.handle = handle;
	}

	/** Return the status set by the driver.  The default value is OK.  This
	 * value will never be CLEANED because that value cannot be passed to setStatus(). */
	public Status getStatus() {
		return this.status;
	}

	/** Set the status to be returned by an AbstractRenderer's getStatus() methods
	 * if the status shouldn't be CLEANED.  AbstractRenderer will manage the returning
	 * of a CLEANED status.
	 * 
	 * Throws an exception if status is set to CLEANED.  If the status
	 * is null, it defaults to OK. */
	public void setStatus(Status status) throws IllegalArgumentException {
		if (status == Status.CLEANED)
			throw new IllegalArgumentException("Drivers may not set the status of a ResourceData to CLEANED");
		if (status == null)
			status = Status.OK;
		this.status = status;
	}

	/** Return the status message to be used in conjunction with the
	 * driver's set status.  The default value is the empty string and
	 * will never be null. */
	public String getStatusMessage() {
		return this.statusMessage;
	}

	/** Set the string message to be used together with the set status.
	 * Throws an exception if the message is null. */
	public void setStatusMessage(String statusMessage) throws NullPointerException {
		if (statusMessage == null)
			throw new NullPointerException("A null status message is a reserved value for AbstractRenderer");
		this.statusMessage = statusMessage;
	}
}
