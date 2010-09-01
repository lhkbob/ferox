package com.ferox.physics.collision;

/**
 * PairDetector is an interface encapsulating the narrow-phase of a collision
 * detection system. PairDetector implementations are responsible for computing
 * two vectors within world space. Each vector represents the closest point on
 * one {@link Collidable} to the other. Implementations must handle cases where
 * the two objects are intersecting each other, too.
 * 
 * @author Michael Ludwig
 */
public interface PairDetector {
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
     */
    public ClosestPair getClosestPair(Collidable objA, Collidable objB);
}
