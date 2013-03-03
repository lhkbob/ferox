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
import com.ferox.math.Vector3;

public class GJK {
    private static final int GJK_MAX_ITERATIONS = 128;
    private static final double GJK_MIN_DISTANCE = .00001;
    private static final double GJK_DUPLICATE_EPS = .0001;
    private static final double GJK_ACCURACY = .00001;

    public static int numGJK = 0;

    public static Simplex evaluate(MinkowskiShape shape, @Const Vector3 guess) {
        numGJK++;

        Simplex simplex = new Simplex(shape);
        Vector3 ray = new Vector3(guess);
        if (ray.lengthSquared() < GJK_MIN_DISTANCE * GJK_MIN_DISTANCE) {
            ray.set(1, 0, 0); // arbitrary guess
        }

        double alpha = 0.0;

        // add first vertex
        ray.set(simplex.addVertex(guess, true));
        simplex.setWeight(0, 1.0);

        Vector3[] oldSupports = new Vector3[] {
                new Vector3(ray), new Vector3(ray), new Vector3(ray), new Vector3(ray)
        };
        int lastSupportIndex = 0;
        for (int i = 0; i < GJK_MAX_ITERATIONS; i++) {
            double rayLength = ray.length();
            if (rayLength < GJK_MIN_DISTANCE) {
                simplex.setIntersection(true);
                return simplex;
            }

            // add another vertex
            Vector3 support = simplex.addVertex(ray, true);

            // check for duplicates
            for (int j = 0; j < oldSupports.length; j++) {
                if (support.epsilonEquals(oldSupports[j], GJK_DUPLICATE_EPS)) {
                    // found a duplicate so terminate after removing duplicate
                    simplex.discardLastVertex();
                    return simplex;
                }
            }

            lastSupportIndex = (lastSupportIndex + 1) & 3;
            oldSupports[lastSupportIndex].set(support);

            // check termination condition
            alpha = Math.max(ray.dot(support) / rayLength, alpha);
            if ((rayLength - alpha) - (GJK_ACCURACY * rayLength) <= 0.0) {
                // error threshold is small enough
                simplex.discardLastVertex();
                return simplex;
            }

            // reduce for next iteration
            if (simplex.reduce()) {
                if (simplex.getRank() == Simplex.MAX_RANK) {
                    // it's a valid simplex, but represents an intersection
                    simplex.setIntersection(true);
                    return simplex;
                }

                // compute next guess
                ray.set(0.0, 0.0, 0.0);
                for (int j = 0; j < simplex.getRank(); j++) {
                    ray.addScaled(simplex.getWeight(j), simplex.getVertex(j));
                }
            } else {
                simplex.discardLastVertex();
                return simplex;
            }
        }

        // if we've reached here, it's invalid
        return null;
    }
}
