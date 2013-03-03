/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.physics.collision.algorithm;

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.Shape;

public class JitteringCollisionAlgorithm<A extends Shape, B extends Shape>
        implements CollisionAlgorithm<A, B> {
    private static final int MAX_JITTERS = 4;

    private final CollisionAlgorithm<A, B> wrapped;

    private final Vector3 jitter;
    private final Matrix4 jitteredTransform;

    public JitteringCollisionAlgorithm(CollisionAlgorithm<A, B> wrapped) {
        if (wrapped == null) {
            throw new NullPointerException("CollisionAlgorithm cannot be null");
        }
        this.wrapped = wrapped;
        jitter = new Vector3();
        jitteredTransform = new Matrix4();
    }

    @Override
    public ClosestPair getClosestPair(A shapeA, Matrix4 transA, B shapeB,
                                      Matrix4 transB) {
        ClosestPair unjittered = wrapped.getClosestPair(shapeA, transA, shapeB, transB);
        if (unjittered != null) {
            // no jittering required to find a solution
            return unjittered;
        } else {
            // apply random jitters to one transform
            for (int i = 0; i < MAX_JITTERS; i++) {
                jitter.set(Math.random() * shapeA.getMargin(),
                           Math.random() * shapeA.getMargin(),
                           Math.random() * shapeA.getMargin());

                jitteredTransform.set(transA);
                jitteredTransform.m03 += jitter.x;
                jitteredTransform.m13 += jitter.y;
                jitteredTransform.m23 += jitter.z;

                ClosestPair jittered = wrapped
                        .getClosestPair(shapeA, jitteredTransform, shapeB, transB);
                if (jittered != null) {
                    // remove any jittering from the two closest points
                    // - since we translated the shape by jitter, the point in a
                    //   moves in the opposite direction of untranslating the shape
                    //   by jitter (which is just adding the jitter)
                    Vector3 newPointOnA = new Vector3(jittered.getClosestPointOnA())
                            .add(jitter);
                    return new ClosestPair(newPointOnA, jittered.getContactNormal(),
                                           jittered.getDistance());
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
