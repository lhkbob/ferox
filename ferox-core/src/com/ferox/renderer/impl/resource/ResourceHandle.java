package com.ferox.renderer.impl.resource;

import com.ferox.resource.Resource.Status;

/**
 * <p>
 * ResourceHandle represents the top-level class containing information about
 * Resources that have been updated and are stored on the graphics card. Each
 * ResourceHandle has an associated id, Status and a status message. The Status
 * and status message are what is reported back whenever a Resource's status or
 * message is queried. A ResourceHandle's id is the low-level id associated with
 * the OpenGL object stored on the graphics card (e.g. a texture id, or program
 * id).
 * </p>
 * <p>
 * It is assumed that ResourceManagers and RenderManagers properly interact with
 * ResourceHandles using the provided lock mechanism. ResourceHandle does not
 * use any internal synchronization because it is assumed that the manager's
 * must synchronize over a larger block of code anyway.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class ResourceHandle {
	private final int id;
	
	private Status status;
	private String statusMessage;

	/**
	 * Create a new ResourceHandle that will use the given id. If the Resource
	 * that holds onto this handle has no id, then use a value of -1. The
	 * initial Status will be DISPOSED and the message is the empty string.
	 * 
	 * @param id The ResourceHandle's id
	 */
	public ResourceHandle(int id) {
		this.id = id;
		setStatus(Status.DISPOSED);
		setStatusMessage("");
	}
	
	/**
	 * @return This ResourceHandle's id
	 */
	public final int getId() {
		return id;
	}

	/**
	 * @return The Status of this ResourceHandle, and thus it's associated
	 *         Resource
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * @return The the status message of this ResourceHandle, and thus it's
	 *         associated Resource
	 */
	public String getStatusMessage() {
		return statusMessage;
	}

	/**
	 * Set the Status of this ResourceHandle. This should only be called by
	 * implementations of ResourceManager when it is appropriate and meaningful.
	 * Because null has a special meaning for Status, if status is null then
	 * DISPOSED is used instead.
	 * 
	 * @param status The new Status of this ResourceHandle
	 */
	public void setStatus(Status status) {
		this.status = (status == null ? Status.DISPOSED : status);
	}

	/**
	 * Set the status message for this ResourceHandle. This should only be
	 * called by implementations of ResourceManager when it is appropriate and
	 * meaningful. Because null has a special meaning for status message, is
	 * message is null then the empty string is used instead.
	 * 
	 * @param message The new status message for this ResourceHandle
	 */
	public void setStatusMessage(String message) {
		statusMessage = (message == null ? "" : message);
	}
}
