package com.ferox.renderer.impl2;

import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

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
     * Initialize a Resource. This includes determining it's low-level handle id
     * for the first time and then completing the equivalent of a full update.
     * This is responsible for performing any error checks that would make the
     * Resource unusable.
     *
     * @param context The current context 
     * @param res The Resource to initialize
     * @return The ResourceHandle that will now be associated to res
     */
    public ResourceHandle init(OpenGLContextAdapter context, R res);

    /**
     * Perform an update on the given resource based on the dirty state. The
     * ResourceHandle is the ResourceHandle instance previously returned by an
     * {@link #init(Resource)} call on this driver for res. This is responsible
     * for performing any error checks that would make the Resource unusable.
     * This is responsible for performing the operations required by
     * {@link HardwareAccessLayer#update(Resource)}.
     * 
     * @param context The current context
     * @param res The Resource to update
     * @param handle The ResourceHandle associated with res
     * @return The new Status of the Resource
     */
    public Status update(OpenGLContextAdapter context, R res, ResourceHandle handle);

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
    public void dispose(OpenGLContextAdapter context, ResourceHandle handle);

    /**
     * Return the class type that this ResourceDriver can process. This should
     * return the class of the most abstract Resource type the driver supports.
     * 
     * @return The resource type processed by the driver
     */
    public Class<R> getResourceType();
}
