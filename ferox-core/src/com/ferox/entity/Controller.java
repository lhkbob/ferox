package com.ferox.entity;

/**
 * <p>
 * A Controller represents the logical, process-oriented side of an
 * EntitySystem. This is in contrast to the data-driven nature of Entities and
 * Components, which have no logical or processing capabilities by themselves.
 * An instance of a Controller is designed to process a single EntitySystem,
 * which makes it easier for Controller implementations to store meta-data about
 * the system that it processes.
 * </p>
 * <p>
 * The effects of a Controller's processing are limited only by the Controller
 * implementations available at runtime. It is a general trend that Controllers
 * are specialized to a few or just one Component type, and only process the
 * Entities in a system with those Components attached. Controllers can be
 * multi-threaded within their {@link #process()} method, or multiple Controllers
 * can be executed in parallel over the same EntitySystem. In general, the
 * entity package is intended and designed to be thread safe, but it is
 * recommended that Controllers which modify the same type of Components on an
 * Entity be serialized to avoid lock contention.
 * </p>
 * <p>
 * Additionally, it is likely that Controllers have some implicit ordering
 * needed when using multiple Controllers to process a system to completion.
 * This could be one Controller computing some function or result, and having
 * another Controller depend on this result to perform the next action. These
 * dependencies are generally obvious when considering the nature of each
 * Controller implementation, but should be documented by the Controller
 * subclasses anyway.
 * </p>
 * 
 * @see Entity
 * @see EntitySystem
 * @author Michael Ludwig
 */
public abstract class Controller {
    private static final int MAX_INVOKE_COUNT = 0;
    protected final EntitySystem system;
    
    private int invokeCount;
    private long invokeNanos;

    /**
     * Create a Controller that will process the given EntitySystem each time
     * its {@link #process()} method is invoked.
     * 
     * @param system The EntitySystem to be processed
     * @throws NullPointerException if system is null
     */
    public Controller(EntitySystem system) {
        if (system == null)
            throw new NullPointerException("EntitySystem cannot be null");
        this.system = system;
    }
    
    /**
     * @return The EntitySystem that is processed by this Controller
     */
    public EntitySystem getEntitySystem() {
        return system;
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
    public void process() {
        long nanos = -System.nanoTime();
        processImpl();
        nanos += System.nanoTime();
        
        if (invokeCount > MAX_INVOKE_COUNT) {
            invokeCount = 1;
            invokeNanos = nanos;
        } else {
            invokeCount++;
            invokeNanos += nanos;
        }
    }
    
    /**
     * @return The average nanosecond runtime of this Controller's
     *         {@link #process()}
     */
    public long getAverageNanos() {
        if (invokeCount == 0)
            return 0;
        else
            return (invokeNanos / invokeCount);
    }
    
    protected abstract void processImpl();
}
