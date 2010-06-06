package com.ferox.renderer.impl;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderException;
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
     * AbstractFramework after ensuring the Resource is not null. This method is
     * responsible for performing, or setting in motion, all actions necessary
     * to follow {@link Framework#update(Resource, boolean)}'s specification.
     * 
     * @param resource The Resource to be updated
     * @param forceFull The equivalent boolean parameter for the Framework
     *            method
     * @return A Future tied to this update task
     */
	public Future<Status> scheduleUpdate(Resource resource, boolean forceFull);

	/**
	 * Schedule the disposal of the given Resource. This is called directly by
	 * an AbstractFramework after ensuring the Resource is not null. This method
	 * is responsible for performing, or setting in motion, all actions
	 * necessary to follow the specification of
	 * {@link Framework#dispose(Resource)}.
	 * 
	 * @param resource The Resource to be disposed of
	 * @return A Future tied this disposal task
	 */
	public Future<Void> scheduleDispose(Resource resource);

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
	 */
	public ResourceHandle getHandle(Resource resource);

	/**
	 * @param resource The Resource whose Status is returned
	 * @return Return the current Status of resource
	 */
	public Status getStatus(Resource resource);

	/**
	 * @param resource The Resource whose status message is returned
	 * @return Return the current status message of resource
	 */
	public String getStatusMessage(Resource resource);

	/**
	 * Destroy this ResourceManager. If the manager has already been destroyed,
	 * then do nothing. This method is responsible for canceling any pending
	 * updates, clean-ups and interrupting any worker threads so that it
	 * terminates quickly.
	 */
	public void destroy();

    /**
     * Complete the initialization of the ResourceManager and begin any Threads
     * needed for correct functioning.
     * 
     * @param lock The lock controlling access to the Framework using this
     *            manager, the lock should be used to manage the work of any
     *            started Threads
     */
	public void initialize(ReentrantReadWriteLock lock);

    /**
     * Lock the given Resource so that it may not be disposed of until it has
     * been unlocked. This should be used on the TextureImages used by any
     * created TextureSurfaces. This does nothing if a resource is locked
     * multiple times.
     * 
     * @param r The Resource to lock
     * @throws NullPointerException if r is null
     */
	public void lock(Resource r);

    /**
     * Unlock the given Resource so that it may be disposed of. This should be
     * called on the TextureImages used by created TextureSurfaces when they are
     * destroyed. This does nothing if the resource was alread unlocked.
     * 
     * @param r The Resource to unlock
     * @throws NullPointerException if r is null
     */
	public void unlock(Resource r);

    /**
     * <p>
     * Assign the Context that this ResourceManager will use to perform updates
     * and disposals. The AbstractFramework will notify the ResourceManager the
     * first time a surface is created with a non-null Context if the manager
     * does not already have a context. It will also set it to null if the
     * surface becomes destroyed later on.
     * </p>
     * <p>
     * When this is set to null, the ResourceManager assumes that all resources
     * have been disconnected. If a non-surface Context is used, it is assumed
     * that its lifetime matches the Framework's and that created surfaces share
     * resources across their context. If this is not true, whoever owns the
     * Context must manually set the context to null when it becomes invalid.
     * </p>
     * 
     * @param context The Context to use by this ResourceManager
     * @throws RenderException if {@link #getContext()} returns a non-null
     *             Context and context is also non-null
     */
	public void setContext(Context context);
	
	/**
     * @return The currently assigned Context used by this ResourceManager
     */
	public Context getContext();

    /**
     * Utility to invoke the given Runnable on the currently assigned resource
     * Context. This should be preferred to using
     * <code>getContext().runWithLock(run)</code> because the ResourceManager
     * may be managing which Thread the Context is current on.
     * 
     * @param run The Runnable to run
     * @throws NullPointerException if run is null
     * @throws RenderException if the ResourceManager has no resource Context
     */
	public void runOnResourceThread(Runnable run);
}
