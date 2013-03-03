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
import com.ferox.physics.collision.shape.Sphere;

/**
 * The SphereSphereCollisionAlgorithm is a CollisionAlgorithm optimized to handle
 * collision checks between two spheres.
 *
 * @author Michael Ludwig
 */
public class SphereSphereCollisionAlgorithm
        implements CollisionAlgorithm<Sphere, Sphere> {
    @Override
    public ClosestPair getClosestPair(Sphere shapeA, @Const Matrix4 transA, Sphere shapeB,
                                      @Const Matrix4 transB) {
        Vector3 ca = new Vector3(transA.m03, transA.m13, transA.m23);
        Vector3 cb = new Vector3(transB.m03, transB.m13, transB.m23);

        double ra = shapeA.getRadius() + shapeA.getMargin();
        double rb = shapeB.getRadius() + shapeB.getMargin();
        double dist = ca.distance(cb) - ra - rb;

        // FIXME: doesn't work if spheres are centered on each other
        Vector3 normal = new Vector3().sub(cb, ca);

        if (normal.lengthSquared() > .000001f) {
            normal.normalize();
            Vector3 pa = cb.addScaled(ca, ra, normal);
            return new ClosestPair(pa, normal, dist);
        } else {
            // happens when spheres are perfectly centered on each other
            if (ra < rb) {
                // sphere a is inside sphere b
                normal.set(0, 0, -1);
                return new ClosestPair(cb, normal, ra - rb);
            } else {
                // sphere b is inside sphere a
                normal.set(0, 0, 1);
                return new ClosestPair(cb, normal, rb - ra);
            }
        }
    }

    @Override
    public Class<Sphere> getShapeTypeA() {
        return Sphere.class;
    }

    @Override
    public Class<Sphere> getShapeTypeB() {
        return Sphere.class;
    }
}
