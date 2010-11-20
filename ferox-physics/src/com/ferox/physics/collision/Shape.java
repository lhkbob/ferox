package com.ferox.physics.collision;

import com.ferox.math.MutableVector3f;
import com.ferox.math.bounds.AxisAlignedBox;

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
 * CollisionAlgorithms} are implemented that support limited sets of shape types.
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
    public AxisAlignedBox getBounds();

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
    public MutableVector3f getInertiaTensor(float mass, MutableVector3f result);

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
    public void setMargin(float margin);
    
    /**
     * @return Return the current margin for this shape, defaults to .05
     */
    public float getMargin();
}
