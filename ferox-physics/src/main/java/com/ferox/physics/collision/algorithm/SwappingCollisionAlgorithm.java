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

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.Shape;

/**
 * SwappingCollisionAlgorithm is a utility to swap the shape types that a true collision
 * algorithm can handle. For example, there might be a SphereBoxCollisionAlgorithm. This
 * class can be used to automatically create a BoxSphereCollisionAlgorithm.
 *
 * @param <A> The first Shape type
 * @param <B> The second Shape type
 *
 * @author Michael Ludwig
 */
public class SwappingCollisionAlgorithm<A extends Shape, B extends Shape>
        implements CollisionAlgorithm<A, B> {
    private final CollisionAlgorithm<B, A> delegate;

    /**
     * Create a SwappingCollisionAlgorithm that wraps the given CollisionAlgorithm. Any
     * call to getClosestPair() on this algorithm will delegate to <tt>toSwap</tt> except
     * that A and B are swapped.
     *
     * @param toSwap The algorithm to wrap
     *
     * @throws NullPointerException if toSwap is null
     */
    public SwappingCollisionAlgorithm(CollisionAlgorithm<B, A> toSwap) {
        if (toSwap == null) {
            throw new NullPointerException("Algorithm cannot be null");
        }
        delegate = toSwap;
    }

    @Override
    public ClosestPair getClosestPair(A shapeA, @Const Matrix4 transA, B shapeB,
                                      @Const Matrix4 transB) {
        ClosestPair original = delegate.getClosestPair(shapeB, transB, shapeA, transA);
        if (original == null) {
            return null;
        }

        // swap the points and contact normal
        return new ClosestPair(original.getClosestPointOnB(),
                               new Vector3().scale(original.getContactNormal(), -1.0),
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
