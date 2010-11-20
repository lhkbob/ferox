package com.ferox.physics.collision;

/**
 * <p>
 * CollisionManagers are responsible for providing efficient access to
 * potentially intersecting {@link Collidable Collidables}. Collidables can be
 * added to the manager, at which point they are elligable for collision
 * detection with the other objects in the manager. Potentially intersecting
 * objects should be determined by the world bounds of each Collidable.
 * </p>
 * <p>
 * A {@link CollisionAlgorithmProvider} is responsible for narrowing the set potentially
 * intersecting objects down to those that are actually intersecting.
 * {@link CollisionCallback CollisionCallbacks} are used by the CollisionManager
 * to report each pair of objects that might be intersecting. Implementations
 * are free to exclude pairs that do not have their bounds intersecting.
 * </p>
 * <p>
 * CollisionManagers must be thread-safe. Additionally, they are allowed to
 * execute CollisionCallbacks on multiple and/or different threads when
 * processing the collision.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface CollisionManager {
    /**
     * Register a Collidable with this CollisionManager. Adding a Collidable
     * that already is registered should a no-op. In the context of a larger
     * physics simulation, registering Collidables will most likely be managed
     * by the simulator and will not need to be done manually.
     * 
     * @param collidable The object to add to the CollisionManager
     * @throws NullPointerException if collidable is null
     */
    public void add(Collidable collidable);

    /**
     * Remove a Collidable from the CollisionManager. If the Collidable is not
     * registered with this CollisionManager, then this is a no-op.
     * 
     * @param collidable The object to remove
     * @throws NullPointerException if collidable is null
     */
    public void remove(Collidable collidable);

    /**
     * Run the given CollisionCallback on every potentially intersecting pair of
     * registered Collidables in this CollisionManager. It is the manager's
     * responsibility to ensure that actually intersecting objects are reported
     * to the callback, but it is permissible to report non-intersecting objects
     * as well. The CollisionManager is not required to pay attention to the
     * collision groups and masks of candidate Collidable pairs.
     * 
     * @param callback The callback to invoke on each pair of potentially
     *            intersecting objects
     * @throws NullPointerException if callback is null
     */
    public void processCollisions(CollisionCallback callback);

    /**
     * Return the current CollisionAlgorithmProvider used to process potentially
     * intersecting Collidables. The returned CollisionAlgorithmProvider is provided to
     * CollisionCallbacks when {@link #processCollisions(CollisionCallback)} is
     * invoked.
     * 
     * @return The CollisionAlgorithmProvider
     */
    public CollisionAlgorithmProvider getCollisionHandler();

    /**
     * Set the CollisionAlgorithmProvider to use for subsequent invocations of
     * {@link #processCollisions(CollisionCallback)}.
     * 
     * @param handler The new CollisionAlgorithmProvider
     * @throws NullPointerException if handler is null
     */
    public void setCollisionHandler(CollisionAlgorithmProvider handler);
}
