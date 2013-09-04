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
 * Box represents a three dimensional rectangular prism centered on its local origin with configurable
 * dimensions aligned with its local x, y, and z axis.
 *
 * @author Michael Ludwig
 */
public class Box extends ConvexShape {
    private final Vector3 localTensorPartial;
    private final Vector3 halfExtents;

    /**
     * Create a new Box with the given dimensions.
     *
     * @param xExtent The x dimension or width
     * @param yExtent The y dimension or height
     * @param zExtent The z dimension or depth
     */
    public Box(double xExtent, double yExtent, double zExtent) {
        localTensorPartial = new Vector3();
        halfExtents = new Vector3();

        setExtents(xExtent, yExtent, zExtent);
    }

    /**
     * @return Get the half extents of this box. It should not be mutated
     */
    @Const
    public Vector3 getHalfExtents() {
        return halfExtents;
    }

    /**
     * @return Get the full extents of this box. This creates a new vector
     */
    public Vector3 getExtents() {
        return new Vector3().scale(halfExtents, 2.0);
    }

    /**
     * Set the extents of this box to the given dimensions.
     *
     * @param width  The x dimension or width of the box
     * @param height The y dimension or height of the box
     * @param depth  The z dimension or depth of the box
     *
     * @throws IllegalArgumentException if any dimension is less than or equal to 0
     */
    public void setExtents(double width, double height, double depth) {
        if (width <= 0) {
            throw new IllegalArgumentException("Invalid width, must be greater than 0, not: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Invalid height, must be greater than 0, not: " + height);
        }
        if (depth <= 0) {
            throw new IllegalArgumentException("Invalid depth, must be greater than 0, not: " + depth);
        }

        halfExtents.set(width / 2.0, height / 2.0, depth / 2.0);
        localTensorPartial
                .set((height * height + depth * depth) / 12.0, (width * width + depth * depth) / 12.0,
                     (width * width + height * height) / 12.0);
        updateBounds();
    }

    /**
     * @return Get the width of this box
     */
    public double getWidth() {
        return halfExtents.x * 2.0;
    }

    /**
     * @return Get the height of this box
     */
    public double getHeight() {
        return halfExtents.y * 2.0;
    }

    /**
     * @return Get the depth of this box
     */
    public double getDepth() {
        return halfExtents.z * 2.0;
    }

    @Override
    public Vector3 computeSupport(@Const Vector3 v, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }

        double x = (v.x < 0.0 ? -halfExtents.x : halfExtents.x);
        double y = (v.y < 0.0 ? -halfExtents.y : halfExtents.y);
        double z = (v.z < 0.0 ? -halfExtents.z : halfExtents.z);

        return result.set(x, y, z);
    }

    @Override
    public Vector3 getInertiaTensor(double mass, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }
        return result.scale(localTensorPartial, mass);
    }
}
