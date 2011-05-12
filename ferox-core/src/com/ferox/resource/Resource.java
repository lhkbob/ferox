package com.ferox.resource;

import java.util.concurrent.atomic.AtomicInteger;

import com.ferox.renderer.Framework;

/**
 * <p>
 * An abstract class that represents some type of data stored on the graphics
 * card. A resource is fairly abstract so there many things can be
 * represented (assuming there are hardware capabilities supporting it). Some
 * examples include {@link Texture}, {@link VertexBufferObject}, and
 * {@link GlslShader}.
 * </p>
 * <p>
 * There are multiple ways that a Resource can be managed with a Framework. A
 * Resource cannot be used until its been updated by a Framework. There are
 * different ways that a Resource can be updated, some of which are automatic.
 * See {@link UpdatePolicy} for more details.
 * </p>
 * <p>
 * When a Resource can only be accessed by weak references, a Framework will
 * automatically schedule it for disposal. A Framework that's destroyed will
 * have any remaining Resource's internal data disposed, too.
 * </p>
 * <p>
 * To provide thread safety while accessing and using resources, each resource
 * is guarded by its built-in monitor, meaning manipulating the resource should
 * be done with a 'synchronized' block on that resource. Resources that expose
 * simple setters are recommended to be 'synchronized'. Application code should
 * acquire the lock when performing bulk manipulation such as editing any
 * associated {@link BufferData buffer data}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class Resource {
    /**
     * Each resource will have a status with each active {@link Framework}. A
     * Resource is usable if it has a status of READY. Resources that are
     * DISPOSED have no stored data on the graphics card or in memory. A
     * Resource that has a status of ERROR is unusable until it's been repaired.
     */
    public static enum Status {
        /** The resource has been updated successfully and is ready to use. */
        READY,
        /**
         * The Framework has tried to update the resource and there may be
         * internal storage used for the Resource, but something is wrong and
         * the Resource isn't usable.
         */
        ERROR,
        /**
         * The Framework has no support for the Resource sub-class. Like ERROR
         * it means the Resource is unusable. Unlike ERROR, the Resource cannot
         * be used, and it's impossible to modify the Resource to change this
         * status.
         */
        UNSUPPORTED,
        /**
         * The Framework has no internal representations of the Resource (never
         * updated, or it's been disposed).
         */
        DISPOSED
    }

    /**
     * The UpdatePolicy of a Resource controls the behavior of Frameworks and
     * how they manage their Resources. By default, created Resources have the
     * ON_DEMAND policy, which checks for changes when a Resource is needed for
     * rendering. The MANUAL
     * policy forces Resources to be updated manually with
     * {@link Framework#update(Resource)} as needed.
     */
    public static enum UpdatePolicy {
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
    private UpdatePolicy policy;
    
    public Resource() {
        id = idCounter.getAndIncrement();
        policy = UpdatePolicy.ON_DEMAND;
    }
    
    public synchronized UpdatePolicy getUpdatePolicy() {
        return policy;
    }
    
    public synchronized void setUpdatePolicy(UpdatePolicy policy) {
        if (policy == null)
            throw new NullPointerException("UpdatePolicy cannot be null");
        this.policy = policy;
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
