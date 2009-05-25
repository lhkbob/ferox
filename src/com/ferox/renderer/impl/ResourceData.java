package com.ferox.renderer.impl;

import com.ferox.resource.Resource.Status;

/**
 * <p>
 * Each Resource object that is used by an AbstractRenderer has an associated
 * ResourceData object. This class provides some internal bookkeeping for the
 * renderer and allows ResourceDrivers to store there own information associated
 * with a Resource instance.
 * </p>
 * <p>
 * Both Geometries and Resources have an associated ResourceData. If the
 * associated object implements Geometry, then isGeometry() returns true and the
 * resource driver used is also a GeometryDriver.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class ResourceData {
	/**
	 * Every ResourceData object should have a custom Handle object that is
	 * created/updated by the resource driver when its update() method called.
	 */
	public static interface Handle {
		/**
		 * <p>
		 * Return the integer handle that is usable for low-level graphics
		 * calls. This integer is only valid if it has a status of OK or DIRTY
		 * and it is not a Geometry. It is recommended that drivers relying on
		 * this should call getHandle() in AbstractRenderer, which checks these
		 * conditions.
		 * </p>
		 * <p>
		 * Geometries may still have a Handle object, because the drivers need
		 * some means of tracking status. However, the handle implementations
		 * must return -1 in this case.
		 * </p>
		 * 
		 * @return The low-level implementation dependent id referring to this
		 *         resource handle
		 */
		public int getId();
	}

	private Handle handle;
	private final boolean isGeometry;

	private Status status;
	private String statusMessage;

	final ResourceDriver driver;

	/**
	 * Construct a new ResourceData that will use the given resource driver.
	 * 
	 * @param driver The ResourceDriver that will be used for this data's
	 *            Resource for all future operations
	 * @throws NullPointerException if driver is null
	 */
	public ResourceData(ResourceDriver driver) {
		if (driver == null)
			throw new NullPointerException(
				"Constructor arguments cannot be null: " + driver);

		this.driver = driver;
		isGeometry = driver instanceof GeometryDriver;

		setHandle(null);
		setStatus(null);
		setStatusMessage("");
	}

	/**
	 * Return true if this resource is associated with a Geometry instead of a
	 * plain Resource. If this is true, then getHandle() will return the
	 * driver's assigned Handle, except the handle's id should not be used.
	 * 
	 * @return True if the resource is a Geometry
	 */
	public boolean isGeometry() {
		return isGeometry;
	}

	/**
	 * Return the driver's assigned Handle. This is the instance last passed
	 * into setHandle(), or null if the resource hasn't been used by the driver
	 * before (or since it's last cleaning).
	 * 
	 * @return The Resource's associated low-level handle
	 */
	public Handle getHandle() {
		return handle;
	}

	/**
	 * <p>
	 * Set the driver data to be used for the associated resource's driver.
	 * Handle can be implemented to provide driver specific information for
	 * managing the resource. Drivers must ensure that a Geometry handle returns
	 * -1 since there is no real handle to point to.
	 * </p>
	 * <p>
	 * Only the driver that actually updates and cleans-up the resource should
	 * call this method.
	 * </p>
	 * 
	 * @param handle The new Handle to associate with this ResourceData (and
	 *            resource)
	 */
	public void setHandle(Handle handle) {
		this.handle = handle;
	}

	/**
	 * Return the status set by the driver. The default value is OK. This value
	 * will never be CLEANED because that value cannot be passed to setStatus().
	 * 
	 * @return The resource's current status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Set the status to be returned by an AbstractRenderer's getStatus()
	 * methods if the status shouldn't be CLEANED. AbstractRenderer will manage
	 * the returning of a CLEANED status.
	 * 
	 * @param status The new Status to use, if null it uses OK
	 * @throws IllegalArgumentException if status is CLEANED
	 */
	public void setStatus(Status status) {
		if (status == Status.CLEANED)
			throw new IllegalArgumentException(
				"Drivers may not set the status of a ResourceData to CLEANED");
		if (status == null)
			status = Status.OK;
		this.status = status;
	}

	/**
	 * Return the status message to be used in conjunction with the driver's set
	 * status. The default value is the empty string and will never be null.
	 * 
	 * @return The current status message
	 */
	public String getStatusMessage() {
		return statusMessage;
	}

	/**
	 * Set the string message to be used together with the set status.
	 * 
	 * @param statusMessage The new status message, null is converted to the
	 *            empty string
	 */
	public void setStatusMessage(String statusMessage) {
		if (statusMessage == null)
			statusMessage = "";
		this.statusMessage = statusMessage;
	}
}
