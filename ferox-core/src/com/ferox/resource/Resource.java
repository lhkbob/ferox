package com.ferox.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ferox.renderer.Framework;

/**
 * <p>
 * An abstract class that represents some type of data stored on the graphics
 * card. A resource is fairly abstract so there many things can be represented
 * (assuming there are hardware capabilities supporting it). Some examples
 * include Texture, Geometry, and GlslProgram.
 * </p>
 * <p>
 * There are multiple ways that a Resource can be managed with a Framework. A
 * Resource cannot be used until its been updated by a Framework. There are
 * multiple ways that a Resource can be updated, some of which are automatic:
 * <ol>
 * <li>Implement some manager to call update() and dispose() with the necessary
 * Resources, and monitor the returned Futures</li>
 * <li>Use a Framework's update() and dispose() methods and trust that they
 * complete as needed (per their contract).</li>
 * <li>Rely on the Framework automatically updating a Resource if it's never
 * seen the Resource before, or if the Resource has a non-null dirty descriptor</li>
 * </ol>
 * When a Resource can only be accessed by weak references, a Framework will
 * automatically schedule it for disposal. A Framework that's destroyed will
 * have any remaining Resource's internal data disposed, too.
 * </p>
 * <p>
 * It is not required that Resource implementations be thread-safe. Instead,
 * each Thread that intends to modify or use the Resource (e.g. worker threads
 * of a Framework) must synchronize on the Resource instance. To prevent
 * deadlocks, each Thread should acquire only one resource lock at a time.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class Resource {    
    public static final Object FULL_UPDATE = new Object();
    
    /**
     * Each resource will have a status with the active renderer. A Resource is
     * usable if it has a status of READY. Resources that are DISPOSED will be
     * auto-updated when used. A Resource that has a status of ERROR is unusable
     * until it's been repaired.
     */
    public static enum Status {
        /** The resource has been updated successfully and is ready to use. */
        READY,
        /**
         * The Framework has tried to update the resource and there may be
         * internal data for the Resource, but something is wrong and the
         * Resource isn't usable.
         */
        ERROR,
        /**
         * The Framework has no support for the Resource sub-class. Like ERROR
         * it means the Resource is unusable. Unlike ERROR, the Resource cannot
         * be used without an exception being thrown, and it's impossible to
         * modify the Resource to change this status.
         */
        UNSUPPORTED,
        /**
         * The Framework's connection to the graphics card was lost and the
         * Resource's internal data has been invalidated. The Framework will
         * automatically change the Resource's status back to READY when
         * possible.
         */
        DISCONNECTED,
        /**
         * The Framework has no internal representations of the Resource (never
         * updated, or it's been disposed).
         */
        DISPOSED
    }

    /**
     * The UpdatePolicy of a Resource controls the behavior of Frameworks and
     * how they manage their Resources. By default, created Resources have the
     * AUTOMATIC policy which applies changes automatically as they occur. The
     * ON_DEMAND policy batches changes to minimize state changes and operations
     * on a resource, and is a mix between AUTOMATIC and MANUAL. The MANUAL
     * policy forces Resources to be updated manually with
     * {@link Framework#update(Resource)} as needed.
     */
    public static enum UpdatePolicy {
        /**
         * Changes will be applied automatically as they are reported by the
         * Resource to its ResourceListeners. If needed, all pending changes
         * will be flushed before the Resource is used by a Framework. This is
         * the default policy. Calling {@link Framework#update(Resource)} on an
         * AUTOMATIC resource will return a Future that will block until all
         * currently queued changes are flushed.
         */
        AUTOMATIC,
        /**
         * Like AUTOMATIC, changes are applied automatically, however, changes
         * are only flushed before the Resource is used by a Framework. Calling
         * {@link Framework#update(Resource)} on an ON_DEMAND resource will
         * flush all pending changes and will return a FUTURE that blocks until
         * they are completed. A scheduled update can be used to flush changes
         * before a resource is needed.
         */
        ON_DEMAND,
        /**
         * Frameworks track changes to a Resource but do not apply the changes
         * until specifically requested with a call to
         * {@link Framework#update(Resource)}. Changes are not flushed even if
         * the Resource is needed by the Framework.
         */
        MANUAL
    }
    
    private static AtomicInteger idCounter = new AtomicInteger(0);
    
    private final int id;
    private final List<ResourceListener> listeners;
    
    private UpdatePolicy policy;
    
    public Resource() {
        id = idCounter.getAndIncrement();
        listeners = new ArrayList<ResourceListener>();
        policy = UpdatePolicy.AUTOMATIC;
    }
    
    public UpdatePolicy getUpdatePolicy() {
        return policy;
    }
    
    public void setUpdatePolicy(UpdatePolicy policy) {
        if (policy == null)
            throw new NullPointerException("UpdatePolicy cannot be null");
        this.policy = policy;
    }
    
    public void addResourceListener(ResourceListener listener) {
        if (listener == null)
            throw new NullPointerException("ResourceListener cannot be null");
        
        synchronized(this) {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }
    
    public void removeResourceListener(ResourceListener listener) {
        if (listener == null)
            throw new NullPointerException("ResourceListener cannot be null");
        
        synchronized(this) {
            listeners.remove(listener);
        }
    }
    
    protected void notifyChange(Object changeDescriptor) {
        synchronized(this) {
            int ct = listeners.size();
            for (int i = 0; i < ct; i++) {
                listeners.get(i).onResourceChange(this, changeDescriptor);
            }
        }
    }

    /**
     * Return a unique numeric id that's assigned to this Resource instance.
     * Each instantiated Resource is assigned an id, starting at 0, which is
     * valid only for the lifetime of the current JVM.
     * 
     * @return This Resource's unique id
     */
    public final int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id;
    }
    
    @Override
    public boolean equals(Object o) {
        return o == this;
    }
}
