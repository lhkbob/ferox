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

/**
 * Vector utilities used by the GJK and EPA algorithms.
 *
 * @author Michael Ludwig
 */
public class Util {
    /**
     * Compute the triple product over the three vectors.
     *
     * @param a First vector
     * @param b Second vector
     * @param c Third vector
     *
     * @return The triple product over a, b, and c
     *
     * @throws NullPointerException if a, b, or c are null
     */
    public static double tripleProduct(@Const Vector3 a, @Const Vector3 b, @Const Vector3 c) {
        return a.y * b.z * c.x + a.z * b.x * c.y - a.x * b.z * c.y - a.y * b.x * c.z + a.x * b.y * c.z -
               a.z * b.y * c.x;
    }

    /**
     * Compute the normal vector for the triangle formed by {@code va}, {@code vb}, {@code vc} and store it in
     * {@code result}.
     *
     * @param va     First vertex
     * @param vb     Second vertex
     * @param vc     Third vertex
     * @param result The output
     *
     * @return The result vector with normal, or a new vector if result is null
     *
     * @throws NullPointerException if va, vb, vc are null
     */
    public static Vector3 normal(@Const Vector3 va, @Const Vector3 vb, @Const Vector3 vc, Vector3 result) {
        // inline subtraction of 2 vectors
        double e1x = vb.x - va.x;
        double e1y = vb.y - va.y;
        double e1z = vb.z - va.z;

        double e2x = vc.x - va.x;
        double e2y = vc.y - va.y;
        double e2z = vc.z - va.z;

        if (result == null) {
            result = new Vector3();
        }

        // compute the cross-product of e1 and e2
        return result.set(e1y * e2z - e2y * e1z, e1z * e2x - e2z * e1x, e1x * e2y - e2x * e1y);
    }
}
