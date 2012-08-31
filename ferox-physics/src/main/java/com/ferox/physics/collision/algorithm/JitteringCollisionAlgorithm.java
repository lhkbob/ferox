package com.ferox.physics.collision.algorithm;

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.Shape;

public class JitteringCollisionAlgorithm<A extends Shape, B extends Shape> implements CollisionAlgorithm<A, B> {
    private static final int MAX_JITTERS = 4;
    
    private final CollisionAlgorithm<A, B> wrapped;
    
    private final Vector3 jitter;
    private final Matrix4 jitteredTransform;
    
    public JitteringCollisionAlgorithm(CollisionAlgorithm<A, B> wrapped) {
        if (wrapped == null)
            throw new NullPointerException("CollisionAlgorithm cannot be null");
        this.wrapped = wrapped;
        jitter = new Vector3();
        jitteredTransform = new Matrix4();
    }
    
    @Override
    public ClosestPair getClosestPair(A shapeA, Matrix4 transA, B shapeB, Matrix4 transB) {
        ClosestPair unjittered = wrapped.getClosestPair(shapeA, transA, shapeB, transB);
        if (unjittered != null) {
            // no jittering required to find a solution
            return unjittered;
        } else {
            // apply random jitters to one transform
            for (int i = 0; i < MAX_JITTERS; i++) {
                jitter.set(Math.random() * shapeA.getMargin(), Math.random() * shapeA.getMargin(), Math.random() * shapeA.getMargin());
                
                jitteredTransform.set(transA);
                jitteredTransform.m03 += jitter.x;
                jitteredTransform.m13 += jitter.y;
                jitteredTransform.m23 += jitter.z;
                
                ClosestPair jittered = wrapped.getClosestPair(shapeA, jitteredTransform, shapeB, transB);
                if (jittered != null) {
                    // remove any jittering from the two closest points
                    // - since we translated the shape by jitter, the point in a
                    //   moves in the opposite direction of untranslating the shape
                    //   by jitter (which is just adding the jitter)
                    Vector3 newPointOnA = new Vector3(jittered.getClosestPointOnA()).add(jitter);
                    return new ClosestPair(newPointOnA, jittered.getContactNormal(), jittered.getDistance());
                }
            }
            
            // jittering did not find a solution
            return null;
        }
    }

    @Override
    public Class<A> getShapeTypeA() {
        return wrapped.getShapeTypeA();
    }

    @Override
    public Class<B> getShapeTypeB() {
        return wrapped.getShapeTypeB();
    }
}
