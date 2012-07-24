package com.ferox.physics.collision;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;

/**
 * <p>
 * CollisionAlgorithm is an interface encapsulating the narrow-phase of a
 * collision detection system. CollisionAlgorithm implementations are
 * responsible for computing two vectors within world space. Each vector
 * represents the closest point on one {@link CollisionBody} to the other.
 * Implementations must handle cases where the two objects are intersecting each
 * other as well.
 * 
 * @author Michael Ludwig
 */
public interface CollisionAlgorithm<A extends Shape, B extends Shape> {
    /**
     * <p>
     * Compute the closest pair of points in world space between <tt>shapeA</tt>
     * and <tt>shapeB</tt>. If the implementation cannot determine a closest
     * pair, it should return null to indicate that the input was
     * ill-conditioned. When a non-null {@link ClosestPair} is returned, it
     * means the two Collidables are either guaranteed separated or
     * intersecting.
     * </p>
     * <p>
     * If the pair's reported distance is negative, it means the two objects are
     * intersecting. {@link ClosestPair#getClosestPointOnA()} will return the
     * point on the surface of <tt>shapeA</tt> and
     * {@link ClosestPair#getClosestPointOnB()} will return the point on the
     * surface of <tt>shapeB</tt>. The contact normal between the two objects
     * will be from A to B (which is also why negative distance implies
     * intersection). The surface points and contact normal are in world space,
     * as determined by <tt>transA</tt> and <tt>transB</tt>.
     * </p>
     * 
     * @param shapeA The Shape of the first object in the collision
     * @param transA The transform that represents the world-space orientation
     *            of shapeA
     * @param shapeB The Shape of the second object in the collision
     * @param transB The transform that represents the world-space orientation
     *            of shapeB
     * @return The closest pair of points on the surfaces of shapeA and shapeB,
     *         or null if no pair could be computed
     * @throws NullPointerException if any argument is null
     */
    public ClosestPair getClosestPair(A shapeA, @Const Matrix4 transA, 
                                      B shapeB, @Const Matrix4 transB);
    
    /**
     * @return The Class representing the type A
     */
    public Class<A> getShapeTypeA();
    
    /**
     * @return The Class representing the type B
     */
    public Class<B> getShapeTypeB();
}
