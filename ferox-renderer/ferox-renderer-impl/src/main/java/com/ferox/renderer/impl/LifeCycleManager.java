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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ferox.renderer.Framework;

/**
 * <p>
 * The LifeCycleManager is a utility to provide a thread-safe mechanism that
 * imposes a single life cycle on a number of related components. It is assumed
 * that there is an owning object (such as the {@link Framework} implementation)
 * that controls the creation of the LifeCycleManager and exposes a public
 * interface to begin the life cycle.
 * </p>
 * <p>
 * The LifeCycleManager provides two methods to change its status. The method
 * {@link #start(Runnable)} is used to start the manager and
 * {@link #stop(Runnable)} is used to end it. Both take Runnable method hooks to
 * allow custom code to be run during these state transitions. The owner of the
 * LifeCycleManager would use these to initialize all components that it
 * depended on.
 * 
 * @author Michael Ludwig
 */
public class LifeCycleManager {
    /**
     * A LifeCycleManager has a monotonically increasing status. The status will
     * only change in the order defined in this enum, although it may skip
     * states.
     */
    public static enum Status {
        WAITING_INIT,
        STARTING,
        ACTIVE,
        STOPPING,
        WAITING_ON_CHILDREN,
        STOPPED
    }

    private final ReentrantReadWriteLock lock;
    private final ThreadGroup managedThreadGroup;
    private final CopyOnWriteArrayList<Thread> managedThreads;

    private volatile Status status;

    /**
     * Create a new LifeCycleManager that uses the given group name for the
     * ThreadGroup that all managed threads will be part of.
     * 
     * @param groupName The ThreadGroup name for all threads that will be
     *            managed by this manager
     * @throws NullPointerException if groupName is null
     */
    public LifeCycleManager(String groupName) {
        if (groupName == null) {
            throw new NullPointerException("groupName cannot be null");
        }

        managedThreadGroup = new ThreadGroup(groupName);
        managedThreads = new CopyOnWriteArrayList<Thread>();

        status = Status.WAITING_INIT;
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Return whether or not the LifeCycleManager has reached the end of its
     * lifetime. This returns true if its status is STOPPING or STOPPED.
     * 
     * @return True if the manager is stopped
     */
    public boolean isStopped() {
        Status status = this.status;
        return status == Status.STOPPING || status == Status.STOPPED || status == Status.WAITING_ON_CHILDREN;
    }

    /**
     * <p>
     * Start or initialize this LifeCycleManager. This will transition its
     * status from WAITING_INIT to STARTING to ACTIVE. This can only be called
     * once. The first time this is invoked, true is returned. All other
     * invocations return false and do not change the status of the manager.
     * <p>
     * <tt>onInit</tt> is invoked only if true will be returned (i.e. the first
     * time this is called), and should contain framework level code to be
     * performed on initialization. Some examples might be to start managed
     * threads or to initialize subcomponents that exist within this managed
     * lifecycle. While the provided Runnable is running, the manager has a
     * status of STARTING. When the Runnable completes, this changes to ACTIVE.
     * <p>
     * The provided Runnable must be "trusted" code and should not throw
     * exceptions or the manager will be trapped in STARTING. The runnable must
     * be thread safe so that it can safely initialize the system from whatever
     * thread invoked start().
     * 
     * @param onInit A Runnable to run initialization code within an exclusive
     *            lock on the lifecycle
     * @return True if the manager was successfully started
     */
    public boolean start(Runnable onInit) {
        lock.writeLock().lock();
        try {
            if (status != Status.WAITING_INIT) {
                return false;
            }
            status = Status.STARTING;
            if (onInit != null) {
                onInit.run();
            }
            status = Status.ACTIVE;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * <p>
     * Stop or destroy this LifeCycleManager. The status transitions depends on
     * the current state of the manager. Like {@link #start(Runnable)}, this can
     * only be invoked once and all future calls do nothing except return false.
     * <p>
     * If the manager is WAITING_INIT, its status changes directly to STOPPED
     * and does not run either Runnable. If the status is ACTIVE, it first runs
     * <tt>preDestroy</tt>, then changes status changes to STOPPING. The manager
     * then starts a new thread that will eventually run the code in
     * <tt>onDestroy</tt>. The new thread will first block until all managed
     * threads have terminated. After the threads have finished,
     * <tt>onDestroy</tt> is run and the status is changed to STOPPED.
     * <p>
     * A value of true is returned the first time this is invoked. A value of
     * false is returned if the manager is stopping, has stopped or is starting.
     * Calls to this method while the status is STARTING return false and do
     * nothing.
     * <p>
     * The provided Runnable must be "trusted" code and should not throw
     * exceptions or the manager's state will be undefined. <tt>preDestroy</tt>
     * must be thread safe so that it can be safely called from whatever thread
     * invoked stop(). <tt>postDestroy</tt> must be safe to call from the
     * shutdown thread that is started by this manager.
     * 
     * @param preDestroy A Runnable executed after the exclusive lock is held,
     *            but before the state transitions to STOPPING
     * @param postDestroy A Runnable executed after status changes to STOPPED
     * @return True if the manager will transition to STOPPING or STOPPED and
     *         false otherwise
     * @throws NullPointerException if either runnable is null
     */
    public boolean stop(Runnable preDestroy, Runnable postDestroy) {
        lock.writeLock().lock();
        try {
            // Cannot destroy if actively being started, destroyed or has already been destroyed
            if (status != Status.WAITING_INIT && status != Status.ACTIVE) {
                return false;
            }

            if (status == Status.WAITING_INIT) {
                // never initialized
                status = Status.STOPPED;
                return true;
            } else {
                // status must be ACTIVE, so start a shutdown thread
                status = Status.STOPPING;

                // invoke this task while STOPPING, but before we block on children
                if (preDestroy != null) {
                    preDestroy.run();
                }

                // Send an interrupt to all managed threads
                status = Status.WAITING_ON_CHILDREN;
                // - we can't just interrupt the group because some impl's use AWT
                //   which then inherits this group and gets fussy when we send
                //   interrupts out.
                for (Thread m : managedThreads) {
                    m.interrupt();
                }

                ThreadGroup shutdownOwner = Thread.currentThread().getThreadGroup();
                while (managedThreadGroup.parentOf(shutdownOwner)) {
                    // The shutdown thread joins on threads within the managedThreads group,
                    // so it can't be part of that thread group.
                    shutdownOwner = shutdownOwner.getParent();
                }

                Thread shutdown = new Thread(shutdownOwner,
                                             new ShutdownTask(postDestroy),
                                             "lifecycle-shutdown-thread");
                shutdown.setDaemon(false); // Don't let the JVM die until this is finished
                shutdown.start();

                return true;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Return the lock to hold that will prevent the lifecycle from
     * transitioning to STOPPING or STOPPED. After acquiring the lock, code
     * should verify that the status is ACTIVE and act appropriately. This is
     * not an exclusive lock, it prevents status changes but multiple threads
     * can hold this lock and run in parallel.
     * 
     * @return The lock that prevents status changes
     */
    public Lock getLock() {
        return lock.readLock();
    }

    /**
     * @return The current status of the manager
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Return the ThreadGroup that all managed threads must be part of. If
     * {@link #startManagedThread(Thread)} is used, the created thread must have
     * the returned group as an ancestor or direct parent. The returned group
     * has the name provided in the constructor.
     * 
     * @return The LifeCycleManager's managed thread group
     */
    public ThreadGroup getManagedThreadGroup() {
        return managedThreadGroup;
    }

    /**
     * <p>
     * Start the provided thread and assume management over it. This thread
     * becomes a managed thread of this LifeCycleManager. It is only valid to
     * start a managed thread if the LifeCycleManager has a status of STARTING
     * or ACTIVE. All other attempts will do nothing and return false. True is
     * returned when the Thread becomes managed and has had its
     * {@link Thread#start()} method invoked.
     * <p>
     * A managed thread implies that the thread is responsible for terminating
     * when the LifeCycleManager has its {@link #stop(Runnable)}. This does not
     * need to be immediate but should be as-soon-as-possible. The
     * LifeCycleManager will interrupt all managed threads in case they are
     * asleep or blocking on some task.
     * <p>
     * Care must be given to prevent managed threads from dead-locking because
     * it will halt the entire shutdown process. Managed threads will block the
     * final transition from STOPPING to STOPPED until they have all terminated,
     * giving them a way to automatically finish their current task.
     * <p>
     * The provided thread should not have been started or an exception is
     * thrown. An exception is thrown if the thread's ThreadGroup is not a child
     * of the group returned by {@link #getManagedThreadGroup()}.
     * 
     * @param thread The thread to start
     * @return True if the thread becomes managed and has been started
     */
    public boolean startManagedThread(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("Thread cannot be null");
        }
        if (!managedThreadGroup.parentOf(thread.getThreadGroup())) {
            throw new IllegalArgumentException("Managed thread must be in the ThreadGroup provided by this LifeCycleManager");
        }

        lock.readLock().lock();
        try {
            // Cannot start a thread if the lifecycle is ending or hasn't started
            if (status == Status.STOPPED || status == Status.STOPPING || status == Status.WAITING_INIT) {
                return false;
            }

            // It is okay to start a thread while active, or starting
            thread.start();
            managedThreads.add(thread);
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    /*
     * Internal task to block while managed threads are shutdown and then run
     * the code provided in stop()
     */
    private class ShutdownTask implements Runnable {
        private final Runnable postStop;

        public ShutdownTask(Runnable postStop) {
            this.postStop = postStop;
        }

        @Override
        public void run() {
            // This iteration does not need to worry about changes to the thread-list
            // since the shutdown task is only started after the status is set to STOPPING
            // at which point no threads can be added to the managed group.
            boolean loop;
            do {
                loop = false;
                for (Thread managed : managedThreads) {
                    while (managed.isAlive()) {
                        try {
                            managed.join();
                        } catch (InterruptedException e) {
                            // remember that this thread may not be dead, so loop again
                            loop = true;
                        }
                    }
                }
            } while (loop);

            // We don't need to lock here since this is the only place where STOPPING -> STOPPED
            // and there will only ever be one shutdown thread.
            status = Status.STOPPED;

            postStop.run();
        }
    }
}
