package com.ferox.physics.collision.algorithm;

import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.Shape;

/**
 * SwappingCollisionAlgorithm is a utility to swap the shape types that a true
 * collision algorithm can handle. For example, there might be a
 * SphereBoxCollisionAlgorithm. This class can be used to automatically create a
 * BoxSphereCollisionAlgorithm.
 * 
 * @author Michael Ludwig
 * @param <A> The first Shape type
 * @param <B> The second Shape type
 */
public class SwappingCollisionAlgorithm<A extends Shape, B extends Shape> implements CollisionAlgorithm<A, B> {
    private final CollisionAlgorithm<B, A> delegate;

    /**
     * Create a SwappingCollisionAlgorithm that wraps the given
     * CollisionAlgorithm. Any call to getClosestPair() on this algorithm will
     * delegate to <tt>toSwap</tt> except that A and B are swapped.
     * 
     * @param toSwap The algorithm to wrap
     * @throws NullPointerException if toSwap is null
     */
    public SwappingCollisionAlgorithm(CollisionAlgorithm<B, A> toSwap) {
        if (toSwap == null)
            throw new NullPointerException("Algorithm cannot be null");
        delegate = toSwap;
    }
    
    @Override
    public ClosestPair getClosestPair(A shapeA, ReadOnlyMatrix4f transA, 
                                      B shapeB, ReadOnlyMatrix4f transB) {
        ClosestPair original = delegate.getClosestPair(shapeB, transB, shapeA, transA);
        if (original == null)
            return null;
        
        // swap the points and contact normal
        return new ClosestPair(original.getClosestPointOnB(), original.getContactNormal().scale(-1f, null), 
                               original.getDistance());
    }
}
