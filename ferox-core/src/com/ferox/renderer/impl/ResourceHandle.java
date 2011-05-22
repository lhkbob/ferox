package com.ferox.renderer.impl;

import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

/**
 * <p>
 * ResourceHandle represents the top-level class containing information about
 * Resources that have been updated and are stored on the graphics card. Each
 * ResourceHandle has a Status and a status message. The Status and status
 * message are what is reported back whenever a Resource's status or message is
 * queried.
 * </p>
 * <p>
 * It is assumed that ResourceManagers and Renderers properly lock the owning
 * Resource when interacting with the handle. ResourceHandle does not use any
 * internal synchronization because it is assumed that it is always within an
 * outer lock.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class ResourceHandle {
    private final Resource resource;
    
    private Status status;
    private String statusMessage;

    /**
     * Create a new ResourceHandle. The initial Status will be DISPOSED and the
     * message is the empty string.
     * 
     * @param resource The Resource associated with this handle
     */
    public ResourceHandle(Resource resource) {
        if (resource == null)
            throw new NullPointerException("Resource cannot be null");
        this.resource = resource;
        setStatus(Status.DISPOSED);
        setStatusMessage("");
    }
    
    /**
     * @return The Resource associated with this ResourceHandle
     */
    public Resource getResource() {
        return resource;
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
