package com.ferox.renderer.impl2.drivers;

import com.ferox.renderer.impl.DefaultResourceManager;
import com.ferox.renderer.impl.resource.ResourceHandle;
import com.ferox.resource.DirtyState;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

/**
 * A ResourceDriver manages the lifecycle of a given Resource type. It is used
 * by {@link DefaultResourceManager} to perform the actual low-level graphics
 * operations when it is appropriate. Implementations may assume that a
 * JoglContext is current for each of the three methods exposed here.
 * 
 * @author Michael Ludwig
 */
public interface ResourceDriver {
    /**
     * Initialize a Resource. This includes determining it's low-level handle id
     * for the first time and then completing the equivalent of a full update.
     * This is responsible for performing any error checks that would make the
     * Resource unusable.
     * 
     * @param res The Resource to initialize
     * @return The ResourceHandle that will now be associated to res
     */
    public ResourceHandle init(Resource res);

    /**
     * Perform an update on the given resource based on the dirty state. The
     * ResourceHandle is the ResourceHandle instance previously returned by an
     * {@link #init(Resource)} call on this driver for res. This is responsible
     * for performing any error checks that would make the Resource unusable.
     * 
     * @param res The Resource to update
     * @param handle The ResourceHandle associated with res
     * @param dirtyState The DirtyState to use in the update, may be null
     * @return The new Status of the Resource
     */
    public Status update(Resource res, ResourceHandle handle, DirtyState<?> dirtyState);

    /**
     * Dispose of all low-level graphics resources that are associated with this
     * ResourceHandle. The given handle was, at some point, earlier returned by
     * a call to {@link #init(Resource)} on this driver. The JoglResourceManager
     * will take care of updating the Status for the Resource at this point, it
     * is not necessary to declare the handle's Status as DISPOSED.
     * 
     * @param handle The ResourceHandle to dispose of
     */
    public void dispose(ResourceHandle handle);
}
