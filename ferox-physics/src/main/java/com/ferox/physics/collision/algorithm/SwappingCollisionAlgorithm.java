package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
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
        if (toSwap == null) {
            throw new NullPointerException("Algorithm cannot be null");
        }
        delegate = toSwap;
    }

    @Override
    public ClosestPair getClosestPair(A shapeA, @Const Matrix4 transA,
                                      B shapeB, @Const Matrix4 transB) {
        ClosestPair original = delegate.getClosestPair(shapeB, transB, shapeA, transA);
        if (original == null) {
            return null;
        }

        // swap the points and contact normal
        return new ClosestPair(original.getClosestPointOnB(), new Vector3().scale(original.getContactNormal(), -1.0),
                               original.getDistance());
    }

    @Override
    public Class<A> getShapeTypeA() {
        return delegate.getShapeTypeB();
    }

    @Override
    public Class<B> getShapeTypeB() {
        return delegate.getShapeTypeA();
    }
}
