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
package com.ferox.physics.collision;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Vector3;

/**
 * <p>
 * A Shape represents some solid volume within a pseudo-physical world. There
 * are numerous representations of a Shape, some of which are static and others
 * are dynamic. Shapes can be approximations to higher detailed geometries (as
 * often is the case in a game).
 * </p>
 * <p>
 * Collisions between Shapes and different shape types are not the
 * responsibility of Shape implementations. Instead {@link CollisionAlgorithm
 * CollisionAlgorithms} are implemented that support limited sets of shape
 * types.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Shape {
    /**
     * Return the local-space bounds approximation of this Shape. The returned
     * instance should be considered read-only and can be modified by the Shape
     * instance at any time. The computed bounds should take into account the
     * configured margin, plus an additional unspecified epsilon so that shapes
     * with touching bounds are correctly detected in the event of minor
     * floating point errors.
     * 
     * @return The Shape's local bounds
     */
    public @Const
    AxisAlignedBox getBounds();

    /**
     * Return the vector containing the inertia tensor for this shape, in its
     * local transform space. The returned instance should remain consistent
     * with any changes to the Shape.
     * 
     * @param mass The mass of this shape
     * @param result A vector to hold the computed tensor, can be null
     * @return The Shape's local inertia tensor in result if it was not null,
     *         otherwise a new vector
     */
    // FIXME if this isn't called that often, maybe make it create a new instance to simplify signature
    public Vector3 getInertiaTensor(double mass, Vector3 result);

    /**
     * <p>
     * Set the margin of padding around the shape. Every shape has a very small
     * amount of padding that extends its effective bounds. This is a mechanism
     * meant to help ease collision detection and response, but it means that
     * graphical models must be updated to correctly account for a margin.
     * </p>
     * <p>
     * The resulting shape, after applying the margin, is equivalent to
     * Minkowski sum of this shape and a sphere with a radius equal to the
     * margin.
     * </p>
     * 
     * @param margin The new margin, must be greater than or equal to 0
     * @throws IllegalArgumentException if margin is less than 0
     */
    public void setMargin(double margin);

    /**
     * @return Return the current margin for this shape, defaults to .05
     */
    public double getMargin();
}
