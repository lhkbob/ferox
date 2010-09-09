package com.ferox.physics.collision;

/**
 * <p>
 * CollisionAlgorithm is an interface encapsulating the narrow-phase of a
 * collision detection system. CollisionAlgorithm implementations are
 * responsible for computing two vectors within world space. Each vector
 * represents the closest point on one {@link Collidable} to the other.
 * Implementations must handle cases where the two objects are intersecting each
 * other, too.
 * </p>
 * <p>
 * CollisionAlgorithms must be thread-safe because it is likely that code
 * relying on the algorithms will reuse the same instance. This should not be
 * difficult as each invocation of
 * {@link #getClosestPair(Collidable, Collidable)} should be independent.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface CollisionAlgorithm {
    /**
     * <p>
     * Compute the closest pair of points in world space between <tt>objA</tt>
     * and <tt>objB</tt>. If the implementation cannot determine a closest pair,
     * it should return null to indicate that the input was ill-conditioned.
     * When a non-null {@link ClosestPair} is returned, it means the two
     * Collidables are either guaranteed separated or intersecting.
     * </p>
     * <p>
     * If the pair's reported distance is negative, it means the two objects are
     * intersecting. {@link ClosestPair#getClosestPointOnA()} will return the
     * point on the surface of <tt>objA</tt> and
     * {@link ClosestPair#getClosestPointOnB()} will return the point on the
     * surface of <tt>objB</tt>. The contact normal between the two objects will
     * be from A to B (which is also why negative distance implies
     * intersection).
     * </p>
     * 
     * @param objA The first object involved in collision detection
     * @param objB The second object involved in collision detection
     * @return The closest pair of points on the surfaces of objA and objB, or
     *         null if no pair could be computed
     * @throws NullPointerException if objA or objB are null
     * @throws UnsupportedOperationException if the shape in objA or objB is unsupported
     */
    public ClosestPair getClosestPair(Collidable objA, Collidable objB);

    /**
     * Return whether or not this CollisionAlgorithm can support the given
     * Shape. If false is returned, the Collidable owning the provided Shape
     * should not be passed to {@link #getClosestPair(Collidable, Collidable)}.
     * 
     * @param s The shape to test for support
     * @return True or false if this algorithm can detect collisions with the
     *         given shape
     */
    public boolean isShapeSupported(Shape s);
}
