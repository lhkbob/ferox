package com.ferox.physics.collision;


/**
 * <p>
 * CollisionCallbacks are custom callbacks that are invoked by a
 * CollisionManager on every potentially intersecting object registered with the
 * CollisionManager. The set of potentially colliding objects is a superset of
 * the set of intersecting objects. A CollisionAlgorithmProvider is provided to the
 * callback to distinguish between true intersecting objects and those that
 * aren't.
 * </p>
 * <p>
 * A CollisionManager implementation is allowed to invoke a callback on multiple
 * threads or from a different thread than the one calling
 * {@link CollisionManager#processCollisions(CollisionCallback)}, so it is
 * important for CollisionCallback implementations to be thread-safe.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface CollisionCallback {
    /**
     * <p>
     * Process the given pair of Collidables. This is invoked by
     * CollisionManagers when their processCollisions() method is called with
     * this Callback. The pair, <tt>objA</tt> and <tt>objB</tt>, may or may not
     * be actually intersecting. The provided CollisionAlgorithmProvider can be
     * used to generate {@link ClosestPair ClosestPairs} containing correct
     * intersecting information.
     * </p>
     * <p>
     * The ordering of the Collidables may not be consistent and is meaningless,
     * e.g. (<tt>objA</tt>, <tt>objB</tt>) should processed the same as (
     * <tt>objB</tt>, <tt>objA</tt>).
     * </p>
     * 
     * @param objA The first object in the pair
     * @param objB The second object in the pair
     * @param handler The currently configured CollisionAlgorithmProvider of the
     *            CollisionManager
     * @throws NullPointerException if any argument is null
     */
    public void process(Collidable objA, Collidable objB, CollisionAlgorithmProvider handler);
}
