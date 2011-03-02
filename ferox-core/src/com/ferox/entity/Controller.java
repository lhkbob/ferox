package com.ferox.entity;

/**
 * <p>
 * A Controller represents the logical, process-oriented side of an
 * EntitySystem. This is in contrast to the data-driven nature of Entities and
 * Components, which have no logical or processing capabilities by themselves.
 * An instance of a Controller is designed to process a single EntitySystem,
 * which makes it easier for Controller implementations to store meta-data about
 * the system that it processes. Additionally, be sure to call
 * {@link #destroy()} when a Controller is no longer used so that it can clean
 * up its internal resources.
 * </p>
 * <p>
 * The effects of a Controller's processing are limited only by the Controller
 * implementations available at runtime. It is a general trend that Controllers
 * are specialized to a few or just one Component type, and only process the
 * Entities in a system with those Components attached. Controllers can be
 * multi-threaded within their {@link #execute()} method, or multiple
 * Controllers can be executed in parallel over the same EntitySystem. The
 * {@link ControllerExecutor} is a utility class that makes parallelism across
 * Controllers easy to achieve.
 * </p>
 * <p>
 * To take advantage of the ControllerExecutor, controllers that can be
 * multi-threaded should be annotated with the {@link Parallel} annotation. This
 * annotation provides the information needed to work out which controllers can
 * run in parallel, and is used by the executor. If a Controller does not have
 * the annotation, it is assumed that it is too critical to be run in parallel
 * with other controllers.
 * </p>
 * 
 * @see Entity
 * @see EntitySystem
 * @author Michael Ludwig
 */
public abstract class Controller {
    private static final int MAX_INVOKE_COUNT = 10;
    
    protected final EntitySystem system;
    protected final Object lock;
    
    private boolean destroyed;
    
    private int invokeCount;
    private long invokeNanos;

    /**
     * Create a Controller that will process the given EntitySystem each time
     * its {@link #execute()} method is invoked.
     * 
     * @param system The EntitySystem to be processed
     * @throws NullPointerException if system is null
     */
    public Controller(EntitySystem system) {
        if (system == null)
            throw new NullPointerException("EntitySystem cannot be null");
        this.system = system;
        lock = new Object();
        destroyed = false;
        invokeCount = 0;
        invokeNanos = 0;
    }
    
    /**
     * @return The EntitySystem that is processed by this Controller
     */
    public EntitySystem getEntitySystem() {
        return system;
    }

    /**
     * Destroy the Controller, which lets it clean up any cached resources it
     * may have been using to speed up its execution. It also allows it to
     * remove any EntityListeners it added to the EntitySystem it processes.
     * This must be invoked before discarding a Controller or memory leaks could
     * occur.
     */
    public void destroy() {
        synchronized(lock) {
            if (destroyed)
                return;
            destroyed = true;
            destroyImpl();
        }
    }

    /**
     * <p>
     * Process this Controller's associated EntitySystem in an implementation
     * dependent fashion, to achieve some effect on the Entities within the
     * system. Some possible examples include calculating the physics of
     * Entities that need to be simulated, or rendering Entities that have a
     * graphical nature.
     * </p>
     * <p>
     * The process() method is intentionally very vague in the effects its
     * performed because each implementation of Controller is intended to
     * perform the changes to the system state necessary to perform all of the
     * 'logic' of a system. The Entities and Components store the data of the
     * system and are simulated and processed by this method of the desired
     * Controllers.
     * </p>
     */
    public void execute() {
        synchronized(lock) {
            if (destroyed)
                return;

            long nanos = -System.nanoTime();
            executeImpl();
            nanos += System.nanoTime();

            if (invokeCount > MAX_INVOKE_COUNT) {
                invokeCount = 1;
                invokeNanos = nanos;
            } else {
                invokeCount++;
                invokeNanos += nanos;
            }
        }
    }
    
    /**
     * @return The average nanosecond runtime of this Controller's
     *         {@link #execute()}
     */
    public long getAverageNanos() {
        synchronized(lock) {
            if (invokeCount == 0)
                return 0;
            else
                return (invokeNanos / invokeCount);
        }
    }

    /**
     * Perform execution of this Controller, will be within the controller's
     * lock, and is only called by execute(). It will not be called if the
     * Controller is destroyed.
     */
    protected abstract void executeImpl();

    /**
     * Perform any necessary clean-up, will be within the controller's lock, and
     * will be called at most once.
     */
    protected abstract void destroyImpl();
}
