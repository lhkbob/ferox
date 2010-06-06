package com.ferox.renderer.impl;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.renderer.RenderException;
import com.ferox.resource.DirtyState;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

/**
 * <p>
 * DefaultResourceManager is an implementation of ResourceManager that is likely
 * to be sufficient for many uses cases. It was designed to work hand-in-hand
 * with the DefaultRenderManager, although it should be possible to use a
 * different RenderManager with this resource manager. The
 * DefaultResourceManager manages two internal threads: one for automatically
 * disposing of garbage collected resources, and one for executing the update
 * and disposal tasks (and any tasks queued with
 * {@link #runOnResourceThread(Runnable)}. It will properly manage disconnected
 * resources when its resource context is set to null.
 * </p>
 * <p>
 * The DefaultResourceManager supports two modes of operation:
 * <ol>
 * <li>A Framework implementation has a Context that has its lifetime tied to
 * the Framework. All created surface's contexts can share resources with the
 * provided Context. The DefaultResourceManager is notified of this shared
 * context at the Framework's initialization and does not have it invalidated
 * until the manager is itself destroyed.</li>
 * <li>There is no shared Context to use, so the manager assumes there will only
 * be one active Context at a time. This means the Framework implementation must
 * restrict the number of surfaces created with a context to one at a time. This
 * is a fallback mode for systems without pbuffers and context sharing
 * capabilities.</li>
 * </ol>
 * </p>
 * <p>
 * In the first mode of operation, resources can be updated before any surface
 * is created and they can never be disconnected unless
 * {@link #setContext(Context)} is invoked inappropriately. In the second mode
 * of operation, resources will be disconnected when the single valid surface is
 * destroyed. It is important that {@link #setContext(Context)} not be invoked
 * out of order because the manager assumes that any resource data can be
 * discarded because the context has already been reclaimed by the system. If
 * this is not the case, memory leaks in the OpenGL drivers may occur.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class DefaultResourceManager implements ResourceManager {
    private static final long CLEANUP_WAKEUP_INTERVAL = 1000;
    private static final AtomicInteger threadCounter = new AtomicInteger(0);

    private final BlockingDeque<Sync<?>> pendingTasks;
    private final ConcurrentMap<Integer, ResourceData> resourceData;
    private final Map<Class<? extends Resource>, ResourceDriver> drivers;

    private final Object contextLock = new Object(); // used to guard access to resourceContext
    private final Object updateLock = new Object(); // used within getHandle() to wait for an update
    private volatile ReentrantReadWriteLock frameworkLock;

    private final AtomicBoolean destroyed;
    private volatile Context resourceContext;
    
    private final Thread taskThread;
    private final Thread disposalScheduler;

    /**
     * Create a DefaultResourceManager that uses the specified Map as its source
     * of ResourceDrivers for performing the low-level resource operations.
     * Initially it has no valid resource Context, this must be assigned by the
     * Framework during construction if using a shared context, or will be
     * automatically assigned when the first surface is created.
     * 
     * @param drivers The drivers to use, must not contain null elements
     * @throws NullPointerException if drivers is null
     */
    public DefaultResourceManager(Map<Class<? extends Resource>, ResourceDriver> drivers) {
        if (drivers == null)
            throw new NullPointerException("Resource driver map cannot be null");
        this.drivers = new HashMap<Class<? extends Resource>, ResourceDriver>(drivers);

        destroyed = new AtomicBoolean(false);
        resourceContext = null;
        resourceData = new ConcurrentHashMap<Integer, ResourceData>();
        
        pendingTasks = new LinkedBlockingDeque<Sync<?>>();

        int id = threadCounter.incrementAndGet();
        taskThread = new Thread(new ResourceWorker());
        taskThread.setName("resource-worker " + id);
        taskThread.setDaemon(true);
        
        disposalScheduler = new Thread(new DisposalScheduler());
        disposalScheduler.setName("disposal-scheduler " + id);
        disposalScheduler.setDaemon(true);
    }
    
    @Override
    public void lock(Resource resource) {
        if (resource == null)
            throw new NullPointerException("Cannot lock a null Resource");
        
        synchronized(resource) {
            ResourceData data = getResourceData(resource);
            if (data == null)
                throw new UnsupportedOperationException("umm");
            data.locked = true;
        }
    }
    
    @Override
    public void unlock(Resource resource) {
        if (resource == null)
            throw new NullPointerException("Cannot unlock a null Resource");
        
        synchronized(resource) {
            ResourceData data = getResourceData(resource);
            if (data != null)
                data.locked = false;
        }
    }
    
    @Override
    public void initialize(ReentrantReadWriteLock lock) {
        if (lock == null)
            throw new NullPointerException("Lock cannot be null");
        if (destroyed.get() || frameworkLock != null)
            throw new IllegalStateException("Cannot reinitialize ResourceManager");
        
        frameworkLock = lock;

        // start everything
        taskThread.start();
        disposalScheduler.start();
    }
    
    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true))
            return; // already destroyed
        
        try {
            taskThread.interrupt();
            disposalScheduler.interrupt();
            
            if (Thread.currentThread() != taskThread)
                taskThread.join();
            if (Thread.currentThread() != disposalScheduler)
                disposalScheduler.join();
        } catch(InterruptedException ie) {
            // do nothing
        }
        
        synchronized(contextLock) {
            resourceContext = null;
            disconnect();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResourceHandle getHandle(Resource resource) {
        if (resource == null)
            throw new NullPointerException("Cannot retreive handle for null Resource");
        if (resourceContext == null)
            return null;
        
        Sync<Status> forceSync = null;
        boolean isHandleLocked = false;
        synchronized(resource) {
            ResourceData rd = getResourceData(resource);
            DirtyState<?> ds = resource.getDirtyState();

            if (ds != null || rd == null || rd.sync != null || !rd.hasContextData()) {
                // something is going on, so check everything
                if (rd == null) {
                    // do a new update
                    ResourceDriver driver = getDriver(resource);
                    if (driver == null)
                        return null;
                    
                    rd = new ResourceData(resource, driver);
                    setResourceData(resource, rd);

                    forceSync = new Sync<Status>(new UpdateTask(resource, null));
                } else if (rd.sync != null) {
                    if (rd.sync.getTask() instanceof UpdateTask) {
                        // merge the dirty states
                        UpdateTask ut = (UpdateTask) rd.sync.getTask();
                        if (ds != null) // only merge if we have to
                            ut.dirtyState = merge(ds, ut.dirtyState);

                        pendingTasks.remove(rd.sync);
                        forceSync = (Sync<Status>) rd.sync; // re-use old queued sync
                    } else if (rd.sync.getTask() instanceof DisposeTask) {
                        // cleanup is still pending, so return null so we don't get
                        // a destroyed resource bound in the rendering thread
                        return null;
                    }
                } else if (ds != null) {
                    // perform a plain update using the new dirty state
                    UpdateTask ut = new UpdateTask(resource, ds);
                    forceSync = new Sync<Status>(ut);
                }

                if (forceSync != null) {
                    if (Thread.currentThread() == taskThread) {
                        // this can only happen if a resource driver invokes getHandle()
                        // while processing a different resource, so we should have a valid 
                        // context to run on
                        forceSync.run();
                        forceSync = null; // null this so we don't wait later on
                    } else
                        pendingTasks.addFirst(forceSync);
                }
            }
            
            // return now if possible to avoid double synchronization
            if (forceSync == null)
                return (rd.handle.getStatus() == Status.READY ? rd.handle : null);
            isHandleLocked = rd.handle != null && rd.handle.isLocked();
        }
        
        if (forceSync != null && !isHandleLocked) {
            // block until update is completed, the resource is released so it can continue
            // if the calling thread has locked the handle, however, we can't wait
            // because the resource thread won't be able to proceed -> deadlock
            synchronized(updateLock) {
                try {
                    while(resourceContext != null && !forceSync.isDone())
                        updateLock.wait();
                } catch (InterruptedException ie) {
                    throw new RenderInterruptedException("Interrupted while waiting for Resource", ie);
                }
            }
        }

        synchronized(resource) {
            // re-synchronize and re-fetch now that the update has completed
            // this is slower but shouldn't happen very often
            ResourceData rd = getResourceData(resource);
            return (resourceContext != null && rd.handle.getStatus() == Status.READY ? rd.handle : null);
        }
    }

    @Override
    public Status getStatus(Resource resource) {
        if (destroyed.get())
            return Status.DISPOSED;
        
        synchronized(resource) {
            ResourceDriver d = getDriver(resource);
            if (d == null)
                return Status.UNSUPPORTED;

            ResourceData rd = getResourceData(resource);
            if (rd != null) {
                if (rd.handle == null) {
                    // a null handle is flag for disconnected within this resource manager,
                    // whereas a null resource-data signals a disposed resource
                    return Status.DISCONNECTED;
                } else {
                    switch(rd.handle.getStatus()) {
                    case DISCONNECTED: return Status.DISCONNECTED;
                    case DISPOSED: return Status.DISPOSED;
                    default:
                        return rd.handle.getStatus();
                    }
                }
            } else 
                return Status.DISPOSED;
        }
    }

    @Override
    public String getStatusMessage(Resource resource) {
        if (destroyed.get())
            return "Framework is destroyed";
        
        synchronized(resource) {
            ResourceDriver d = getDriver(resource);
            if (d == null)
                return "Resource of type " + resource.getClass().getSimpleName() + " is unsupported";

            ResourceData rd = getResourceData(resource);
            if (rd != null) {
                if (rd.handle == null) {
                    // a null handle is flag for disconnected within this resource manager,
                    // whereas a null resource-data signals a disposed resource
                    return "Disconnected, no usable context to use for resource";
                } else {
                    switch(rd.handle.getStatus()) {
                    case DISCONNECTED: return "Disconnected, no usable context to use for resource";
                    case DISPOSED: return "Resource is disposed of";
                    default:
                        return rd.handle.getStatusMessage();
                    }
                }
            } else 
                return "Resource is disposed of";
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<Void> scheduleDispose(Resource resource) {
        if (destroyed.get()) // shouldn't happen
            throw new RenderException("Cannot retrieve ResourceHandle from a destroyed ResourceManager");

        synchronized(resource) {
            ResourceDriver d = getDriver(resource);
            if (d == null)
                return new CompletedFuture<Void>(null); // if it's not supported it's disposed of already
            
            ResourceData rd = getResourceData(resource);
            if (rd == null || !rd.hasContextData()) {
                // already cleaned up
                setResourceData(resource, null);
                return new CompletedFuture<Void>(null); 
            }
            
            if (rd.locked)
                throw new IllegalArgumentException("Cannot dispose of a locked Resource");
            
            // handle cases where the resource is in the process of disposal or updating
            Callable<?> task = (rd.sync == null ? null : rd.sync.getTask());
            if (task instanceof DisposeTask) {
                // re-use the existing sync
                return new FutureSync<Void>((Sync<Void>) rd.sync);
            } else if (task instanceof UpdateTask) {
                // must cancel the update
                rd.sync.cancel(false);
                pendingTasks.remove(rd.sync);
            }

            // new disposal task
            rd.sync = new Sync<Void>(new DisposeTask(resource));
            pendingTasks.add(rd.sync);
            
            return new FutureSync<Void>((Sync<Void>) rd.sync);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<Status> scheduleUpdate(Resource resource, boolean forceFullUpdate) {
        if (destroyed.get())
            throw new RenderException("Cannot retrieve ResourceHandle from a destroyed ResourceManager");
        
        synchronized(resource) {
            // handle cases where no action is necessary
            ResourceDriver d = getDriver(resource);
            if (d == null)
                return new CompletedFuture<Status>(Status.UNSUPPORTED);
            
            ResourceData rd = getResourceData(resource);
            if (rd == null) {
                // first time resource has been seen
                rd = new ResourceData(resource, d);
                setResourceData(resource, rd);
            }
            
            // if we don't have a context, we can't actually update it yet
            // so flag it as DISCONNECTED and return.
            // it is theoretically possible that the context is nulled after
            // enqueuing the task, but this is handled by the worker thread
            if (resourceContext == null)
                return new CompletedFuture<Status>(Status.DISCONNECTED);
            
            // fetch and clear the dirty state for the resource
            DirtyState<?> dirtyDescriptor = resource.getDirtyState();
            if (forceFullUpdate)
                dirtyDescriptor = null;
            
            // handle cases where an existing scheduled event is in the way
            Callable<?> task = (rd.sync == null ? null : rd.sync.getTask());
            if (task instanceof UpdateTask) {
                // merge dirty states
                UpdateTask ut = (UpdateTask) task;
                ut.dirtyState = merge(dirtyDescriptor, ut.dirtyState);

                return new FutureSync<Status>((Sync<Status>) rd.sync);
            } else if (task instanceof DisposeTask) {
                // cancel it
                rd.sync.cancel(false);
                pendingTasks.remove(rd.sync);
            }
            
            // new update task to schedule
            Sync<Status> sync = new Sync<Status>(new UpdateTask(resource, dirtyDescriptor));
            rd.sync = sync;
            pendingTasks.add(sync);
            
            return new FutureSync<Status>(sync);
        }
    }
    
    /* Internal utility methods */
    
    @Override
    public void runOnResourceThread(Runnable run) {
        if (resourceContext == null)
            throw new RenderException("ResourceManager has no usable context");
        
        Sync<Void> sync = new Sync<Void>(new RunnableTask(run));
        pendingTasks.addFirst(sync);
        
        try {
            // now block until this is completed
            sync.get();
        } catch (InterruptedException e) {
            throw new RenderInterruptedException("Render on resource thread interrupted", e);
        } catch (ExecutionException e) {
            throw new RenderException("Exception while rendering on resource thread", e);
        }
    }
    
    @Override
    public Context getContext() {
        return resourceContext;
    }
    
    @Override
    public void setContext(Context context) {
        synchronized(contextLock) {
            Context old = resourceContext;
            resourceContext = context;
            
            if (context == null && old != null) {
                // it is important that disconnect be called
                // after updating the resourceContext so that
                // any blocked calls to getHandle() will wake up
                disconnect();
            }
        }
    }
    
    /*
     * Invoked when resourceContext has been invalidated or destroyed.
     */
    private void disconnect() {
        synchronized(updateLock) {
            // wake up any blocked getHandle() requests first
            updateLock.notifyAll();
        }
        
        ResourceData rd;
        Resource r;
        Iterator<ResourceData> it = resourceData.values().iterator();
        while(it.hasNext()) {
            rd = it.next();
            r = rd.resource.get();
            if (r != null) {
                synchronized(r) {
                    if (rd.hasContextData()) {
                        rd.handle.setStatus(Status.DISCONNECTED);
                        rd.handle = null;
                    }
                }
            } else
                it.remove();
        }
    }

    /*
     * Merge newDs and oldDs into a new DirtyState. It is assumed that newDs
     * represents a new state of dirtiness than oldDs. We do not need to
     * synchronize this explicitly because it should only be invoked within
     * a lock for the Resource that owned both dirty state instances.
     */
    @SuppressWarnings("unchecked")
    private DirtyState<?> merge(DirtyState<?> newDs, DirtyState<?> oldDs) {
        if (newDs == null || oldDs == null)
            return null;
        return ((DirtyState) newDs).merge((DirtyState) oldDs); // lame generics
    }
    
    private ResourceDriver getDriver(Resource r) {
        // we do not need to synchronize this method because
        // the drivers map is read-only and final
        Class<?> clazz = r.getClass();
        while(clazz != null && Resource.class.isAssignableFrom(clazz)) {
            ResourceDriver d = drivers.get(clazz);
            if (d != null)
                return d;
            clazz = clazz.getSuperclass();
        }
        return null;
    }
    
    private ResourceData getResourceData(Resource r) {
        Integer key = Integer.valueOf(r.getId());
        return resourceData.get(key);
    }
    
    private void setResourceData(Resource r, ResourceData rd) {
        Integer key = Integer.valueOf(r.getId());
        if (rd == null)
            resourceData.remove(key);
        else
            resourceData.put(key, rd);
    }
    
    /* Inner classes that provide much grunt work */
    
    private class ResourceWorker implements Runnable {
        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            while(!destroyed.get()) {
                try {
                    Sync<?> sync = pendingTasks.take();
                    
                    frameworkLock.readLock().lock();
                    try {
                        synchronized(contextLock) {
                            if (resourceContext != null) {
                                // we have a context so we can actually run the sync
                                resourceContext.runWithLock(sync);
                            } else {
                                // no context, override callable's return value without running it
                                if (sync.getTask() instanceof UpdateTask)
                                    ((Sync<Status>) sync).set(Status.DISCONNECTED);
                                else
                                    sync.set(null);
                            }
                        }
                    } finally {
                        frameworkLock.readLock().unlock();
                    }

                    synchronized(updateLock) {
                        // wake-up any thread blocking on getHandle()
                        updateLock.notifyAll();
                    }
                } catch(InterruptedException ie) {
                    // do nothing
                }
            }
        }
    }
    
    private class DisposalScheduler implements Runnable {
        @Override
        public void run() {
            while(!destroyed.get()) {
                ResourceData rd;
                Iterator<ResourceData> it = resourceData.values().iterator();
                while(it.hasNext()) {
                    rd = it.next();
                    if (rd.resource.get() == null) {
                        if (rd.hasContextData())
                            pendingTasks.add(new Sync<Void>(new HandleDisposeTask(rd)));
                        it.remove();
                    }
                }
                    
                try {
                    // sleep and check periodically
                    Thread.sleep(CLEANUP_WAKEUP_INTERVAL);
                } catch(InterruptedException ie) {
                    // do nothing
                }
            }
        }
    }
    
    private class UpdateTask implements Callable<Status> {
        private final Resource toUpdate;
        
        private DirtyState<?> dirtyState;
        
        public UpdateTask(Resource toUpdate, DirtyState<?> dirtyState) {
            this.toUpdate = toUpdate;
            this.dirtyState = dirtyState;
        }
        
        @Override
        public Status call() throws Exception {
            synchronized(toUpdate) {
                ResourceData rd = getResourceData(toUpdate);
                ResourceHandle handle = rd.handle;
                try {
                    if (!rd.hasContextData()) {
                        // initialize resource, we don't lock on the handle because
                        // we have to create the handle
                        handle = rd.driver.init(toUpdate);
                        rd.handle = handle;
                    } else {
                        // perform an update
                        handle.lock();
                        try {
                            rd.driver.update(toUpdate, handle, dirtyState);
                        } catch(RuntimeException re) {
                            // note that we cannot do a similar ERROR status if
                            // an exception is thrown during initialization because
                            // we have no handle to modify
                            handle.setStatus(Status.ERROR);
                            handle.setStatusMessage("Exception thrown during update");
                            throw re;
                        } finally {
                            handle.unlock();
                        }
                    }
                } finally {
                    // must be certain to clear clear the sync
                    if (rd.sync != null && rd.sync.getTask() == this)
                        rd.sync = null;
                }
                return handle.getStatus();
            }
        }
    }
    
    private class DisposeTask implements Callable<Void> {
        private final Resource toDispose;
        
        public DisposeTask(Resource toDispose) {
            this.toDispose = toDispose;
        }
        
        @Override
        public Void call() throws Exception {
            synchronized(toDispose) {
                ResourceData rd = getResourceData(toDispose);
                setResourceData(toDispose, null);

                try {
                    if (rd != null && rd.hasContextData()) {
                        // dispose resource
                        rd.handle.lock();
                        try {
                            rd.driver.dispose(rd.handle);
                            rd.handle.setStatus(Status.DISPOSED);
                        } finally {
                            rd.handle.unlock();
                        }
                    }
                } finally {
                    // must be certain to clear clear the sync
                    if (rd.sync != null && rd.sync.getTask() == this)
                        rd.sync = null;
                }
            }
            
            // dummy return value
            return null;
        }
    }
    
    /* These classes can be static */
    
    private static class HandleDisposeTask implements Callable<Void> {
        private final ResourceData data;
        
        public HandleDisposeTask(ResourceData rd) {
            data = rd;
        }
        
        @Override
        public Void call() throws Exception {
            if (data.hasContextData()) {
                ResourceHandle handle = data.handle;
                handle.lock();
                try {
                    data.driver.dispose(handle);
                    handle.setStatus(Status.DISPOSED);
                } finally {
                    handle.unlock();
                }
            }
            
            return null;
        }
    }
    
    private static class RunnableTask implements Callable<Void> {
        private final Runnable task;
        
        public RunnableTask(Runnable r) {
            task = r;
        }
        
        @Override
        public Void call() throws Exception {
            task.run();
            return null;
        }
    }
    
    private static class ResourceData {
        final ResourceDriver driver;
        final int id;
        final WeakReference<Resource> resource;

        /*
         * These fields are guarded by synchronizing on the Resource that is
         * paired with each ResourceData. All modifications of the RD must occur
         * within such an implicit lock.
         */
        boolean locked; // referring to disposal
        ResourceHandle handle;
        Sync<?> sync;
        
        public ResourceData(Resource r, ResourceDriver driver) {
            this.driver = driver;

            resource = new WeakReference<Resource>(r);
            id = r.getId();
        }
        
        public boolean hasContextData() {
            // we don't need to check for Status.UNSUPPORTED because
            // in those situations, we should never have created a ResourceData
            return handle != null && handle.getStatus() != Status.DISCONNECTED && handle.getStatus() != Status.DISPOSED;
        }
    }
}
