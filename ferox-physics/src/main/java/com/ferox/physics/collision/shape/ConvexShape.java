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

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.Shape;
import com.ferox.physics.collision.algorithm.GjkEpaCollisionAlgorithm;

/**
 * ConvexShape is a Shape type that represents a convex hull. It itself is not a concrete implementation, but
 * instead, declares that all convex shapes can be described implicitly by a support function. This function
 * can be used by various collision algorithms, such as GJK or EPA, to report collisions between any correctly
 * implemented convex shape.
 *
 * @author Michael Ludwig
 * @see GjkEpaCollisionAlgorithm
 */
public abstract class ConvexShape implements Shape {
    private double margin;
    private final AxisAlignedBox bounds;

    public ConvexShape() {
        bounds = new AxisAlignedBox();
        margin = .05; // avoid setter so we don't call updateBounds()
    }

    /**
     * <p/>
     * Compute and return the evaluation of this convex shape's support function, on input <var>v</var>. The
     * support should be stored and returned in <var>result</var>. If result is null, a new vector should be
     * created and returned. The support function will not include the margin.
     * <p/>
     * The support of a convex shape is a function <var>Sc</var> that maps a vector to a point on the shape,
     * such that <code>dot(Sc, v)</code> maximizes <code>dot(x, v)</code> for all <var>x</var> on the shape's
     * surface.
     *
     * @param v      The support input
     * @param result A vector to contain the result
     *
     * @return result, or a new vector, if result was null
     *
     * @throws NullPointerException if v is null
     */
    public abstract Vector3 computeSupport(@Const Vector3 v, @Const Vector3 result);

    @Override
    public
    @Const
    AxisAlignedBox getBounds() {
        return bounds;
    }

    @Override
    public void setMargin(double margin) {
        if (margin < 0.0) {
            throw new IllegalArgumentException("Margin must be at least 0, not: " + margin);
        }
        this.margin = margin;
        updateBounds();
    }

    @Override
    public double getMargin() {
        return margin;
    }

    /**
     * Recomputes the new bounds by evaluating the support function along the six principal axis. Subclasses
     * should call this any time their parameters affecting the bounds are changed.
     */
    protected void updateBounds() {
        Vector3 d = new Vector3();
        Vector3 t = new Vector3();

        computeSupport(d.set(1, 0, 0), t);
        double maxX = t.x + margin;
        computeSupport(d.set(-1, 0, 0), t);
        double minX = t.x - margin;

        computeSupport(d.set(0, 1, 0), t);
        double maxY = t.y + margin;
        computeSupport(d.set(0, -1, 0), t);
        double minY = t.y - margin;

        computeSupport(d.set(0, 0, 1), t);
        double maxZ = t.z + margin;
        computeSupport(d.set(0f, 0f, -1f), t);
        double minZ = t.z - margin;

        bounds.max.set(maxX, maxY, maxZ);
        bounds.min.set(minX, minY, minZ);
    }
}
