package com.ferox.renderer.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.renderer.Framework;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Resource.UpdatePolicy;

/**
 * ResourceManager is a manager that controls the data linking Resource
 * instances to ResourceHandles created by ResourceDrivers. It also handles the
 * cleanup of all orphaned handles so there are not memory leaks of low-level
 * system resources. When paired with a {@link ContextManager} and a
 * {@link SurfaceFactory}, the majority of requirements for a Framework
 * implementation can be easily fulfilled.
 * 
 * @author Michael Ludwig
 */
public class ResourceManager {
    // These are final after "initialize()" is called
    private LifeCycleManager lifecycleManager;
    private Thread garbageCollector;
    
    private final ContextManager contextManager;
    
    private final ThreadLocal<List<LockToken<?>>> locks;
    
    private final ReferenceQueue<Resource> collectedResources;
    private final ConcurrentMap<Integer, ResourceData<?>> resources;
    private final Map<Class<? extends Resource>, ResourceDriver<?>> drivers;

    /**
     * Create a new ResourceManager that uses the given ContextManager to queue
     * tasks to dispose of graphics card level data tied to garbage-collected
     * resources. The provided ResourceDrivers are used to process specific
     * types of resources. They implicitly define the set of supported resource
     * types.
     * 
     * @param contextManager The ContextManager used by the ResourceManager (and
     *            the rest of the framework)
     * @param drivers A varargs array of resource drivers, cannot have any null
     *            values
     * @throws NullPointerException if contextManager or any of the drivers are
     *             null
     */
    public ResourceManager(ContextManager contextManager, ResourceDriver<?>... drivers) {
        if (contextManager == null)
            throw new NullPointerException("ContextManager cannot be null");
        this.contextManager = contextManager;
        
        locks = new ThreadLocal<List<LockToken<?>>>() {
            @Override
            protected List<LockToken<?>> initialValue() {
                return new ArrayList<LockToken<?>>();
            }
        };
        
        collectedResources = new ReferenceQueue<Resource>();
        resources = new ConcurrentHashMap<Integer, ResourceData<?>>();
        
        Map<Class<? extends Resource>, ResourceDriver<?>> tmpDrivers = new HashMap<Class<? extends Resource>, ResourceDriver<?>>();
        if (drivers != null) {
            // Build up the map of drivers based on the input array
            for (ResourceDriver<?> driver: drivers) {
                if (driver == null)
                    throw new NullPointerException("ResourceDriver cannot be null");
                tmpDrivers.put(driver.getResourceType(), driver);
            }
        }
        
        // We don't modify the map after the constructor so that it is thread-safe,
        // but wrap it in an immutable collection to make sure.
        this.drivers = Collections.unmodifiableMap(tmpDrivers);
    }

    /**
     * <p>
     * Complete the initialization of this ResourceManager and start up an inner
     * thread that handles processing garbage collected resources. This method
     * ties the ResourceManager to the life cycle enforced by the given
     * LifeCycleManager. It is required that this method is called by the
     * ResourceManager's owner in the initialization Runnable passed to
     * {@link LifeCycleManager#start(Runnable)}. The provided LifeCycleManager
     * should be the same manager that was used to initialize this
     * ResourceManager's ContextManager. Because the ResourceManager depends on
     * the ContextManager, the ContextManager should be initialized first.
     * </p>
     * <p>
     * The ResourceManager will automatically terminate its threads when it
     * detects that the LifeCycleManager is being shutdown. All internal threads
     * are managed threads so the final destruction code passed to
     * {@link LifeCycleManager#destroy(Runnable)} will not run until the
     * ResourceManager's thread terminates.
     * </p>
     * <p>
     * The ResourceManager cannot be initialized more than once. It is illegal
     * to use a LifeCycleManager that has a status other than STARTING (i.e.
     * within the scope of its initialize() method).
     * </p>
     * 
     * @param lifecycle The LifeCycleManager that controls when the
     *            ResourceManager ends
     * @throws NullPointerException if lifecycle is null
     * @throws IllegalStateException if lifecycle doesn't have a status of
     *             STARTING, or if the ResourceManager has already been
     *             initialized
     */
    public void initialize(LifeCycleManager lifecycle) {
        if (lifecycle == null)
            throw new NullPointerException("LifeCycleManager cannot be null");
        
        // We are assuming that we're in the right threading situation, so this is safe.
        // If this is called outside of the manager's lock then all bets are off, but that's their fault.
        if (lifecycle.getStatus() != com.ferox.renderer.impl.LifeCycleManager.Status.STARTING)
            throw new IllegalStateException("LifeCycleManager must have status STARTING, not: " + lifecycle.getStatus());
        
        // Do a simple exclusive lock to check for double-init attempts. This won't hurt threading
        // since we should already be in lifecycle's write lock.
        synchronized(this) {
            if (lifecycleManager != null)
                throw new IllegalStateException("ResourceManager already initialized");
            lifecycleManager = lifecycle;
        }
        
        garbageCollector = new Thread(lifecycleManager.getManagedThreadGroup(), new WeakReferenceMonitor(), "resource-disposer");
        lifecycleManager.startManagedThread(garbageCollector);
    }

    /*
     * Internal method to handle actual locking of a resource and resourceData.
     * The resource is an argument to make sure the weak reference in the data
     * is not garbage collected. The LockListener may be null.
     */
    private <R extends Resource> LockToken<R> lockHandle(R resource, ResourceData<R> data, LockListener<R> listener, boolean writeLock) {
        List<LockToken<?>> threadLocks = locks.get();
        LockToken<R> newToken = new LockToken<R>(resource, data, listener, writeLock, false);
        
        Lock lock = (writeLock ? data.lock.writeLock() : data.lock.readLock());
        
        // We can't upgrade a read lock to a write lock, so if we have a read lock (readHoldCount > 0)
        // and we don't already have a write lock, but need one, we need to reorder the locks so that
        // the write lock is the outer lock.
        boolean forceReorder = (writeLock && !data.lock.isWriteLockedByCurrentThread() 
                                && data.lock.getReadHoldCount() > 0);
        
        if (forceReorder || !lock.tryLock()) {
            // Unwind already held locks
            LockToken<?> other;
            for (int i = threadLocks.size() - 1; i >= 0; i--) {
                other = threadLocks.get(i);
                boolean relock = other.notifyForceUnlock();
                other.unlock();
                
                if (!relock)
                    threadLocks.remove(i); // safe because this is reverse list traversal
            }
            
            // Add new token and sort the list
            threadLocks.add(newToken);
            Collections.sort(threadLocks);
            
            // Relock all locks, blocking until completed
            int ct = threadLocks.size();
            for (int i = 0; i < ct; i++) {
                other = threadLocks.get(i);
                other.lock();
                
                if (other != newToken) {
                    // We only do these actions if it's not the new token. The new token
                    // can't be "relocked" because it was the first lock. And if there is no
                    // handle for the new token, we want to let the calling code deal with it.
                    
                    boolean keepLock = other.notifyRelock();
                    if (!keepLock) {
                        other.unlock();
                        threadLocks.remove(i);
                        i--; // decrement i by one so we don't skip the shifted element
                    }
                }
            }
            
            // Since newToken was added to the list, we have locked the new one, too
        } else {
            // Managed to lock in an arbitrary order, so just add it to the list
            threadLocks.add(newToken);
        }
        
        return newToken;
    }

    /**
     * Unlock the Resource that had previously been locked by the given
     * LockToken. This does nothing if the token has already been unlocked. Any
     * LockListener specified when the token was created is not notified of the
     * unlock, this only occurs when the unlock is forced for internal
     * lock-reordering.
     * 
     * @param token The token to unlock
     * @throws NullPointerException if token is null
     */
    public void unlock(LockToken<?> token) {
        if (token == null)
            throw new NullPointerException("LockToken cannot be null");
        
        if (token.fullLock || locks.get().remove(token)) {
            // Unlock token if it's still in the locks list
            // or if it's a lock that wants to break all of the rules
            token.unlock();
        }
    }

    /**
     * <p>
     * Lock the given Resource, <tt>r</tt> for use on the given context. It is
     * assumed that the context is current on the calling thread. Internally,
     * the ResourceManager manages a read-write lock for the given Resource.
     * When the Resource has a MANUAL update policy, the read-lock is used,
     * allowing for more concurrency.
     * </p>
     * <p>
     * Because resource locks may be locked and unlocked in a non-deterministic
     * order, the ResourceManager may need to unlock and re-order all held locks
     * on the current thread to prevent deadlocks. This is also done if a read
     * lock must be upgrade to a write lock (i.e. if
     * {@link #update(OpenGLContext, Resource)} or
     * {@link #dispose(OpenGLContext, Resource)} is called when the
     * resource is bound to a renderer).
     * </p>
     * <p>
     * A LockListener can be provided that will be invoked as appropriate when
     * resources are automatically unlocked and relocked in the event of
     * deadlock prevention. The LockListeners are not invoked the first time the
     * lock is held, or when the resource is unlocked by
     * {@link #unlock(LockToken)}.
     * </p>
     * <p>
     * The returned LockToken provides access to the ResourceHandle of the given
     * Resource. This handle may be null or have a status not equal to READY.
     * Code must properly handle these situations (generally by unlocking the
     * resource and not completing the expected action). It is possible for a
     * locked resource to lose a valid handle during the time period between a
     * forced unlock and a relock.
     * </p>
     * <p>
     * When a LockListener is provided, the listener controls whether or not a
     * resource should be relocked after a forced unlock, or automatically
     * unlocked after a relock. When no listener is provided, a resource will
     * lose its lock if its handle becomes null or invalid.
     * </p>
     * 
     * @param context The current context on the thread
     * @param r The resource to lock
     * @param listener An optional LockListener that is notified of forced
     *            lock/unlock events during lock re-ordering
     * @return A LockToken to unlock the resource when needed, or null if the
     *         resource is of an unsupported type
     * @throws NullPointerException if context or r are null
     */
    public <R extends Resource> LockToken<R> lock(OpenGLContext context, R r, LockListener<R> listener) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        if (context == null)
            throw new NullPointerException("Context cannot be null");

        boolean needsUpdate = false;
        ResourceData<R> data;
        synchronized(r) {
            if (r.getUpdatePolicy() == UpdatePolicy.ON_DEMAND) {
                data = getResourceData(r, true);
                needsUpdate = true;
            } else
                data = getResourceData(r, false);
        }
        
        // At this point if data is null, it means the resource is unknown and wasn't an ON_DEMAND resource,
        // or it was ON_DEMAND but is unsupported. In either case, there is nothing to do
        if (data == null)
            return null;
        
        LockToken<R> token = (needsUpdate ? update(context, data, listener) 
                                          : lockHandle(r, data, listener, false));
        return token;
    }

    /**
     * Exclusively lock the provided resource and block until its handle is
     * available. The lock will not be automatically unlocked/relocked to
     * prevent deadlock (as is the case with locks held by
     * {@link #lock(OpenGLContext, Resource, LockListener)}). Because of this,
     * thread safety becomes the responsibility of the color and it is preferred
     * to use the regular lock method. If the resource has an update policy of
     * ON_DEMAND, it will be updated if need be.
     * 
     * @param <R> The resource type being locked
     * @param context The current context
     * @param r The resource to exclusively lock
     * @return The LockToken to use for unlocking this exclusive lock, returns
     *         null if resource is unuspported
     * @throws NullPointerException if r or context are null
     */
    public <R extends Resource> LockToken<R> acquireFullLock(OpenGLContext context, R r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        if (context == null)
            throw new NullPointerException("Context cannot be null");
        
        boolean needsUpdate = false;
        ResourceData<R> data;
        synchronized(r) {
            if (r.getUpdatePolicy() == UpdatePolicy.ON_DEMAND) {
                data = getResourceData(r, true);
                needsUpdate = true;
            } else
                data = getResourceData(r, false);
        }
        
        if (data == null)
            return null; // See comment in lock()
        
        // here is where we diverge from lock() and handle things differently
        data.lock.writeLock().lock();
        try {
            if (needsUpdate) {
                // perform an update action 
                synchronized(r) {
                    data.handle = data.driver.update(context, r, data.handle);
                }
            }
            
            return new LockToken<R>(r, data, null, true, true);
        } catch(RuntimeException re) {
            data.lock.writeLock().unlock();
            throw re;
        }
    }

    /**
     * Update the given resource as required by
     * {@link HardwareAccessLayer#update(Resource)}. This will only update if
     * the ResourceDriver's for <tt>r</tt> detect that the resource has been
     * changed. The new status of the resource is returned. The provided context
     * is assumed to be the context current on the calling thread; if this is
     * not true then undefined behavior will result.
     * 
     * @param <R> The Resource type of r
     * @param context The current context on the calling thread
     * @param r The resource to update
     * @return The new status of r
     * @throws NullPointerException if context or r are null
     */
    public <R extends Resource> Status update(OpenGLContext context, R r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        if (context == null)
            throw new NullPointerException("Context cannot be null");
        
        ResourceData<R> data;
        synchronized(r) {
            data = getResourceData(r, true);
            if (data == null)
                return Status.UNSUPPORTED;
        }
        
        LockToken<R> token = update(context, data, null);
        Status status = data.handle.getStatus();
        unlock(token);
        return status;
    }

    /*
     * Internal method to obtain a write-lock on the given resource, and perform
     * the actions needed for update() or an ON_DEMAND resource. The LockToken
     * is returned (to be unlocked in the case of update(), or returned to use
     * code in the case of lock()). If an exception is thrown, then the resource
     * is unlocked.
     */
    private <R extends Resource> LockToken<R> update(OpenGLContext context, ResourceData<R> data, LockListener<R> listener) {
        R r = data.get();
        if (r == null)
            return null;
        
        LockToken<R> token = lockHandle(r, data, listener, true); // acquires write lock
        try {
            // Re-lock on the resource to make sure no one edits it on outside threads.
            // Since we already have an exclusive write lock on the ResourceData,
            // we don't need to worry about blocking against other context threads that have
            // multiple locks - and we trust outside threads to only have one resource locked
            // at a time.
            synchronized(r) {
                data.handle = data.driver.update(context, r, data.handle);
                return token;
            }
        } catch(RuntimeException re) {
            // In the event of an exception, unlock the token since normally the caller
            // is responsible for the unlock but that clearly won't work when an exception is thrown.
            unlock(token);
            throw re;
        }
    }

    /**
     * Dispose of the given resource as required by
     * {@link HardwareAccessLayer#dispose(Resource)}. If the resource has no
     * handle, this does nothing since the resource is already effectively
     * disposed of. The provided context is assumed to be the context current on
     * the calling thread; if this is not true then undefined behavior will
     * result.
     * 
     * @param <R> The Resource type of r
     * @param context The current context on this thread
     * @param r The resource to dispose of
     * @throws NullPointerException if context or r are null
     * @throws IllegalStateException if r cannot be disposed of (see
     *             {@link #setDisposable(Resource, boolean)}).
     */
    public <R extends Resource> void dispose(OpenGLContext context, R r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        if (context == null)
            throw new NullPointerException("Context cannot be null");
        
        ResourceData<R> data;
        synchronized(r) {
            data = getResourceData(r, false);
            if (data == null)
                return; // Don't need to dispose
            else if (!data.disposable)
                throw new IllegalStateException("Resource is in use by a Surface and cannot be destroyed");
        }
        
        
        LockToken<R> token = lockHandle(r, data, null, true);
        try {
            if (data.handle != null) {
                data.driver.dispose(context, data.handle);
                data.handle.setStatus(Status.DISPOSED);
            }
            data.handle = null;
            
            // Don't remove the ResourceData from the resources map because we want to
            // keep reusing the ResourceData instance. That way other threads that might have
            // instances to it don't need to constantly look it up.  The ResourceData is cleaned
            // up only once the Resource has been garbage collected.
        } finally {
            unlock(token);
        }
    }

    /**
     * Reset the internal tracking of this resource as required by
     * {@link HardwareAccessLayer#reset(Resource)}. If this resource has no
     * ResourceHandle, then this request does nothing.
     * 
     * @param <R> The Resource type
     * @param r The resource to reset
     * @throws NullPointerException if r is null
     */
    public <R extends Resource> void reset(R r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        
        ResourceData<R> data;
        synchronized(r) {
            data = getResourceData(r, false);
            if (data == null)
                return; // Don't need to dispose
        }
        
        LockToken<R> token = lockHandle(r, data, null, true);
        try {
            if (data.handle != null)
                data.driver.reset(r, data.handle);
        } finally {
            unlock(token);
        }
    }

    /**
     * Set whether or not the given resource, <tt>r</tt>, is disposable. If it
     * is not disposable, an exception is thrown when
     * {@link #dispose(OpenGLContext, Resource)} is invoked. This should
     * be used to prevent the textures used by a TextureSurface from being
     * disposed of until the surface is destroyed. This does nothing if r is an
     * unsupported resource type.
     * 
     * @param <R> The Resource type
     * @param r The resource to flag as disposable or not
     * @param disposable True if it can be disposed
     * @throws NullPointerException if r is null
     */
    public <R extends Resource> void setDisposable(R r, boolean disposable) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        
        synchronized(r) {
            // Since we're only operating on the ResourceData, we can 
            // use just a synchronized block without getting a LockToken
            ResourceData<R> data = getResourceData(r, true);
            if (data != null)
                data.disposable = disposable;
        }
    }

    /**
     * Return the current status message of the given resource. This functions
     * identically to {@link Framework#getStatusMessage(Resource)}. This returns
     * null if the manager's lifecyle has ended. In most cases, the empty string
     * is returned unless the resource has a status of ERROR (since that is when
     * the message is most informative).
     * 
     * @param r The resource whose status message is queried
     * @return The status message of r
     * @throws NullPointerException if r is null
     */
    public <R extends Resource> String getStatusMessage(R r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        
        // Special case for if we're shutting down
        if (lifecycleManager.isStopped())
            return null;

        ResourceData<R> data;
        synchronized(r) {
            data = getResourceData(r, false);
            if (data == null) {
                // This is either a disposed resource, or an unsupported resource,
                // but all of that is encoded in the Status, the message doesn't matter
                return "";
            }
        }
        
        LockToken<R> token = lockHandle(r, data, null, false);
        try {
            return (data.handle != null ? data.handle.getStatusMessage() : "");
        } finally {
            unlock(token);
        }
    }

    /**
     * Return the current Status of the given resource. This functions
     * identically to {@link Framework#getStatus(Resource)}. This returns
     * DISPOSED if the manager's lifecyle has ended.
     * 
     * @param r The resource whose status is queried
     * @return The status of r
     * @throws NullPointerException if r is null
     */
    public <R extends Resource> Status getStatus(R r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        
        // Special case for if we're shutting down
        if (lifecycleManager.isStopped())
            return Status.DISPOSED;
        
        ResourceData<R> data;
        synchronized(r) {
            data = getResourceData(r, false);
            if (data == null) {
                // This is either disposed or unsupported, so we have to check the driver.
                // If there is a driver, then it is supported but disposed
                return (getDriver(r) == null ? Status.UNSUPPORTED : Status.DISPOSED);
            }
        }
        
        LockToken<R> token = lockHandle(r, data, null, false);
        try {
            return (data.handle != null ? data.handle.getStatus() : Status.DISPOSED);
        } finally {
            unlock(token);
        }
    }
    
    /*
     * Query the map of drivers for the best-matching driver.
     */
    @SuppressWarnings("unchecked")
    private <R extends Resource> ResourceDriver<R> getDriver(R resource) {
        Class<?> clazz = resource.getClass();
        while(clazz != null && Resource.class.isAssignableFrom(clazz)) {
            ResourceDriver<?> d = drivers.get(clazz);
            if (d != null)
                return (ResourceDriver<R>) d;
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /*
     * Grab the ResourceData associated with the resource. If vivify is false
     * and there is no resouce data, then null is returned. If vivify is true, a
     * new ResourceData is created and stored for later (unless the resource has
     * no driver, at which point null is returned).
     */
    @SuppressWarnings("unchecked")
    private <R extends Resource> ResourceData<R> getResourceData(R resource, boolean vivify) {
        ResourceData<R> data = (ResourceData<R>) resources.get(resource.getId());
        if (vivify && data == null) {
            ResourceDriver<R> driver = getDriver(resource);
            if (driver == null)
                return null; // Unsupported resources never store an RD
            data = new ResourceData<R>(resource, driver, collectedResources);
            resources.put(resource.getId(), data);
        }
        
        return data;
    }

    /**
     * LockToken is a small token representing the locked state of a Resource. A
     * Resource may be locked multiple times by a thread, and multiple threads
     * may be able to hold read-only locks on the same resource at the same
     * time. LockTokens must be held onto until the resource is unlocked.
     * 
     * @see ResourceManager#lock(OpenGLContext, Resource, LockListener)
     * @see ResourceManager#unlock(LockToken)
     * @param <R> The Resource type that is locked
     */
    public static class LockToken<R extends Resource> implements Comparable<LockToken<?>> {
        private final R resource; // Have an actual reference to the resource so it doesn't get GC'ed
        private final ResourceData<R> data;
        private final LockListener<? super R> listener;
        
        private final boolean writeLock;
        private final boolean fullLock;

        private LockToken(R resource, ResourceData<R> data, LockListener<? super R> listener, boolean writeLockHeld, boolean fullLock) {
            this.resource = resource;
            this.data = data;
            this.listener = listener;

            writeLock = writeLockHeld;
            this.fullLock = fullLock;
        }
        
        /**
         * @return The Resource locked by this token, this will not be null
         */
        public R getResource() {
            return resource;
        }

        /**
         * Return the ResourceHandle associated with the Resource that is locked
         * by this token. The handle may be null if the resource has been
         * disposed of, or it may have a Status other than READY.
         * 
         * @return The ResourceHandle if one exists
         */
        public ResourceHandle getResourceHandle() {
            return data.handle;
        }
        
        // Convenience method to notify any LockListener or perform default action
        private boolean notifyForceUnlock() {
            if (listener != null)
                return listener.onForceUnlock(this);
            else
                return data.handle != null && data.handle.getStatus() == Status.READY;
        }
        
        // Convenience method to notify any LockListener or perform default action
        private boolean notifyRelock() {
            if (listener != null)
                return listener.onRelock(this);
            else
                return data.handle != null && data.handle.getStatus() == Status.READY;
        }
        
        // Lock, using the correct read or write lock
        private void lock() {
            if (writeLock)
                data.lock.writeLock().lock();
            else
                data.lock.readLock().lock();
        }
        
        // Unlock, using the correct read or write lock
        private void unlock() {
            if (writeLock)
                data.lock.writeLock().unlock();
            else
                data.lock.readLock().unlock();
        }

        @Override
        public int compareTo(LockToken<?> o) {
            int id1 = resource.getId();
            int id2 = o.resource.getId();
            
            if (id1 == id2) {
                // Order consistently based on lock type,
                // and have the write lock come first so that the exclusive lock
                // is the outer lock
                if (writeLock && !o.writeLock)
                    return -1;
                else if (!writeLock && o.writeLock)
                    return 1;
                else
                    return 0;
            } else {
                // Compare by unique id
                return id1 - id2;
            }
        }
    }

    /*
     * Internal runner that monitors a ReferenceQueue to dispose of
     * ResourceHandles after their owning Resources have been collected.
     */
    private class WeakReferenceMonitor implements Runnable {
        @Override
        public void run() {
            while(!lifecycleManager.isStopped()) {
                try {
                    // We can't lock on the ResourceData anymore because its
                    // owning Resource has been collected. This is fine, though because
                    // this is the only thread that will be capable of operating on these
                    // orphaned wrappers.
                    ResourceData<?> data = (ResourceData<?>) collectedResources.remove();
                    
                    if (data.handle != null) {
                        // Don't block on this, we just need it to be disposed of in the future
                        contextManager.queue(new DisposeOrphanedHandleTask(data), "resource");
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
        private final ResourceData<?> data;
        
        public DisposeOrphanedHandleTask(ResourceData<?> data) {
            this.data = data;
        }
        
        @Override
        public Void call() throws Exception {
            OpenGLContext context = contextManager.ensureContext();
            
            // We don't need to worry about the disposable property at this point,
            // if the Resource has been GC'ed, we need the handle disposed of no matter what.
            if (data.handle != null)
                data.driver.dispose(context, data.handle);
            data.handle = null;
            return null;
        }
    }

    /*
     * Internal wrapper that collects a driver, ResourceHandle and weak
     * reference to a Resource.
     */
    private static class ResourceData<R extends Resource> extends WeakReference<R> {
        final ResourceDriver<R> driver;
        final int resourceId;
        final ReentrantReadWriteLock lock;
        
        boolean disposable; // True if resource can be disposable
        ResourceHandle handle;
        
        public ResourceData(R resource, ResourceDriver<R> driver, ReferenceQueue<Resource> queue) {
            super(resource, queue);
            
            this.driver = driver;
            resourceId = resource.getId();
            lock = new ReentrantReadWriteLock();
            disposable = true;
            handle = null;
        }
    }
}
