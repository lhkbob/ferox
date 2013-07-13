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

import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.impl.LifeCycleManager.Status;

import java.util.concurrent.*;

/**
 * <p/>
 * ContextManager is the manager that handles the internal threads that runs code within a valid OpenGL
 * context. It has logic to keep a context current after task has completed, meaning that it does not need to
 * have expensive makeCurrent()/release() cycles in the common case where a single surface is rendered into
 * repeatedly.
 * <p/>
 * A newly constructed ContextManager is not ready to use until its {@link #initialize(LifeCycleManager,
 * SurfaceFactory)} is called. The ContextManager is expected to live within the life cycle of its owning
 * Framework (as enforced by the LifeCycleManager).
 *
 * @author Michael Ludwig
 */
public class ContextManager {
    // "final" after initialize() is called
    private LifeCycleManager lifecycleManager;
    private ContextThread thread;
    private OpenGLContext sharedContext;

    /**
     * <p/>
     * Complete the initialization of this ContextManager and start up the thread used to process GPU-based
     * tasks. It will also create the shared context used by every surface with this manager.. This method
     * ties the ContextManager to the life cycle enforced by the given LifeCycleManager. It is required that
     * this method is called by the ContextManager's owner in the initialization Runnable passed to {@link
     * LifeCycleManager#start(Runnable)}.
     * <p/>
     * The ContextManager will automatically terminate its threads when it detects that the LifeCycleManager
     * is being shutdown. It will continue running tasks until the manager is fully stopped.
     * <p/>
     * The ContextManager cannot be initialized more than once. It is illegal to use a LifeCycleManager that
     * has a status other than STARTING (i.e. within the scope of its initialize() method).
     *
     * @param lifecycle      The LifeCycleManager that controls when the ContextManager ends
     * @param surfaceFactory The SurfaceFactory to create the shared context with
     *
     * @throws NullPointerException  if lifecycle or surfaceFactory are null
     * @throws IllegalStateException if lifecycle doesn't have a status of STARTING, or if the ContextManager
     *                               has already been initialized
     */
    public void initialize(LifeCycleManager lifecycle, SurfaceFactory surfaceFactory) {
        if (lifecycle == null) {
            throw new NullPointerException("LifeCycleManager cannot be null");
        }
        if (surfaceFactory == null) {
            throw new NullPointerException("SurfaceFactory cannot be null");
        }

        // We are assuming that we're in the right threading situation, so this is safe.
        // If this is called outside of the manager's lock then all bets are off, but that's their fault.
        if (lifecycle.getStatus() != Status.STARTING) {
            throw new IllegalStateException(
                    "LifeCycleManager must have status STARTING, not: " + lifecycle.getStatus());
        }

        // Do a simple exclusive lock to check for double-init attempts. This won't hurt threading
        // since we should already be in lifecycle's write lock.
        synchronized (this) {
            if (lifecycleManager != null) {
                throw new IllegalStateException("ContextManager already initialized");
            }
            lifecycleManager = lifecycle;
        }

        thread = new ContextThread(lifecycle.getManagedThreadGroup(), "gpu-task-thread");

        // Start the managed thread as a high priority thread so that it can run
        // while the other threads terminate (and potentially queue tasks)
        lifecycle.startManagedThread(thread, true); //

        // Very first task must be to allocate the shared context
        try {
            invokeOnContextThread(new ConstructContextCallable(surfaceFactory), false).get();
        } catch (InterruptedException e) {
            // ignore for now
        } catch (ExecutionException e) {
            throw new FrameworkException("Error creating shared context", e.getCause());
        }
    }

    /**
     * @return The shared context that must be used by all surfaces for this manager
     *
     * @throws IllegalStateException if the context manager hasn't been properly initialized yet
     */
    public OpenGLContext getSharedContext() {
        if (sharedContext == null) {
            throw new IllegalStateException("Shared context has not been created yet");
        }
        return sharedContext;
    }

    /**
     * <p/>
     * Invoke the given Callable on the context thread managed by this manager. If the calling thread is not
     * the context thread, this task is queued behind any other pending tasks. However, if the calling thread
     * is the context thread, this will run the task immediately. In this case the returned Future will have
     * already completed.
     * <p/>
     * If the LifeCycleManager controlling this ContextManager is being shutdown, or has been shutdown, the
     * returned Future is not queued and is preemptively cancelled. It will never be null.
     *
     * @param <T>              The type of data returned by task
     * @param task             The task to run on an internal thread
     * @param acceptOnShutdown True if the task should be queued even while shutting down
     *
     * @return A Future linked to the queued task, will be cancelled if the ContextManager has been shutdown
     *         or is shutting down
     *
     * @throws NullPointerException if task is null
     */
    public <T> Future<T> invokeOnContextThread(Callable<T> task, boolean acceptOnShutdown) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        // Create the Future now so that it can be easily canceled later if need be
        Sync<T> sync = new Sync<T>(task);
        Future<T> future = new FutureSync<T>(sync);

        lifecycleManager.getLock().lock();
        try {
            Status status = lifecycleManager.getStatus();
            if (!lifecycleManager.isStopped() ||
                (acceptOnShutdown && status == Status.STOPPING_LOW_PRIORITY)) {
                if (isContextThread()) {
                    // don't queue and run the task right away
                    sync.run();
                } else {
                    // this is written like this to guard against interrupts
                    boolean queued;
                    do {
                        try {
                            queued = thread.tasks.offerLast(sync, 5, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException ie) {
                            queued = false;
                        }
                    } while (!queued);
                }
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
     * Return whether or not the current Thread is a thread managed by this ContextManager and is capable of
     * having OpenGL contexts current on it.
     *
     * @return True if the calling Thread is an inner thread managed by this ContextManager
     */
    public boolean isContextThread() {
        return Thread.currentThread() == thread;
    }

    /**
     * <p/>
     * Activate the provided Surface on the current thread. If the surface has its own OpenGLContext, that
     * context is made current on the thread. If it does not have a context, the surface piggybacks on the
     * last surface's context or on an internal offscreen context.
     * <p/>
     * An activated surface that has its own context will continue to have its context current on the thread
     * after the task completes until a new surface is activated with its own context, or {@link
     * #forceRelease(AbstractSurface)} method is called.
     * <p/>
     * Passing in a null Surface will deactivate the currently active surface, and the layer parameter is
     * ignored.
     * <p/>
     * This can only be called from code running on the internal thread managed by the ContextManager. This
     * assumes the surface is owned by the Framework owning this ContextManager (if this assumption is broken,
     * undefined results will occur).
     * <p/>
     * If <var>surface</var> is already destroyed, the current surface is deactivated and a null context is
     * returned.
     *
     * @param surface The AbstractSurface to activate
     * @param layer   The layer to activate, will be passed directly to {@link AbstractSurface#onSurfaceActivate(OpenGLContext,
     *                int)}
     *
     * @return The OpenGLContext that is current after this surface has been activated
     *
     * @throws IllegalStateException if {@link #isContextThread()} returns false
     */
    // FIXME how do we specify the render targets? Is that a later part of the
    // surface activation specific to the texture surfaces? e.g. it's something
    // the hardware access layer is concerned with and not the context manager?
    //
    // This seems reasonable; we can also move surface activation/deactivation there?
    // or at least move renderer resetting to the hardware access layer
    public OpenGLContext setActiveSurface(AbstractSurface surface, int layer) {
        // Further validation is handled in the ContextThread after the lock is made
        Thread current = Thread.currentThread();
        if (current == thread) {
            // Delegate to the thread implementation
            return thread.setActiveSurface(surface, layer);
        } else {
            // Should never happen, these methods should be restricted to the ContextThreads
            throw new IllegalThreadStateException("setActiveSurface() cannot be called on this Thread");
        }
    }

    /**
     * <p/>
     * Force the context thread to deactivate the given surface (if it was active), and release the surface's
     * context if it is still current. The context may need to be released even if the surface wasn't active.
     * If a second surface is relying on the given surface's context, that surface will be forcefully
     * deactivated as well.
     * <p/>
     * This can only be called on the task thread of this manager.
     *
     * @param surface The surface to release
     *
     * @throws NullPointerException  if surface is null
     * @throws IllegalStateException if this is not the context thread
     */
    public void forceRelease(AbstractSurface surface) {
        if (surface == null) {
            throw new NullPointerException("Surface cannot be null");
        }

        Thread current = Thread.currentThread();
        if (current == thread) {
            // Delegate to the thread implementation
            thread.releaseSurface(surface);
        } else {
            // Should never happen, these methods should be restricted to the ContextThreads
            throw new IllegalThreadStateException("forceRelease() cannot be called on this Thread");
        }
    }

    /**
     * <p/>
     * Ensure that there is a valid context current on this thread. If a surface is already active, the
     * returned context might be that surface's context. If there is no active surface, the context will be
     * the context of the last activated surface that had a context (assuming this thread still has the lock),
     * or the offscreen shared context.
     * <p/>
     * Regardless, this will not return null and the returned context will correctly share resources with the
     * other contexts created by the SurfaceFactory provided to this ContextManager during initialization.
     *
     * @return The current context
     *
     * @throws IllegalStateException if {@link #isContextThread()} returns false
     */
    public OpenGLContext ensureContext() {
        Thread current = Thread.currentThread();
        if (current == thread) {
            // Delegate to the thread implementation
            return thread.ensureContext();
        } else {
            // Should never happen, these methods should be restricted to the ContextThreads
            throw new IllegalThreadStateException("ensureContext() cannot be called on this Thread");
        }
    }

    /*
     * Internal thread class that manages the contexts and locks for the active
     * surface, allowing this to be used easily with a simple
     * HardwareAccessLayer implementation. It uses up to two surfaces at a time
     * (one for being "active" and one that provides a context).
     */
    private class ContextThread extends Thread {
        private AbstractSurface activeSurface; // active surface, might differ from contextProvider
        private OpenGLContext currentContext; // non-null when a context is current

        private final BlockingDeque<Sync<?>> tasks; // pending tasks

        public ContextThread(ThreadGroup group, String name) {
            super(group, name);
            tasks = new LinkedBlockingDeque<Sync<?>>(10);
            setDaemon(true);
        }

        public OpenGLContext ensureContext() {
            if (currentContext == null) {
                // There is no surface to piggy-back off of, so we we use the shared context
                if (sharedContext == null) {
                    // bad initialization code, a task got queued before the shared
                    // context was created (should not happen)
                    throw new IllegalStateException("Shared context has not been created yet");
                }

                sharedContext.makeCurrent();

                currentContext = sharedContext;
            } // else there's a current context from somewhere, just go with it

            return currentContext;
        }

        public OpenGLContext setActiveSurface(AbstractSurface surface, int layer) {
            if (surface != activeSurface) {
                // The new surface is different from the active surface, so
                // we must deactivate the last active surface.
                deactivateSurface();

                // If we don't have a surface to activate, just return now
                // before tampering with the current context
                if (surface == null || surface.isDestroyed()) {
                    return null;
                }

                // Now check to see if the underlying context needs to change
                OpenGLContext newContext = surface.getContext();
                if (newContext != null) {
                    if (newContext != currentContext) {
                        // New surface needs its own context, so release and unlock the current context
                        releaseContext();

                        // Now make its context current
                        newContext.makeCurrent();
                        currentContext = newContext;
                    }
                } else {
                    // Make sure we have a context for this surface, since it doesn't have its own
                    ensureContext();
                }

                activeSurface = surface;
                surface.onSurfaceActivate(currentContext, layer);
            } else {
                // This is already the active surface, but cycle deactivate/activate
                // to make it notice the switch
                surface.onSurfaceDeactivate(currentContext);
                surface.onSurfaceActivate(currentContext, layer);
            }

            return currentContext;
        }

        public void releaseSurface(AbstractSurface surface) {
            // first deactivate it if it's the active surface
            if (surface == activeSurface) {
                deactivateSurface();
            }

            // then make sure we release the current context because many libs
            // make the surface being destroyed current prior to destruction
            // and release, but that makes our tracking get out of sync
            releaseContext();
        }

        private void deactivateSurface() {
            if (activeSurface != null) {
                // Since activeSurface is not null, a currentContext won't be null
                activeSurface.onSurfaceDeactivate(currentContext);
                activeSurface = null;

                // we do not release the current context, even if
                // the active surface was the context provider
            }
        }

        private void releaseContext() {
            if (activeSurface != null) {
                // If we're unlocking the context, we can't have any active surface
                deactivateSurface();
            }

            if (currentContext != null) {
                currentContext.release();
                currentContext = null;
            }
        }

        @Override
        public void run() {
            // loop until we hit STOPPING_HIGH_PRIORITY, so that we still process tasks while in that stage
            // transition to STOPPED until all children are done
            while (lifecycleManager.getStatus().compareTo(Status.STOPPING_HIGH_PRIORITY) < 0) {
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

                // Because all surfaces that are active must be AbstractSurfaces, this will
                // reset all of the renderers so resources will be unlocked correctly,
                // even in the event of an exception.
                deactivateSurface();
            }
            releaseContext();
            // This thread is the owner of the shared context
            sharedContext.destroy();

            // At this point, the task queue is no longer being modified.
            // The lifecycle manager is shutting down, so any calls to queue()
            // will have the tasks automatically canceled.

            // All remaining tasks need to be canceled
            for (Sync<?> sync : tasks) {
                sync.cancel(false);
            }
            tasks.clear();
        }
    }

    /*
     * Simple task to initialize the shared context of this manager
     */
    private class ConstructContextCallable implements Callable<Void> {
        private final SurfaceFactory surfaceFactory;

        public ConstructContextCallable(SurfaceFactory surfaceFactory) {
            this.surfaceFactory = surfaceFactory;
        }

        @Override
        public Void call() throws Exception {
            // the context is only ever used on the thread running this
            // task, so this assignment is thread safe
            sharedContext = surfaceFactory.createOffscreenContext(null);
            return null;
        }
    }
}
