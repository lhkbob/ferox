package com.ferox.renderer.impl;

import java.util.concurrent.Future;

import com.ferox.renderer.Framework;
import com.ferox.renderer.UnsupportedResourceException;
import com.ferox.resource.DirtyState;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

/**
 * <p>
 * ResourceManager manages all operations that can be performed on Resources by
 * a Framework. It is responsible for scheduling updates, disposals and cleaning
 * up Resources automatically when they are GC'ed. In addition, they are
 * responsible for managing each Resource's ResourceHandle so that other
 * components of a Framework can query the handle and use it to perform
 * rendering, etc.
 * </p>
 * <p>
 * Like {@link RenderManager}, the ResourceManager must be thread safe.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface ResourceManager {
	/**
	 * Schedule an update for the given Resource. This is called directly by an
	 * AbstractFramework after ensuring the Resource is not null. It also
	 * provides the DirtyDescriptor at the time the method was called. This
	 * method is responsible for performing, or setting in motion, all actions
	 * necessary to follow {@link Framework#update(Resource, boolean)}'s
	 * specification.
	 * 
	 * @param resource The Resource to be updated
	 * @param dirtyDescriptor The DirtyDescriptor of the resource, or null if
	 *            the update was forced to be a full update
	 * @return A Future tied to this update task
	 * @throws UnsupportedResourceException if resource isn't supported
	 */
	public Future<Status> scheduleUpdate(Resource resource, DirtyState<?> dirtyDescriptor);

	/**
	 * Schedule the disposal of the given Resource. This is called directly by
	 * an AbstractFramework after ensuring the Resource is not null. This method
	 * is responsible for performing, or setting in motion, all actions
	 * necessary to follow the specification of
	 * {@link Framework#dispose(Resource)}.
	 * 
	 * @param resource The Resource to be disposed of
	 * @return A Future tied this disposal task
	 * @throws UnsupportedResourceException if resource isn't supported
	 */
	public Future<Object> scheduleDispose(Resource resource);

	/**
	 * <p>
	 * Return a ResourceHandle that's associated with the given Resource. This
	 * is perhaps the most important part of the ResourceManager interface. This
	 * method is intended to be used by other components of the
	 * AbstractFramework to get a handle to a Resource so that it can actually
	 * be used while rendering.
	 * </p>
	 * <p>
	 * This method must update the Resource if necessary, or initialize it if
	 * it's never been seen by the Framework, before returning its associated
	 * ResourceHandle. Because this operation must block, and could be called
	 * frequently, is highly recommended that in the common situation where a
	 * Resource has been initialized, but does not need to be updated, that the
	 * ResourceHandle be returned without any synchronization.
	 * </p>
	 * 
	 * @param resource The Resource whose handle is returned
	 * @return The ResourceHandle for resource, this will not be null
	 * @throws UnsupportedResourceException if resource isn't supported
	 */
	public ResourceHandle getHandle(Resource resource);

	/**
	 * @param resource The Resource whose Status is returned
	 * @return Return the current Status of resource, or null if it's
	 *         unsupported
	 */
	public Status getStatus(Resource resource);

	/**
	 * @param resource The Resource whose status message is returned
	 * @return Return the current status message of resource, or null if it's
	 *         unsupported
	 */
	public String getStatusMessage(Resource resource);

	/**
	 * Destroy this ResourceManager. If the manager has already been destroyed,
	 * then do nothing. This method is responsible for canceling any pending
	 * updates, clean-ups and interrupting any worker threads so that it
	 * terminates quickly.
	 */
	public void destroy();
}
