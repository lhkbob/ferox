package com.ferox.renderer.impl;

import com.ferox.renderer.Destructible;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * DestructibleManager controls a thread that monitors registered {@link
 * Destructible}/{@link ManagedDestructible} pairs of instances. When the Destructible
 * instance is weak-referenceable the ManagedDestructible is destroyed.  The
 * ManagedDestructible will be strong-referenced so it will not be garbage collected
 * before its destroy() method has been invoked.
 *
 * @author Michael Ludwig
 */
public class DestructibleManager {
    /**
     * ManagedDestructible is an interface like {@link Destructible} except that it
     * doesn't have the requirement that the instance cleans up automatically when it is
     * garbage collected. A managed destructible is the object that is strong referenced
     * by the DestructibleManager that is destroyed when an exposed Destructible instance
     * is GC'ed.
     */
    public static interface ManagedDestructible {
        /**
         * Destroy the instance, just like {@link com.ferox.renderer.Destructible#destroy()}.
         *
         * @return A future linked with the actual destruction task
         */
        public Future<Void> destroy();

        /**
         * @return True if {@link #destroy()} has been called
         */
        public boolean isDestroyed();
    }

    private final ReferenceQueue<Destructible> monitors;
    private final ConcurrentHashMap<ManagedDestructible, Boolean> managedInstances;

    private LifeCycleManager lifecycle;

    /**
     * Create a new DestructibleManager.
     */
    public DestructibleManager() {
        monitors = new ReferenceQueue<>();
        managedInstances = new ConcurrentHashMap<>();
    }

    /**
     * <p/>
     * Complete the initialization of this DestructibleManager and start up an inner
     * thread that handles processing garbage collected Destructibles. This method ties
     * the DestructibleManager to the life cycle enforced by the given LifeCycleManager.
     * It is required that this method is called by the DestructibleManager's owner in the
     * initialization Runnable passed to {@link LifeCycleManager#start(Runnable)}. The
     * provided LifeCycleManager should be the same manager that was used to initialize
     * the rest of the framework.
     * <p/>
     * The DestructibleManager will automatically terminate its thread when it detects
     * that the LifeCycleManager is being shutdown.
     * <p/>
     * The ResourceManager cannot be initialized more than once. It is illegal to use a
     * LifeCycleManager that has a status other than STARTING.
     *
     * @param lifecycle The LifeCycleManager that controls when the DestructibleManager
     *                  ends
     *
     * @throws NullPointerException  if lifecycle is null
     * @throws IllegalStateException if lifecycle doesn't have a status of STARTING, or if
     *                               the DestructibleManager has already been initialized
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
            if (this.lifecycle != null) {
                throw new IllegalStateException(
                        "DestructibleManager already initialized");
            }
            this.lifecycle = lifecycle;
        }

        // start a low priority managed thread
        Thread destroyer = new Thread(lifecycle.getManagedThreadGroup(),
                                      new WeakReferenceMonitor(),
                                      "destructible-gc-thread");
        destroyer.setDaemon(true);
        lifecycle.startManagedThread(destroyer, false);
    }

    /**
     * Have this DestructibleManager monitor a weak reference to {@code exposed} and
     * invoke {@link ManagedDestructible#destroy()} on {@code realInstance} when the
     * public facing instance has been collected.
     *
     * @param exposed      The public-facing or shim instance
     * @param realInstance The actual data-storing instance that can be cleaned up
     *
     * @throws NullPointerException if exposed or realInstance are null
     */
    public void manage(Destructible exposed, ManagedDestructible realInstance) {
        if (exposed == null || realInstance == null) {
            throw new NullPointerException("Destructible instances cannot be null");
        }

        lifecycle.getLock().lock();
        try {
            if (!lifecycle.isStopped()) {
                managedInstances.put(realInstance, true);
                new DestructibleReference(exposed, realInstance, monitors);
            }
        } finally {
            lifecycle.getLock().unlock();
        }
    }

    private class WeakReferenceMonitor implements Runnable {
        @Override
        public void run() {
            while (!lifecycle.isStopped()) {
                try {
                    // The Destructible associated with this data has been GC'ed
                    DestructibleReference data = (DestructibleReference) monitors
                            .remove();

                    // Don't block on this, we just need it to be disposed of in the future
                    // and don't bother accepting during shutdown since the context
                    // is about to be destroyed then anyway.
                    data.destructible.destroy();
                    managedInstances.remove(data.destructible);
                } catch (InterruptedException e) {
                    // Do nothing and keep looping
                }
            }

            // we're shutting down, but destroy everything that we have at this point,
            // but because the lifecycle is stopped we know this collection won't be
            // changing anymore either
            Iterator<ManagedDestructible> it = managedInstances.keySet().iterator();
            while (it.hasNext()) {
                it.next().destroy();
                it.remove();
            }
        }
    }

    private class DestructibleReference extends WeakReference<Destructible> {
        final ManagedDestructible destructible;

        public DestructibleReference(Destructible exposedInstance,
                                     ManagedDestructible realInstance,
                                     ReferenceQueue<Destructible> queue) {
            super(exposedInstance, queue);
            destructible = realInstance;
        }
    }
}
