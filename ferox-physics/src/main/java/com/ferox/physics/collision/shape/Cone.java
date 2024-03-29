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
 * Cone is a convex shape implementation that represents a cone. It has two parameters, the height of the cone
 * and the radius of its base. The orientation of the cylinder is chosen by its dominant axis. The base of the
 * cone is on the negative half-space along the axis, and the tip is in the positive half-space.
 *
 * @author Michael Ludwig
 */
public class Cone extends AxisSweptShape {
    private double halfHeight;
    private double baseRadius;

    /**
     * Create a new Cone with the given radius and height aligned on the Z axis.
     *
     * @param baseRadius The cone's base radius
     * @param height     The height of the cone
     */
    public Cone(double baseRadius, double height) {
        this(baseRadius, height, Axis.Z);
    }

    /**
     * Create a new Cone with the given radius, height, and axis of revolution.
     *
     * @param baseRadius   The cone's base radius
     * @param height       The height of the cone
     * @param dominantAxis The dominant axis that the cone is aligned with
     */
    public Cone(double baseRadius, double height, Axis dominantAxis) {
        super(dominantAxis);
        setBaseRadius(baseRadius);
        setHeight(height);
    }

    /**
     * @return The height of the cone
     */
    public double getHeight() {
        return 2.0 * halfHeight;
    }

    /**
     * @return The base radius of the cone
     */
    public double getBaseRadius() {
        return baseRadius;
    }

    /**
     * Set the height of the cone
     *
     * @param height The new height
     *
     * @throws IllegalArgumentException if height is less than or equal to 0
     */
    public void setHeight(double height) {
        if (height <= 0f) {
            throw new IllegalArgumentException("Height must be greater than 0, not: " + height);
        }
        this.halfHeight = height / 2.0;
        update();
    }

    /**
     * Set the base radius of the cone
     *
     * @param radius The new radius
     *
     * @throws IllegalArgumentException if radius is less than or equal to 0
     */
    public void setBaseRadius(double radius) {
        if (radius <= 0f) {
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        }
        baseRadius = radius;
        update();
    }

    @Override
    public Vector3 computeSupport(@Const Vector3 v, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }

        double sin = baseRadius / Math.sqrt(baseRadius * baseRadius + 4 * halfHeight * halfHeight);
        switch (dominantAxis) {
        case X:
            if (v.x <= v.length() * sin) {
                double sigma = sigma(v);
                if (sigma <= 0.0) {
                    result.set(-halfHeight, 0.0, 0.0);
                } else {
                    result.set(-halfHeight, baseRadius / sigma * v.y, baseRadius / sigma * v.z);
                }
            } else {
                result.set(halfHeight, 0.0, 0.0);
            }
            break;
        case Y:
            if (v.y <= v.length() * sin) {
                double sigma = sigma(v);
                if (sigma <= 0.0) {
                    result.set(0.0, -halfHeight, 0.0);
                } else {
                    result.set(baseRadius / sigma * v.x, -halfHeight, baseRadius / sigma * v.z);
                }
            } else {
                result.set(0.0, halfHeight, 0.0);
            }
            break;
        case Z:
            if (v.z <= v.length() * sin) {
                double sigma = sigma(v);
                if (sigma <= 0.0) {
                    result.set(-0.0, 0.0, -halfHeight);
                } else {
                    result.set(baseRadius / sigma * v.x, baseRadius / sigma * v.y, -halfHeight);
                }
            } else {
                result.set(0.0, 0.0, halfHeight);
            }
            break;
        }

        return result;
    }

    private void update() {
        double m1 = 4.0 / 10.0 * halfHeight * halfHeight + 3.0 / 20.0 * baseRadius * baseRadius;
        double m2 = 3.0 / 10.0 * baseRadius * baseRadius;

        switch (dominantAxis) {
        case X:
            inertiaTensorPartial.set(m2, m1, m1);
            break;
        case Y:
            inertiaTensorPartial.set(m1, m2, m1);
            break;
        case Z:
            inertiaTensorPartial.set(m1, m1, m2);
            break;
        }
        updateBounds();
    }
}
