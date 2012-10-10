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
 * Sphere is a ConvexShape that represents a mathematical sphere.
 * 
 * @author Michael Ludwig
 */
public class Sphere extends ConvexShape {
    private double radius;
    private double inertiaTensorPartial;

    /**
     * Create a new Sphere with the initial radius, ignoring the margin.
     * 
     * @see #setRadius(double)
     * @param radius The initial radius
     * @throws IllegalArgumentException if radius is less than or equal to 0
     */
    public Sphere(double radius) {
        setRadius(radius);
    }

    /**
     * Set the radius of the sphere. This does not include the margin that all
     * shapes are padded with.
     * 
     * @param radius The new radius, must be greater than 0
     * @throws IllegalArgumentException if radius is less than or equal to 0
     */
    public void setRadius(double radius) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Radius must be greater than 0, not: " + radius);
        }

        this.radius = radius;
        inertiaTensorPartial = 2.0 * radius * radius / 5.0;
        updateBounds();
    }

    /**
     * Return the current radius of the sphere, excluding the margin.
     * 
     * @return The radius of the sphere
     */
    public double getRadius() {
        return radius;
    }

    @Override
    public Vector3 computeSupport(@Const Vector3 v, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }
        return result.normalize(v).scale(radius);
    }

    @Override
    public Vector3 getInertiaTensor(double mass, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }
        double m = inertiaTensorPartial * mass;
        return result.set(m, m, m);
    }
}
