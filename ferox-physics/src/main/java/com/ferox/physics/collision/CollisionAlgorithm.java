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
package com.ferox.physics.collision;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;

/**
 * <p/>
 * CollisionAlgorithm is an interface encapsulating the narrow-phase of a collision
 * detection system. CollisionAlgorithm implementations are responsible for computing two
 * vectors within world space. Each vector represents the closest point on one {@link
 * CollisionBody} to the other. Implementations must handle cases where the two objects
 * are intersecting each other as well.
 *
 * @author Michael Ludwig
 */
public interface CollisionAlgorithm<A extends Shape, B extends Shape> {
    /**
     * <p/>
     * Compute the closest pair of points in world space between <tt>shapeA</tt> and
     * <tt>shapeB</tt>. If the implementation cannot determine a closest pair, it should
     * return null to indicate that the input was ill-conditioned. When a non-null {@link
     * ClosestPair} is returned, it means the two Collidables are either guaranteed
     * separated or intersecting.
     * <p/>
     * If the pair's reported distance is negative, it means the two objects are
     * intersecting. {@link ClosestPair#getClosestPointOnA()} will return the point on the
     * surface of <tt>shapeA</tt> and {@link ClosestPair#getClosestPointOnB()} will return
     * the point on the surface of <tt>shapeB</tt>. The contact normal between the two
     * objects will be from A to B (which is also why negative distance implies
     * intersection). The surface points and contact normal are in world space, as
     * determined by <tt>transA</tt> and <tt>transB</tt>.
     *
     * @param shapeA The Shape of the first object in the collision
     * @param transA The transform that represents the world-space orientation of shapeA
     * @param shapeB The Shape of the second object in the collision
     * @param transB The transform that represents the world-space orientation of shapeB
     *
     * @return The closest pair of points on the surfaces of shapeA and shapeB, or null if
     *         no pair could be computed
     *
     * @throws NullPointerException if any argument is null
     */
    public ClosestPair getClosestPair(A shapeA, @Const Matrix4 transA, B shapeB,
                                      @Const Matrix4 transB);

    /**
     * @return The Class representing the type A
     */
    public Class<A> getShapeTypeA();

    /**
     * @return The Class representing the type B
     */
    public Class<B> getShapeTypeB();
}
