package com.ferox.renderer.impl;

import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.resource.Resource;

/**
 * <p>
 * A ResourceDriver manages the lifecycle of a given Resource type. It is used
 * by {@link ResourceManager} to perform the actual low-level graphics
 * operations when requested by the HardwareAccessLayer or when a Resource must
 * be updated becaue of an ON_DEMAND update policy. Implementations may assume
 * that a low-level context is current on the running thread.
 * </p>
 * <p>
 * Implementations do not need to acquire the lock on any of the input resources
 * because the {@link ResourceManager} will have already locked the resource.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface ResourceDriver<R extends Resource> {
    /**
     * Perform an update on the given resource. The ResourceHandle is the
     * ResourceHandle instance previously returned by the last call to update()
     * with this resource. If the resource has not been updated, this handle
     * will be null. This is responsible for performing any error checks that
     * would make the Resource unusable. This is responsible for performing the
     * operations required by {@link HardwareAccessLayer#update(Resource)}.
     * 
     * @param context The current context
     * @param res The Resource to update
     * @param handle The ResourceHandle associated with res, may be null
     * @return The ResourceHandle to use until the next update
     */
    public ResourceHandle update(OpenGLContext context, R res, ResourceHandle handle);

    /**
     * Reset any internal data that is tracking the state of the resource so
     * that the next update is a full update. See
     * {@link HardwareAccessLayer#reset(Resource)}. This does not have to delete
     * any of the resource's low-level data since it does not act like a
     * dispose. Unlike the other resource operations, this may not have a
     * current context.
     * 
     * @param res The Resource to reset
     * @param handle The ResourceHandle associated with res
     */
    public void reset(R res, ResourceHandle handle);

    /**
     * Dispose of all low-level graphics resources that are associated with this
     * ResourceHandle. The given handle was, at some point, earlier returned by
     * a call to {@link #init(Resource)} on this driver. The provided handle
     * should have its status set to DISPOSED. There is a chance that the
     * Resource instance associated with the handle has already been garbage
     * collected.
     * 
     * @param context The current context
     * @param handle The ResourceHandle to dispose of
     */
    public void dispose(OpenGLContext context, ResourceHandle handle);

    /**
     * Return the class type that this ResourceDriver can process. This should
     * return the class of the most abstract Resource type the driver supports.
     * 
     * @return The resource type processed by the driver
     */
    public Class<R> getResourceType();
}
