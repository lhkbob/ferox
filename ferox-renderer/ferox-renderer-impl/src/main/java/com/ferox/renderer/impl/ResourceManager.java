/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl;

import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Resource.UpdatePolicy;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ResourceManager is a manager that controls the data linking Resource instances to
 * ResourceHandles created by ResourceDrivers. It also handles the cleanup of all orphaned
 * handles so there are not memory leaks of low-level system resources. When paired with a
 * {@link ContextManager} and a {@link SurfaceFactory}, the majority of requirements for a
 * Framework implementation can be easily fulfilled.
 *
 * @author Michael Ludwig
 */
public class ResourceManager {
    // These are final after "initialize()" is called
    private LifeCycleManager lifecycleManager;
    private Thread garbageCollector;

    private final ContextManager contextManager;

    private final ReferenceQueue<Resource> collectedResources;
    private final ConcurrentMap<Integer, ResourceData> resources;
    private final Map<Class<? extends Resource>, ResourceDriver> drivers;

    /**
     * <p/>
     * Create a new ResourceManager that uses the given ContextManager to queue tasks to
     * dispose of graphics card level data tied to garbage-collected resources. The
     * provided ResourceDrivers are used to process specific types of resources. They
     * implicitly define the set of supported resource types.
     *
     * @param lockOrder      A Comparator that imposes an ordering on resources when they
     *                       must be relocked in a consistent manner
     * @param contextManager The ContextManager used by the ResourceManager (and the rest
     *                       of the framework)
     * @param drivers        A varargs array of resource drivers, cannot have any null
     *                       values
     *
     * @throws NullPointerException if lockOrder, contextManager or any of the drivers are
     *                              null
     */
    public ResourceManager(ContextManager contextManager, ResourceDriver... drivers) {
        if (contextManager == null) {
            throw new NullPointerException("ContextManager cannot be null");
        }
        this.contextManager = contextManager;

        collectedResources = new ReferenceQueue<Resource>();
        resources = new ConcurrentHashMap<Integer, ResourceData>();

        Map<Class<? extends Resource>, ResourceDriver> tmpDrivers = new HashMap<Class<? extends Resource>, ResourceDriver>();
        if (drivers != null) {
            // Build up the map of drivers based on the input array
            for (ResourceDriver driver : drivers) {
                if (driver == null) {
                    throw new NullPointerException("ResourceDriver cannot be null");
                }
                tmpDrivers.put(driver.getResourceType(), driver);
            }
        }

        // We don't modify the map after the constructor so that it is thread-safe,
        // but wrap it in an immutable collection to make sure.
        this.drivers = Collections.unmodifiableMap(tmpDrivers);
    }

    /**
     * <p/>
     * Complete the initialization of this ResourceManager and start up an inner thread
     * that handles processing garbage collected resources. This method ties the
     * ResourceManager to the life cycle enforced by the given LifeCycleManager. It is
     * required that this method is called by the ResourceManager's owner in the
     * initialization Runnable passed to {@link LifeCycleManager#start(Runnable)}. The
     * provided LifeCycleManager should be the same manager that was used to initialize
     * this ResourceManager's ContextManager. Because the ResourceManager depends on the
     * ContextManager, the ContextManager must be initialized first.
     * <p/>
     * The ResourceManager will automatically terminate its threads when it detects that
     * the LifeCycleManager is being shutdown. All internal threads are managed threads so
     * the final destruction task passed to {@link LifeCycleManager#destroy(Runnable)}
     * will not run until the ResourceManager's thread terminates.
     * <p/>
     * The ResourceManager cannot be initialized more than once. It is illegal to use a
     * LifeCycleManager that has a status other than STARTING (i.e. within the scope of
     * its initialize() method).
     *
     * @param lifecycle The LifeCycleManager that controls when the ResourceManager ends
     *
     * @throws NullPointerException  if lifecycle is null
     * @throws IllegalStateException if lifecycle doesn't have a status of STARTING, or if
     *                               the ResourceManager has already been initialized
     */
    public void initialize(LifeCycleManager lifecycle) {
        if (lifecycle == null) {
            throw new NullPointerException("LifeCycleManager cannot be null");
        }

        // We are assuming that we're in the right threading situation, so this is safe.
        // If this is called outside of the manager's lock then all bets are off, but that's their fault.
        if (lifecycle.getStatus() !=
            com.ferox.renderer.impl.LifeCycleManager.Status.STARTING) {
            throw new IllegalStateException(
                    "LifeCycleManager must have status STARTING, not: " +
                    lifecycle.getStatus());
        }

        // Do a simple exclusive lock to check for double-init attempts. This won't hurt threading
        // since we should already be in lifecycle's write lock.
        synchronized (this) {
            if (lifecycleManager != null) {
                throw new IllegalStateException("ResourceManager already initialized");
            }
            lifecycleManager = lifecycle;
        }

        garbageCollector = new Thread(lifecycleManager.getManagedThreadGroup(),
                                      new WeakReferenceMonitor(), "resource-gc-thread");
        garbageCollector.setDaemon(true);
        lifecycleManager.startManagedThread(garbageCollector);
    }

    /**
     * Get an exclusive lock on the resource that prevents it from being updated, disposed
     * of, or locked via {@link #lock(OpenGLContext, Resource)}.
     *
     * @param resource
     *
     * @return The resource handle for the resource, or null if the lock was unsuccessful
     */
    public Object lockExclusively(Resource resource) {
        if (resource == null) {
            throw new NullPointerException("Resource cannot be null");
        }

        ResourceData data = getData(resource);
        if (data != null) {
            data.lock();
            if (data.handle == null) {
                data.handle = data.driver.init(resource);
            }
            return data.handle;
        } else {
            return null;
        }
    }

    /**
     * Release the exclusive lock on the given resource after {@link
     * #lockExclusively(Resource)} returned a non-null handle. After a call to this
     * method, the locked resource can be locked via {@link #lock(OpenGLContext,
     * Resource)}, updated, and possibly disposed of (depending on its disposable
     * status).
     *
     * @param resource
     */
    public void unlockExclusively(Resource resource) {
        if (resource == null) {
            throw new NullPointerException("Resource cannot be null");
        }

        ResourceData data = getDataIfExists(resource);
        if (data != null) {
            data.unlock();
        }
    }

    /**
     * Unlock the Resource after it has been unbound. This should not be called more times
     * than the resource was locked. After a successful call to {@link
     * #lock(OpenGLContext, Resource)}, this must be called when the resource is no longer
     * being used. It must not need to be called if the resource was not bound
     * successfully.
     *
     * @param resource The resource to unlock
     *
     * @throws NullPointerException if resource is null
     */
    public void unlock(Resource resource) {
        if (resource == null) {
            throw new NullPointerException("Resource cannot be null");
        }

        ResourceData data = getDataIfExists(resource);
        if (data != null) {
            data.unlockShared();
        }
    }

    /**
     * <p/>
     * Lock the given Resource, <var>r</var> so it can be safely bound on the given
     * context. This is a shared lock so that it can be locked multiple times for binding
     * purposes (i.e. to multiple texture units). While a Resource is bound, it cannot be
     * updated or disposed of.
     * <p/>
     * This will automatically update the resource if its update policy is ON_DEMAND. If
     * the resource is not ready, false is returned and the Renderer should cancel the
     * binding. This can happen if the resource has an error, is unsupported, or has an
     * update policy of MANUAL.
     * <p/>
     * It is assumed that the context is current, and that the calling thread is the
     * context thread of the framework.
     *
     * @param context The current context on the thread
     * @param r       The resource to lock
     *
     * @return The handle for the resource, or null
     *
     * @throws NullPointerException if context or r are null
     */
    public Object lock(OpenGLContext context, Resource r) {
        if (r == null) {
            throw new NullPointerException("Resource cannot be null");
        }
        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        boolean needsUpdate;
        ResourceData data;
        if (r.getUpdatePolicy() == UpdatePolicy.ON_DEMAND) {
            data = getData(r);
            needsUpdate = true;
        } else {
            data = getDataIfExists(r);
            needsUpdate = false;
        }

        if (data != null) {
            if (needsUpdate && !data.isLocked()) {
                // only do the update if we're not already bound, otherwise
                // ON_DEMAND would constantly fail if bound in multiple places
                update(context, r);
            }

            if (data.status == Status.READY) {
                data.lockShared();
                return data.handle;
            }
        }

        // If we've reached this point, it means the resource was one of:
        //  - UNSUPPORTED
        //  - DISPOSED with an update policy of MANUAL
        //  - not READY
        return null;
    }

    /**
     * Update the given resource as required by {@link HardwareAccessLayer#update(Resource)}.
     * This will only update if the ResourceDriver's for <var>r</var> detect that the
     * resource has been changed. The new status of the resource is returned. The provided
     * context is assumed to be the context current on the calling thread; if this is not
     * true then undefined behavior will result.
     *
     * @param <R>     The Resource type of r
     * @param context The current context on the calling thread
     * @param r       The resource to update
     *
     * @return The new status of r
     *
     * @throws NullPointerException if context or r are null
     */
    public Status update(OpenGLContext context, Resource r) {
        if (r == null) {
            throw new NullPointerException("Resource cannot be null");
        }
        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        ResourceData data = getData(r);
        if (data == null) {
            return Status.UNSUPPORTED;
        }

        data.lock();
        try {
            if (data.handle == null) {
                data.handle = data.driver.init(r);
            }

            try {
                // for correct thread-safe access of the resource,
                // we must synchronize on resource here
                synchronized (r) {
                    data.message = data.driver.update(context, r, data.handle);
                }
                data.status = Status.READY;
            } catch (UpdateResourceException e) {
                data.message = e.getMessage();
                data.status = Status.ERROR;
            }
        } finally {
            data.unlock();
        }
        return data.status;
    }

    /**
     * Dispose of the given resource as required by {@link HardwareAccessLayer#dispose(Resource)}.
     * If the resource has no handle, this does nothing since the resource is already
     * effectively disposed of. The provided context is assumed to be the context current
     * on the calling thread; if this is not true then undefined behavior will result.
     *
     * @param <R>     The Resource type of r
     * @param context The current context on this thread
     * @param r       The resource to dispose of
     *
     * @throws NullPointerException  if context or r are null
     * @throws IllegalStateException if r cannot be disposed of (see {@link
     *                               #setDisposable(Resource, boolean)}).
     */
    public <R extends Resource> void dispose(OpenGLContext context, R r) {
        if (r == null) {
            throw new NullPointerException("Resource cannot be null");
        }
        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        ResourceData data = getDataIfExists(r);
        if (data == null) {
            return; // Don't need to dispose
        } else if (!data.disposable) {
            throw new IllegalStateException(
                    "Resource is in use by a Surface and cannot be disposed");
        }

        data.lock();
        try {
            if (data.handle != null) {
                data.driver.dispose(context, data.handle);
            }
        } finally {
            data.unlock();
        }
        data.handle = null;
        data.status = Status.DISPOSED;
        data.message = "";

        // Don't remove the ResourceData from the resources map because we want to
        // keep reusing the ResourceData instance. That way other threads that might have
        // instances to it don't need to constantly look it up.  The ResourceData is cleaned
        // up only once the Resource has been garbage collected.
    }

    /**
     * Reset the internal tracking of this resource as required by {@link
     * HardwareAccessLayer#reset(Resource)}. If this resource has no ResourceHandle, then
     * this request does nothing. This method should only be called from a ContextManager
     * owned task thread.
     *
     * @param r The resource to reset
     *
     * @throws NullPointerException if r is null
     */
    public void reset(Resource r) {
        if (r == null) {
            throw new NullPointerException("Resource cannot be null");
        }

        ResourceData data = getDataIfExists(r);
        if (data == null || data.handle == null) {
            return; // Nothing to reset
        }

        data.driver.reset(data.handle);
    }

    /**
     * <p/>
     * Set whether or not the given resource, <var>r</var>, is disposable. If it is not
     * disposable, an exception is thrown when {@link #dispose(OpenGLContext, Resource)}
     * is invoked. This can be used to prevent the textures used by a TextureSurface from
     * being disposed of until the surface is destroyed.
     * <p/>
     * This does nothing if r is an unsupported resource type. If the resource is already
     * disposed, this flags it for after the next time it is initialized.
     * <p/>
     * This should only be called from a context thread.
     *
     * @param r          The resource to flag as disposable or not
     * @param disposable True if it can be disposed
     *
     * @throws NullPointerException if r is null
     */
    public void setDisposable(Resource r, boolean disposable) {
        if (r == null) {
            throw new NullPointerException("Resource cannot be null");
        }

        ResourceData data = getData(r);
        if (data != null) {
            data.disposable = disposable;
        }
    }

    /**
     * Return the current status message of the given resource. This functions identically
     * to {@link Framework#getStatusMessage(Resource)}. This returns null if the manager's
     * lifecycle has ended. In most cases, the empty string is returned unless the
     * resource has a status of ERROR (since that is when the message is most
     * informative).
     *
     * @param r The resource whose status message is queried
     *
     * @return The status message of r
     *
     * @throws NullPointerException if r is null
     */
    public String getStatusMessage(Resource r) {
        if (r == null) {
            throw new NullPointerException("Resource cannot be null");
        }

        // Special case for if we're shutting down
        if (lifecycleManager.isStopped()) {
            return null;
        }

        ResourceData data = getDataIfExists(r);
        if (data == null) {
            // This is either a disposed resource, or an unsupported resource,
            // but all of that is encoded in the Status, the message doesn't matter
            return "";
        }

        return data.message;
    }

    /**
     * Return the current Status of the given resource. This functions identically to
     * {@link Framework#getStatus(Resource)}. This returns DISPOSED if the manager's
     * lifecyle has ended.
     *
     * @param r The resource whose status is queried
     *
     * @return The status of r
     *
     * @throws NullPointerException if r is null
     */
    public Status getStatus(Resource r) {
        if (r == null) {
            throw new NullPointerException("Resource cannot be null");
        }

        // Special case for if we're shutting down
        if (lifecycleManager.isStopped()) {
            return Status.DISPOSED;
        }

        ResourceData data = getDataIfExists(r);
        if (data == null) {
            // This is either disposed or unsupported, so we have to check the driver.
            // If there is a driver, then it is supported but disposed
            return (getDriver(r) == null ? Status.UNSUPPORTED : Status.DISPOSED);
        }

        return data.status;
    }

    private ResourceDriver getDriver(Resource resource) {
        Class<?> clazz = resource.getClass();
        while (clazz != null && Resource.class.isAssignableFrom(clazz)) {
            ResourceDriver d = drivers.get(clazz);
            if (d != null) {
                return d;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private ResourceData getData(Resource resource) {
        ResourceData data = resources.get(resource.getId());
        if (data == null) {
            // no data, but we must allocate it
            ResourceDriver driver = getDriver(resource);
            if (driver == null) {
                return null; // Unsupported resources never store an RD
            }

            data = new ResourceData(resource, driver, collectedResources);
            ResourceData contendedInsert = resources.putIfAbsent(resource.getId(), data);
            if (contendedInsert != null) {
                // another thread inserted a ResourceData before us, so use it
                // - this means we have multiple weak references that can get
                //   queued, but the gc thread can handle that
                data = contendedInsert;
            }
        }

        return data;
    }

    private ResourceData getDataIfExists(Resource resource) {
        // resources is a ConcurrentMap so this is always safe
        return resources.get(resource.getId());
    }

    /*
     * Internal runner that monitors a ReferenceQueue to dispose of
     * ResourceHandles after their owning Resources have been collected.
     */
    private class WeakReferenceMonitor implements Runnable {
        @Override
        public void run() {
            while (!lifecycleManager.isStopped()) {
                try {
                    // The Resource associated with this data has been GC'ed,
                    // which means its impossible for getResourceData(),
                    // update(), dispose(), etc to be called
                    ResourceData data = (ResourceData) collectedResources.remove();

                    if (data.handle != null) {
                        // Don't block on this, we just need it to be disposed of in the future
                        // and don't bother accepting during shutdown since the context
                        // is about to be destroyed then anyway.
                        contextManager.invokeOnContextThread(
                                new DisposeOrphanedHandleTask(data), false);
                    }

                    // Remove it from the collection of current resources
                    resources.remove(data.resourceId);
                } catch (InterruptedException e) {
                    // Do nothing and keep looping
                }
            }
        }
    }

    /*
     * Internal task to clean up a resource after it has been garbage collected.
     */
    private class DisposeOrphanedHandleTask implements Callable<Void> {
        private final ResourceData data;

        public DisposeOrphanedHandleTask(ResourceData data) {
            this.data = data;
        }

        @Override
        public Void call() throws Exception {
            OpenGLContext context = contextManager.ensureContext();

            // We don't need to worry about the disposable property at this point,
            // if the Resource has been GC'ed, we need the handle disposed of no matter what.
            if (data.handle != null) {
                data.driver.dispose(context, data.handle);
            }
            data.handle = null;
            data.status = Status.DISPOSED;
            data.message = "Resource was garbage-collected";
            return null;
        }
    }

    /*
     * Internal wrapper that collects a driver, ResourceHandle and weak
     * reference to a Resource.
     */
    private static class ResourceData extends WeakReference<Resource> {
        final ResourceDriver driver;
        final int resourceId;

        // not volatile, should only be accessed on context thread
        Object handle;
        boolean disposable; // True if resource can be disposed
        int sharedLockCount; // number of times bound by a Renderer
        boolean exclusiveLock; // true if cannot be updated or bound

        // marked volatile so that other threads can safely read values,
        // only the context thread will ever write values so no explicit
        // synchronization is necessary
        volatile String message;
        volatile Status status;

        public ResourceData(Resource resource, ResourceDriver driver,
                            ReferenceQueue<Resource> queue) {
            super(resource, queue);

            this.driver = driver;
            resourceId = resource.getId();
            disposable = true;
            handle = null;
            sharedLockCount = 0;
            exclusiveLock = false;

            // these values should never be null
            status = Status.DISPOSED;
            message = "";
        }

        public boolean isLocked() {
            return sharedLockCount > 0 || exclusiveLock;
        }

        public void lock() {
            if (sharedLockCount > 0) {
                throw new IllegalStateException(
                        "Resource is already locked in shared mode, cannot obtain exclusive lock");
            }
            if (exclusiveLock) {
                throw new IllegalStateException("Resource is already exlusively locked");
            }
            exclusiveLock = true;
        }

        public void unlock() {
            if (!exclusiveLock) {
                throw new IllegalStateException(
                        "Resource is not exclusively locked, and cannot be unlocked");
            }
            exclusiveLock = false;
        }

        public void lockShared() {
            if (exclusiveLock) {
                throw new IllegalStateException(
                        "Resource is already exclusively locked, cannot obtain a shared lock");
            }
            sharedLockCount++;
        }

        public void unlockShared() {
            if (sharedLockCount == 0) {
                throw new IllegalStateException(
                        "Resource is not locked in shared mode, and cannot be unlocked");
            }
            sharedLockCount--;
        }
    }
}
