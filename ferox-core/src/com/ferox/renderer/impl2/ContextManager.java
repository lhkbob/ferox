package com.ferox.renderer.impl2;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.ferox.renderer.Framework;
import com.ferox.renderer.impl2.LifeCycleManager.Status;

/**
 * <p>
 * ContextManager is the manager that handles the internal threads that run code
 * within a valid OpenGL context. It has logic to keep a context current after
 * task has completed, meaning that it may not need to have expensive
 * makeCurrent()/release() cycles in the common case where a single surface is
 * rendered into repeatedly.
 * </p>
 * <p>
 * A newly constructed ContextManager is not ready to use until its
 * {@link #initialize(LifeCycleManager)} is called. The ContextManager is
 * expected to live within the life cycle of its owning Framework (as enforced
 * by the LifeCycleManager).
 * </p>
 * 
 * @author Michael Ludwig
 */
public class ContextManager {
    private LifeCycleManager lifecycleManager; // "final" after initialize() is called
    
    private final SurfaceFactory surfaceFactory;
    private final OpenGLContextAdapter sharedContext;
    
    private final ConcurrentMap<String, ContextThread> groupAffinity; // A group will be placed in this map at most once
    private final ConcurrentMap<AbstractSurface, ContextThread> persistentSurfaceLocks;
    
    private final ContextThread[] threads;
    private final AtomicInteger nextThread;

    /**
     * <p>
     * Create a new ContextManager that uses the given SurfaceFactory to create
     * offscreen contexts to use when a Surface's context is not available for
     * OpenGL work. <tt>sharedContext</tt> must be the Framework's shared
     * context that enables resource sharing across all created contexts. If
     * <tt>numThreads</tt> is less than or equal to 0, the number of created
     * threadsd will be equal to the number of available processors.
     * </p>
     * <p>
     * The created ContextManager cannot be used until it has been
     * {@link #initialize(LifeCycleManager) initialized}.
     * </p>
     * 
     * @see #initialize(LifeCycleManager)
     * @param contextFactory The SurfaceFactory
     * @param sharedContext The OpenGL context that facilitates resource sharing
     * @param numThreads The number of internal threads to create for running
     *            queued tasks
     * @throws NullPointerException if contextFactory or sharedContext are null
     */
    public ContextManager(SurfaceFactory contextFactory, OpenGLContextAdapter sharedContext, int numThreads) {
        if (contextFactory == null)
            throw new NullPointerException("SurfaceFactory cannot be null");
        if (sharedContext == null)
            throw new NullPointerException("Shared context cannot be null");
        
        if (numThreads <= 0)
            numThreads = Runtime.getRuntime().availableProcessors();
        
        this.sharedContext = sharedContext;
        surfaceFactory = contextFactory;
        
        groupAffinity = new ConcurrentHashMap<String, ContextManager.ContextThread>();
        persistentSurfaceLocks = new ConcurrentHashMap<AbstractSurface, ContextManager.ContextThread>();
        
        threads = new ContextThread[numThreads];
        nextThread = new AtomicInteger(0);
    }

    /**
     * <p>
     * Complete the initialization of this ContextManager and start up all
     * required internal threads that process all queued tasks. This method ties
     * the ContextManager to the life cycle enforced by the given
     * LifeCycleManager. It is required that this method is called by the
     * ContextManager's owner in the initialization Runnable passed to
     * {@link LifeCycleManager#start(Runnable)}.
     * </p>
     * <p>
     * The ContextManager will automatically terminate its threads when it
     * detects that the LifeCycleManager is being shutdown. All internal threads
     * are managed threads so the final destruction code passed to
     * {@link LifeCycleManager#destroy(Runnable)} will not run until the
     * ContextManager stop processing tasks.
     * </p>
     * <p>
     * The ContextManager cannot be initialized more than once. It is illegal to
     * use a LifeCycleManager that has a status other than STARTING (i.e. within
     * the scope of its initialize() method).
     * </p>
     * 
     * @param lifecycle The LifeCycleManager that controls when the
     *            ContextManager ends
     * @throws NullPointerException if lifecycle is null
     * @throws IllegalStateException if lifecycle doesn't have a status of
     *             STARTING, or if the ContextManager has already been
     *             initialized
     */
    public void initialize(LifeCycleManager lifecycle) {
        if (lifecycle == null)
            throw new NullPointerException("LifeCycleManager cannot be null");
        
        // We are assuming that we're in the right threading situation, so this is safe.
        // If this is called outside of the manager's lock then all bets are off, but that's their fault.
        if (lifecycle.getStatus() != Status.STARTING)
            throw new IllegalStateException("LifeCycleManager must have status STARTING, not: " + lifecycle.getStatus());
        
        // Do a simple exclusive lock to check for double-init attempts. This won't hurt threading
        // since we should already be in lifecycle's write lock.
        synchronized(this) {
            if (lifecycleManager != null)
                throw new IllegalStateException("ContextManager already initialized");
            lifecycleManager = lifecycle;
        }
        
        // Must use thread group that is a child of the managed group
        ThreadGroup group = new ThreadGroup(lifecycle.getManagedThreadGroup(), "ContextManager Tasks");
        for (int i = 0; i < threads.length; i++) {
            String name = "task-thread-" + i;
            if (i == 0) {
                // First task thread will use the shared context as its shadow context
                threads[i] = new ContextThread(group, name, sharedContext);
            } else {
                // No shadow context until its needed
                threads[i] = new ContextThread(group, name);
            }
            
            // Must start a managed thread, but we can't use the convenience method
            // because the ContextManager uses a special subclass of Thread
            lifecycle.startManagedThread(threads[i]);
        }
    }

    /**
     * <p>
     * Queue the given Callable task to one of the inner threads managed by this
     * ContextManager. The provided group has the same meaning and implications
     * as in {@link Framework#queue(com.ferox.renderer.Task, String)}. The
     * ContextManager is allowed to run tasks with the same group on different
     * threads. The only restriction is that tasks in the same group run in the
     * chronological order they were queued (which can be non-deterministic if
     * multiple threads queue to the same group).
     * </p>
     * <p>
     * If the LifeCycleManager controlling this ContextManager is being
     * shutdown, or has been shutdown, the returned Future is not queued and is
     * preemptively cancelled. It will never be null.
     * </p>
     * 
     * @param <T> The type of data returned by task
     * @param task The task to run on an internal thread
     * @param group The group that controls execution flow
     * @return A Future linked to the queued task, will be cancelled if the
     *         ContextManager has been shutdown or is shutting down
     * @throws NullPointerException if task or group are null
     */
    public <T> Future<T> queue(Callable<T> task, String group) {
        if (task == null)
            throw new NullPointerException("Task cannot be null");
        if (group == null)
            throw new NullPointerException("A null group is not allowed");
        
        // FIXME: this thread distribution logic to preserve within-group ordering
        // works but is naive. ideally we should have something that can switch
        // across threads if there is no other task in a group taking precedent.
        // - the current approach could accidentally put all groups onto a single thread permanently
        //   if things are queued in a bad order
        ContextThread thread = groupAffinity.get(group);
        if (thread == null) {
            // must assign the group to a new thread, using a round-robin policy through the threads
            thread = threads[nextThread.getAndIncrement() % threads.length];
            ContextThread oldThread = groupAffinity.putIfAbsent(group, thread);
            if (oldThread != null) {
                // Another thread tried to queue for the same group, 
                // so use that thread instead (this means that nextThread was
                // advanced an extra count, but that shouldn't be too bad).
                thread = oldThread;
            }
        }
        
        return queue(new Sync<T>(task), thread, false);
    }

    /**
     * Return whether or not the current Thread is a thread managed by this
     * ContextManager and is capable of having OpenGL contexts current on it.
     * 
     * @return True if the calling Thread is an inner thread managed by this
     *         ContextManager
     */
    public boolean isContextThread() {
        return Thread.currentThread() instanceof ContextThread;
    }

    /**
     * Unlock the given surface from the current Thread. If the surface is not
     * locked on this thread, no action is performed. This can only be called
     * while on a context thread. If the Surface is currently active, it will be
     * deactivated. If the Surface has its context in use, the context will be
     * released. This may cause a second surface to be deactivated in the
     * process (i.e. an FBO surface piggybacking on an OnscreenSurface that is
     * being released).
     * 
     * @param surface The surface to unlock
     * @throws NullPointerException if the surface is null
     * @throws IllegalStateException if {@link #isContextThread()} returns false
     */
    public void forceRelease(AbstractSurface surface) {
        if (surface == null)
            throw new NullPointerException("Surface cannot be null");
        Thread current = Thread.currentThread();
        if (current instanceof ContextThread) {
            // Delegate to the thread implementation
            ((ContextThread) current).unlock(surface);
        } else {
            // Must only call on the ContextThread
            throw new IllegalStateException("Cannot call unlock() on this thread");
        }
    }

    /**
     * <p>
     * Acquire the exclusive lock to the given AbstractSurface for the current
     * thread. This should be used instead of simply
     * <code>surface.getLock().lock()</code> because the ContextManager's
     * threads can hold a surface's lock across multiple frames to avoid the
     * cost of constantly switching OpenGL contexts.
     * </p>
     * <p>
     * This will negotiate the unlocking of a surface with any internal threads
     * that acquire the lock first. The owning thread is allowed to finish its
     * currently running task before it unlocks the surface.
     * </p>
     * 
     * @param surface The surface to lock
     * @throws NullPointerException if surface is null
     */
    public void lock(AbstractSurface surface) {
        if (surface == null)
            throw new NullPointerException("Surface cannot be null");
        
        // This method is the trickiest part of the lock management used in ContextManager.
        // Because each thread will hold onto a context-providing surface after a task has finished,
        // we need a way to request an unlock.  If we can't get the lock right away,
        // this method sends an unlock task to the lock owner. This is safe for a number of reasons:
        //   - This won't end until the calling thread has the lock, so that's a good guarantee
        //   - If someone else takes over ownership before the caller, a new request is sent to the new owner
        //   - The old owners ignore the unlock request if the requested surface isn't locked by them
        //   - Deadlocks should not occur.  If a thread is waiting for a lock on a context-surface,
        //     it will be the outer lock so the thread will have no other locks.
        //     If a thread is waiting on an fbo-surface, it will be an inner lock. Any thread that might
        //     want the outer context must then be getting an outer lock (so they have no locks that would
        //     prevent this thread from getting its inner lock).
        
        ReentrantLock lock = surface.getLock();
        while(!lock.tryLock()) {
            // We do a full lock if tryLock() fails and the context manager is shutting down.
            // This is set to true when tryLock fails, and the lifecycle is checked later.
            boolean forceFullLockMaybe = false;
            ContextThread owner = persistentSurfaceLocks.get(surface);
            if (owner != null) {
                // Send an unlock task to the thread and wait for the owner to unlock it
                queue(new Sync<Void>(new UnlockSurfaceCallable(surface)), owner, true);
                
                // We don't block on the returned Future because that is linked to when the 
                // other thread runs the unlock task. There is the chance that the surface
                // gets unlocked within a task, so we do a timed lock.
                // - If the timed lock fails to get it, we might queue multiple unlock tasks
                //   to the same thread. This just means some will get ignored after the first goes through.

                try {
                    if (lock.tryLock(5, TimeUnit.MILLISECONDS)) {
                        // Got the lock so we can break out of the loop
                        break;
                    } else {
                        // Couldn't get the lock, so flag that we might need to do a regular lock
                        forceFullLockMaybe = true;
                    }
                } catch(InterruptedException ie) {
                    // We were interrupted, so we follow the same logic as if a timeout occurred
                    forceFullLockMaybe = true;
                }
            }
            
            if (forceFullLockMaybe && lifecycleManager.isStopped()) {
                // Looping won't work anymore so just block and things will work out
                lock.lock();
                break;
            }
        }
        
        // If we made it this far, we locked with tryLock() or a lock() that was forced because
        // the context manager's lifecycle was ending, but we have the lock so we can return.
    }

    /*
     * Utility method to queue up the task within the context of the lifecycle
     * manager to make sure the queue action does not overlap with a status
     * change.
     */
    private <T> Future<T> queue(Sync<T> task, ContextThread thread, boolean atFront) {
        // Create the Future now so that it can be easily canceled later if need be
        Future<T> future = new FutureSync<T>(task);
        
        lifecycleManager.getLock().lock();
        try {
            if (!lifecycleManager.isStopped()) {
                if (atFront)
                    thread.tasks.addFirst(task);
                else
                    thread.tasks.add(task);
            } else {
                // LifecycleManager is shutting down or already has been, so cancel it
                future.cancel(false);
            }
            
            return future;
        } finally {
            lifecycleManager.getLock().unlock();
        }
    }

    /**
     * <p>
     * Activate the provided Surface on the current thread. The surface will be
     * locked for the duration that it is active, or until the end of running
     * Task. If the surface has its own OpenGLContextAdapter, that context is
     * made current on the thread. If it does not have a context, the surface
     * piggybacks on the last surface's context or on an internal offscreen
     * context.
     * </p>
     * <p>
     * An activated surface that has its own context will continue to be locked
     * after the task completes until a new surface is activated with its own
     * context, or the {@link #lock(AbstractSurface)} method is used. Holding
     * onto the lock across tasks allows the ContextManager to keep a context
     * current on a single thread without needing to release it between frames.
     * </p>
     * <p>
     * Passing in a null Surface will deactivate the currently active surface.
     * </p>
     * <p>
     * This can only be called from code running on an internal thread managed
     * by the ContextManager. The way to guarantee this is to use
     * {@link #queue(Callable, String)}. This can be used as the ContextManager
     * is shutting down. A destroyed surface will make the activation fail,
     * although any previously activate surface will have already been
     * deactivated. This assumes the surface is owned by the Framework owning
     * this ContextManager (if this assumption is broken, undefined results will
     * occur).
     * </p>
     * 
     * @param surface The AbstractSurface to activate
     * @param layer The layer to activate, will be passed directly to
     *            {@link AbstractSurface#onSurfaceActivate(int)}
     * @return The OpenGLContextAdapter that is current after this surface has
     *         been activated
     * @throws IllegalStateException if {@link #isContextThread()} returns false
     */
    public OpenGLContextAdapter setActiveSurface(AbstractSurface surface, int layer) {
        // Further validation is handled in the ContextThread after the lock is made
        
        Thread current = Thread.currentThread();
        if (current instanceof ContextThread) {
            // Delegate to the thread implementation
            return ((ContextThread) current).setActiveSurface(surface, layer);
        } else {
            // Should never happen, these methods should be restricted to the ContextThreads
            throw new IllegalThreadStateException("setActiveSurface() cannot be called on this Thread");
        }
    }

    /**
     * <p>
     * Ensure that there is a valid context current on this thread. If a surface
     * is already active, the returned context will be the same as what was
     * returned by {@link #setActiveSurface(AbstractSurface)}. If there is no
     * active surface, the context will be the context of the last activated
     * surface that had a context (assuming this thread still has the lock), or
     * an internal context that is offscreen.
     * </p>
     * <p>
     * Regardless, this will not return null and the returned context will
     * correctly share resources with the other contexts created by the
     * SurfaceFactory provided to this ContextManager during construction.
     * </p>
     * 
     * @return The current context
     * @throws IllegalStateException if {@link #isContextThread()} returns false
     */
    public OpenGLContextAdapter ensureContext() {
        Thread current = Thread.currentThread();
        if (current instanceof ContextThread) {
            // Delegate to the thread implementation
            return ((ContextThread) current).ensureContext();
        } else {
            // Should never happen, these methods should be restricted to the ContextThreads
            throw new IllegalThreadStateException("ensureContext() cannot be called on this Thread");
        }
    }

    /*
     * Internal thread class that manages the contexts and locks for the active
     * surface, allowing this to be used easily with a simple
     * HardwareAccessLayer implementation. The ContextThread uses a limitless
     * block queue to hold tasks. It can lock up to two surfaces at a time (one
     * for being "active" and one that provides a context). When no surface can
     * provide a context, it will lazily create a "shadow" context.
     */
    private class ContextThread extends Thread {
        private OpenGLContextAdapter shadowContext; // may be null
        private final boolean ownsShadowContext;

        private AbstractSurface activeSurface;
        private AbstractSurface contextProvider;
        
        private OpenGLContextAdapter currentContext;
        
        private final BlockingDeque<Sync<?>> tasks;
        
        public ContextThread(ThreadGroup group, String name) {
            this(group, name, null);
        }
        
        public ContextThread(ThreadGroup group, String name, OpenGLContextAdapter shadowContext) {
            super(group, name);
            this.shadowContext = shadowContext;
            ownsShadowContext = shadowContext == null;
            tasks = new LinkedBlockingDeque<Sync<?>>(10);
            
            setDaemon(true);
        }
        
        public OpenGLContextAdapter ensureContext() {
            if (currentContext == null) {
                // There is no contextProvider to piggy-back off of, so we we use the shadowContext
                // - The shadow context is unique to this thread, so we don't need to lock
                if (shadowContext == null)
                    shadowContext = surfaceFactory.createShadowContext(sharedContext);
                shadowContext.makeCurrent();
                
                currentContext = shadowContext;
                contextProvider = null;
            } // else shadowContext or contextProvider already give us a context
            
            return currentContext;
        }
        
        public OpenGLContextAdapter setActiveSurface(AbstractSurface surface, int layer) {
            if (surface != activeSurface) {
                // The new surface is different from the active surface, so
                // we must deactivate and unlock the last active surface.
                unlockActiveSurface();
                
                // If we don't have a surface to activate, just return now 
                // before tampering with the current context
                if (surface == null)
                    return null;
                
                // Now check to see if the underlying context needs to change
                OpenGLContextAdapter newContext = surface.getContext();
                if (newContext != null) {
                    // New surface needs its own context, so release and unlock the current context
                    releaseContext();
                    
                    // Grab the surface's lock now as the outer context lock
                    if (!getPersistentLock(surface))
                        return null; // Surface is destroyed, so escape
                    
                    // Now make its context current
                    newContext.makeCurrent();
                    currentContext = newContext;
                    contextProvider = surface;
                } else {
                    // Make sure we have a context for this surface, since it doesn't have its own
                    ensureContext();
                }
                
                // At this point we have a valid context (with an outer lock if the surface
                // provided their own context). Now we need to get the inner lock and activate the surface
                //  - This is a double-lock if surface.getContext() != null
                if (!getPersistentLock(surface))
                    return null;
                
                surface.onSurfaceActivate(currentContext, layer);
                activeSurface = surface;
            } // else we're already active so no change is needed
            
            return currentContext;
        }
        
        public void unlock(AbstractSurface surface) {
            // unlock inner lock
            if (surface == activeSurface)
                unlockActiveSurface();
            
            // unlock outer lock
            if (surface == contextProvider)
                releaseContext();
        }
        
        private void unlockActiveSurface() {
            if (activeSurface != null) {
                // Since activeSurface is not null, a context is current
                activeSurface.onSurfaceDeactivate(currentContext);
                
                if (activeSurface != contextProvider) {
                    // The surface's lock is held once, so we need to remove it
                    // from the persistent surface map. Must be done before we unlock
                    persistentSurfaceLocks.remove(activeSurface);
                }
                
                activeSurface.getLock().unlock();
                activeSurface = null;
            }
        }
        
        private void releaseContext() {
            if (activeSurface != null) {
                // If we're unlocking the context, we can't have any active surface
                unlockActiveSurface();
            }
            
            if (contextProvider != null) {
                // The current context will be the context provider's, so release it
                currentContext.release();
                currentContext = null;
                
                // Remove the context provider from the persistent locks map (while we still
                // have the lock). If we're unlocking the context provider, that is the only
                // lock the thread can have on the surface (unlike the active surface).
                persistentSurfaceLocks.remove(contextProvider);
                
                contextProvider.getLock().unlock();
                contextProvider = null;
            } else if (currentContext != null) {
                // The active context is from the shadow context, and it's requested that
                // we unlock that if there was no context surface
                currentContext.release();
                currentContext = null;
            }
        }
        
        private boolean getPersistentLock(AbstractSurface surface) {
            ContextManager.this.lock(surface);
            if (surface.isDestroyed()) {
                // The surface has been destroyed so unlock it and return false
                surface.getLock().unlock();
                return false;
            } else {
                // The surface hasn't been destroyed so record it and return true
                persistentSurfaceLocks.put(surface, this);
                return true;
            }
        }
        
        @Override
        public void run() {
            while(!lifecycleManager.isStopped()) {
                // Grab a single task from the queue and run it
                // Unlocking a surface is handled by pushing a special task
                // to the front of the queue so it skips any line from actual tasks
                Sync<?> task;
                try {
                    task = tasks.take();
                    
                    // We don't need to check for exceptions here because
                    // the sync catches everything and stores it for later.
                    // - If CM's code throws an exception, that should break everything
                    //   since it signals a bigger problem (instead of user code).
                    task.run();
                } catch (InterruptedException e) {
                    // Ignore the interrupted exception and loop again
                }
                
                unlockActiveSurface();
            }
            releaseContext();
            
            if (shadowContext != null && ownsShadowContext) {
                // This thread is the only owner of the shadow context
                // so we don't need to lock anything before we destroy it
                shadowContext.destroy();
            }
            
            // At this point, thee task queue is no longer being modified.
            // The lifecycle manager is shutting down, so any calls to queue()
            // will have the tasks automatically canceled.
            
            // All remaining tasks need to be canceled
            for (Sync<?> sync: tasks)
                sync.cancel(false);
            tasks.clear();
        }
    }
    
    /*
     * Simple task to force a ContextThread to fully unlock a surface.
     */
    private class UnlockSurfaceCallable implements Callable<Void> {
        private final AbstractSurface surface;
        
        public UnlockSurfaceCallable(AbstractSurface surface) {
            this.surface = surface;
        }
        
        @Override
        public Void call() throws Exception {
            // unlock() correctly handles when the surface isn't actually locked
            Thread current = Thread.currentThread();
            if (current instanceof ContextThread) {
                // Delegate to the thread implementation
                ((ContextThread) current).unlock(surface);
            } else {
                // Should never happen, these methods should be restricted to the ContextThreads
                throw new IllegalThreadStateException("unlock() cannot be called on this thread");
            }
            return null;
        }
    }
}
