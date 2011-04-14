package com.ferox.renderer.impl2;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Resource.UpdatePolicy;

public class ResourceManager {
    // These are final after "initialize()" is called
    private LifeCycleManager lifecycleManager;
    private ContextManager contextManager;
    private Thread garbageCollector;
    
    private final ReferenceQueue<Resource> collectedResources;
    private final ConcurrentMap<Integer, ResourceData<?>> resources;
    private final Map<Class<? extends Resource>, ResourceDriver<?>> drivers;

    public ResourceManager(ResourceDriver<?>... drivers) {
        collectedResources = new ReferenceQueue<Resource>();
        resources = new ConcurrentHashMap<Integer, ResourceData<?>>();
        
        Map<Class<? extends Resource>, ResourceDriver<?>> tmpDrivers = new HashMap<Class<? extends Resource>, ResourceDriver<?>>();
        if (drivers != null) {
            // Build up the map of drivers based on the input array
            for (ResourceDriver<?> driver: drivers) {
                tmpDrivers.put(driver.getResourceType(), driver);
            }
        }
        
        // We don't modify the map after the constructor so that it is thread-safe,
        // but wrap it in an immutable collection to make sure.
        this.drivers = Collections.unmodifiableMap(tmpDrivers);
    }
    
    public void initialize(LifeCycleManager lifecycle, ContextManager contextManager) {
        if (lifecycle == null)
            throw new NullPointerException("LifeCycleManager cannot be null");
        if (contextManager == null)
            throw new NullPointerException("ContextManager cannot be null");
        
        // We are assuming that we're in the right threading situation, so this is safe.
        // If this is called outside of the manager's lock then all bets are off, but that's their fault.
        if (lifecycle.getStatus() != com.ferox.renderer.impl2.LifeCycleManager.Status.STARTING)
            throw new IllegalStateException("LifeCycleManager must have status STARTING, not: " + lifecycle.getStatus());
        
        // Do a simple exclusive lock to check for double-init attempts. This won't hurt threading
        // since we should already be in lifecycle's write lock.
        synchronized(this) {
            if (lifecycleManager != null)
                throw new IllegalStateException("ResourceManager already initialized");
            lifecycleManager = lifecycle;
        }
        
        this.contextManager = contextManager;
        garbageCollector = new Thread(lifecycleManager.getManagedThreadGroup(), new WeakReferenceMonitor(), "resource-disposer");
        lifecycleManager.startManagedThread(garbageCollector);
    }

    // TODO doc that this must only be called within r's lock
    public ResourceHandle getHandle(OpenGLContextAdapter context, Resource r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        if (context == null)
            throw new NullPointerException("Context cannot be null");

        // This might not be the most efficient way, but the overhead here
        // should be insignificant compared to the change checking implemented
        // by each driver. Plus, if they want performance, they should set the policy
        // to MANUAL.
        if (r.getUpdatePolicy() == UpdatePolicy.ON_DEMAND)
            update(context, r);
        
        ResourceData<?> data = getResourceData(r, false);
        // Only return a handle if it has a READY status
        if (data == null || data.handle == null || data.handle.getStatus() != Status.READY)
            return null;
        else
            return data.handle;
    }
    
    public <R extends Resource> Status update(OpenGLContextAdapter context, R r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        if (context == null)
            throw new NullPointerException("Context cannot be null");
        
        r.getLock().lock();
        try {
            ResourceData<R> data = getResourceData(r, true);
            if (data == null)
                return Status.UNSUPPORTED;
            
            // Check for a handle, if we have one we can skip the init()
            if (data.handle == null) {
                // No such luck, do a full init (but we don't need an update later)
                data.handle = data.driver.init(context, r);
                return data.handle.getStatus();
            } else {
                // Just do a regular update
                return data.driver.update(context, r, data.handle);
            }
        } finally {
            r.getLock().unlock();
        }
    }
    
    public <R extends Resource> void dispose(OpenGLContextAdapter context, R r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        if (context == null)
            throw new NullPointerException("Context cannot be null");
        
        r.getLock().lock();
        try {
            ResourceData<R> data = getResourceData(r, false);
            if (data != null) {
                // Check if it's disposable
                if (!data.disposable)
                    throw new IllegalStateException("Resource is in use by a Surface and cannot be destroyed");
                
                if (data.handle != null)
                    data.driver.dispose(context, data.handle);
            }
            resources.remove(r.getId());
        } finally {
            r.getLock().unlock();
        }
    }
    
    public <R extends Resource> void reset(R r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        
        r.getLock().lock();
        try {
            ResourceData<R> data = getResourceData(r, false);
            if (data != null && data.handle != null)
                data.driver.reset(r, data.handle);
        } finally {
            r.getLock().unlock();
        }
    }
    
    public <R extends Resource> void setDisposable(R r, boolean disposable) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        
        r.getLock().lock();
        try {
            ResourceData<R> data = getResourceData(r, true);
            if (data != null)
                data.disposable = disposable;
        } finally {
            r.getLock().unlock();
        }
    }
    
    public String getStatusMessage(Resource r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        
        // Special case for if we're shutting down
        if (lifecycleManager.isStopped())
            return null;
        
        r.getLock().lock();
        try {
            ResourceData<?> data = getResourceData(r, false);
            if (data != null) {
                // This is a resource we know about so just grab the status message
                return (data.handle != null ? data.handle.getStatusMessage() : "");
            } else {
                // This is either a disposed resource, or an unsupported resource,
                // but all of that is encoded in the Status, the message doesn't matter
                return "";
            }
        } finally {
            r.getLock().unlock();
        }
    }
    
    public Status getStatus(Resource r) {
        if (r == null)
            throw new NullPointerException("Resource cannot be null");
        
        // Special case for if we're shutting down
        if (lifecycleManager.isStopped())
            return Status.DISPOSED;
        
        r.getLock().lock();
        try {
            ResourceData<?> data = getResourceData(r, false);
            if (data != null) {
                // This is a resource we know about so just grab the status
                return (data.handle != null ? data.handle.getStatus() : Status.DISPOSED);
            } else {
                // This is either disposed or unsupported, so we have to check the driver.
                // If there is a driver, then it is supported but disposed
                return (getDriver(r) == null ? Status.UNSUPPORTED : Status.DISPOSED);
            }
        } finally {
            r.getLock().unlock();
        }
    }
    
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
            OpenGLContextAdapter context = contextManager.ensureContext();
            
            // We don't need to worry about the disposable property at this point,
            // if the Resource has been GC'ed, we need the handle disposed of no matter what.
            if (data.handle != null)
                data.driver.dispose(context, data.handle);
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
        
        boolean disposable; // True if resource can be disposable
        ResourceHandle handle;
        
        public ResourceData(R resource, ResourceDriver<R> driver, ReferenceQueue<Resource> queue) {
            super(resource, queue);
            
            this.driver = driver;
            resourceId = resource.getId();
            disposable = true;
            handle = null;
        }
    }
}
