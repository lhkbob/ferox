package com.ferox.physics.collision;

/**
 * CollisionAlgorithmProviders are responsible for providing CollisionAlgorithm
 * instances that can be used to compute accurate intersections between two
 * {@link Shape shapes} of the requested types.
 * 
 * @author Michael Ludwig
 */
public interface CollisionAlgorithmProvider {
    /**
     * <p>
     * Return a CollisionAlgorithm that can compute intersections between the
     * two shape types. If there is no registered algorithm supporting both
     * types, then null should be returned.
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
    public <A extends Shape, B extends Shape> CollisionAlgorithm<A, B> getAlgorithm(Class<A> shapeA,
                                                                                    Class<B> shapeB);
}
