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
import com.ferox.physics.collision.shape.ConvexShape;

public class GjkEpaCollisionAlgorithm implements CollisionAlgorithm<ConvexShape, ConvexShape> {
    private static final int MAX_EPA_CHECKS = 4;

    @Override
    public ClosestPair getClosestPair(ConvexShape shapeA, @Const Matrix4 transA, ConvexShape shapeB,
                                      @Const Matrix4 transB) {
        ClosestPair p = null;

        MinkowskiShape shape = new MinkowskiShape(shapeA, transA, shapeB, transB);
        shape.setAppliedMargins(0);
        Vector3 guess = shape.getInitialGuess();

        Simplex simplex = GJK.evaluate(shape, guess);
        if (simplex != null && !simplex.isIntersection()) {
            p = shape.getClosestPair(simplex, null);
            if (p != null) {
                return p;
            }
        }

        for (int i = 1; i <= MAX_EPA_CHECKS; i++) {
            shape.setAppliedMargins(i);

            simplex = GJK.evaluate(shape, guess);
            if (simplex != null) {
                if (!simplex.isIntersection()) {
                    // unlikely but is a possible early escape
                    p = shape.getClosestPair(simplex, null);
                    if (p != null) {
                        return p;
                    }
                } else {
                    // run epa
                    p = EPA.evaluate(simplex);
                    if (p != null) {
                        return p;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Class<ConvexShape> getShapeTypeA() {
        return ConvexShape.class;
    }

    @Override
    public Class<ConvexShape> getShapeTypeB() {
        return ConvexShape.class;
    }
}
