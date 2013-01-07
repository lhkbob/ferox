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
package com.ferox.physics.collision.shape;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

/**
 * AxisSweptShape represents a class of class of convex shapes that features a
 * dominant axis and a curve that is swept around that axis. Choosing different
 * dominant axis for the shape is equivalent to applying a rotation. Some
 * examples include cylinders, cones and capsules.
 * 
 * @author Michael Ludwig
 */
public abstract class AxisSweptShape extends ConvexShape {
    public static enum Axis {
        X,
        Y,
        Z
    }

    protected final Vector3 inertiaTensorPartial;
    protected final Axis dominantAxis;

    /**
     * Create an AxisSweptShape that uses the given dominant axis.
     * 
     * @param dominantAxis The dominant axis for the created shape
     * @throws NullPointerException if dominantAxis is null
     */
    public AxisSweptShape(Axis dominantAxis) {
        if (dominantAxis == null) {
            throw new NullPointerException("Axis cannot be null");
        }
        this.dominantAxis = dominantAxis;
        inertiaTensorPartial = new Vector3();
    }

    /**
     * @return The dominant axis of the shape
     */
    public Axis getDominantAxis() {
        return dominantAxis;
    }

    @Override
    public Vector3 getInertiaTensor(double mass, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }
        return result.scale(inertiaTensorPartial, mass);
    }

    /**
     * Return the sign of component of <tt>v</tt> matching the shape's dominant
     * axis. Thus, if the dominant axis was Z, it returns 1 of
     * <code>v.getZ()</code> is positive, and -1 if not.
     * 
     * @param v The input vector whose sign is queried
     * @return The sign of the dominant component of v
     * @throws NullPointerException if v is null
     */
    protected int sign(@Const Vector3 v) {
        switch (dominantAxis) {
        case X:
            return (v.x >= 0.0 ? 1 : -1);
        case Y:
            return (v.y >= 0.0 ? 1 : -1);
        case Z:
            return (v.z >= 0.0 ? 1 : -1);
        default:
            return 0;
        }
    }

    /**
     * Evaluate the "sigma" function of <tt>v</tt>. This is the same as the
     * projected distance of v to the dominant axis.
     * 
     * @param v The input vector evaluated by the sigma function
     * @return The projected distance of v to the dominant axis
     * @throws NullPointerException if v is null
     */
    protected double sigma(Vector3 v) {
        double c1, c2;
        switch (dominantAxis) {
        case X:
            c1 = v.y;
            c2 = v.z;
            break;
        case Y:
            c1 = v.x;
            c2 = v.z;
            break;
        case Z:
            c1 = v.x;
            c2 = v.y;
            break;
        default:
            return -1;
        }

        return Math.sqrt(c1 * c1 + c2 * c2);
    }
}
