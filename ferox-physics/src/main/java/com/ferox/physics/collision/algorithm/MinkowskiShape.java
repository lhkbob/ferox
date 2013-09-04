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
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.shape.ConvexShape;

/**
 * MinkowskiShape represents the minkowski difference between two convex shapes that each have an affine
 * transform applied to them. It contains functions to evaluate the combined support function of the minkowski
 * difference.
 *
 * @author Michael Ludwig
 */
public class MinkowskiShape {
    private static final double CONTACT_NORMAL_ACCURACY = .0001;

    private final Matrix3 rotationA;
    private final Vector3 translationA;
    private final ConvexShape shapeA;

    private final Matrix3 rotationB;
    private final Vector3 translationB;
    private final ConvexShape shapeB;

    private int numMargins;

    // temporary variables to reduce allocation costs
    private final Vector3 pointTemp; // used in computePointOnA/B(), getClosestPair(), and getSupport()
    private final Vector3 inSupportTemp; // used in support()

    public MinkowskiShape(ConvexShape shapeA, @Const Matrix4 transformA, ConvexShape shapeB,
                          @Const Matrix4 transformB) {
        // We could keep these as Matrix4's and use Vector3.transform, but
        // separating them into rotation and translation is more convenient here
        rotationA = new Matrix3().setUpper(transformA);
        translationA = new Vector3(transformA.m03, transformA.m13, transformA.m23);

        rotationB = new Matrix3().setUpper(transformB);
        translationB = new Vector3(transformB.m03, transformB.m13, transformB.m23);

        this.shapeA = shapeA;
        this.shapeB = shapeB;

        pointTemp = new Vector3();
        inSupportTemp = new Vector3();

        numMargins = 1;
    }

    public void setAppliedMargins(int num) {
        if (num < 0) {
            throw new IllegalArgumentException("Applied margin count must be at least 0, not: " + num);
        }
        numMargins = num;
    }

    public Vector3 getInitialGuess() {
        return new Vector3().sub(translationB, translationA);
    }

    public ClosestPair getClosestPair(Simplex simplex, @Const Vector3 zeroNormal) {
        Vector3 a = computePointOnA(simplex);
        Vector3 b = computePointOnB(simplex);

        double scale = 1.0; // no direction flip
        if (translationA.distanceSquared(b) < translationA.distanceSquared(a) ||
            translationB.distanceSquared(a) < translationB.distanceSquared(b)) {
            // shapes are intersecting, so flip everything
            scale = -1.0;
        }

        Vector3 normal = new Vector3().sub(b, a);
        double distance = normal.length() * scale;

        if (Math.abs(distance) < CONTACT_NORMAL_ACCURACY) {
            // special case for handling contacts that are very close
            if (zeroNormal != null) {
                normal.scale(zeroNormal, scale).normalize();
            } else {
                return null;
            }
        } else {
            // normalize and flip if intersecting
            normal.scale(1.0 / distance);
        }

        // update positions to be only a single margin away
        if (numMargins != 1) {
            // adjust a's point by moving N margins along the contact normal
            a.addScaled((1 - numMargins) * shapeA.getMargin(), normal);

            // compute how the contact depth changes
            double delta = scale * Math.abs(numMargins - 1) *
                           (shapeA.getMargin() + shapeB.getMargin());
            if ((numMargins == 0 && delta < 0.0) || (numMargins > 1 && delta > 0.0)) {
                // moving to one margin increases distance
                distance += delta;
            } else {
                // moving to one margin decreases distance
                distance -= delta;
            }
        }

        return new ClosestPair(a, normal, distance);
    }

    private Vector3 computePointOnA(Simplex simplex) {
        Vector3 a = new Vector3();
        for (int i = 0; i < simplex.getRank(); i++) {
            support(shapeA, rotationA, translationA, simplex.getInput(i), false, pointTemp);
            a.add(pointTemp.scale(simplex.getWeight(i)));
        }
        return a;
    }

    private Vector3 computePointOnB(Simplex simplex) {
        Vector3 b = new Vector3();
        for (int i = 0; i < simplex.getRank(); i++) {
            support(shapeB, rotationB, translationB, simplex.getInput(i), true, pointTemp);
            b.add(pointTemp.scale(simplex.getWeight(i)));
        }
        return b;
    }

    public Vector3 getSupport(@Const Vector3 dir, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }
        support(shapeA, rotationA, translationA, dir, false, result);
        support(shapeB, rotationB, translationB, dir, true, pointTemp);
        return result.sub(pointTemp);
    }

    private void support(ConvexShape shape, @Const Matrix3 r, @Const Vector3 t, @Const Vector3 d,
                         boolean negate, Vector3 result) {
        Vector3 transformedDir = (negate ? inSupportTemp.scale(d, -1.0) : inSupportTemp.set(d));
        transformedDir.mul(transformedDir, r);

        shape.computeSupport(transformedDir, result);
        if (numMargins > 0) {
            result.addScaled(numMargins * shape.getMargin(), transformedDir);
        }

        result.mul(r, result).add(t);
    }
}
