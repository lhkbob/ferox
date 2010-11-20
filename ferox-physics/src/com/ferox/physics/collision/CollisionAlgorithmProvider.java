package com.ferox.physics.collision;

import com.ferox.physics.collision.algorithm.GjkEpaCollisionAlgorithm;
import com.ferox.physics.collision.algorithm.SphereSphereCollisionAlgorithm;
import com.ferox.physics.collision.shape.ConvexShape;

/**
 * CollisionAlgorithmProviders are responsible for providing CollisionAlgorithm
 * instances that can be used to compute accurate intersections between two
 * {@link Shape shapes} of the requested types. As with the other components of
 * the collision subsystem, CollisionAlgorithmProviders should be thread-safe.
 * 
 * @author Michael Ludwig
 */
public interface CollisionAlgorithmProvider {
    /**
     * <p>
     * Return a CollisionAlgorithm that can compute intersections between the
     * two shape types. If there is no registered algorithm supporting both
     * types, then null should be returned. When choosing an algorithm to
     * return, the providers should attempt to select an algorithm that best
     * matches the requested types. Additionally, algorithms registered later
     * should be preferred over older algorithms in the event of a tie.
     * </p>
     * <p>
     * As an example, the {@link GjkEpaCollisionAlgorithm} supports any two
     * {@link ConvexShape convex shapes}. Theoretically, it could then be used
     * to compute the intersection between two spheres. However, a dedicated
     * sphere-sphere algorithm (such as the
     * {@link SphereSphereCollisionAlgorithm}), should be selected over the
     * gjk-epa algorithm.
     * </p>
     * 
     * @param <A> The Shape type of the first class
     * @param <B> The Shape type of the second class
     * @param shapeA The Shape class for the first shape
     * @param shapeB The Shape class for the second shape
     * @return A CollisionAlgorithm that can compute intersections between
     *         Shapes of type A and type B, or null if no supporting algorithm
     *         is registered
     * @throws NullPointerException if shapeA or shapeB are null
     */
    public <A extends Shape, B extends Shape> CollisionAlgorithm<A, B> getAlgorithm(Class<A> shapeA, Class<B> shapeB);

    /**
     * Register a new CollisionAlgorithm with the CollisionAlgorithmProvider.
     * Implementations may choose to register a default set of algorithms as
     * well.
     * 
     * @param algorithm An algorithm to register
     * @throws NullPointerException if algorithm is null
     */
    public void register(CollisionAlgorithm<?, ?> algorithm);

    /**
     * Unregister any CollisionAlgorithm whose class equals the given type,
     * <tt>algorithmType</tt>.
     * 
     * @param algorithmType The algorithm type to remove
     * @throws NullPointerException if algorithmType is null
     */
    public void unregister(Class<? extends CollisionAlgorithm<?, ?>> algorithmType);
}
