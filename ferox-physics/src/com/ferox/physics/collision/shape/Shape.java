package com.ferox.physics.collision.shape;

import com.ferox.math.MutableVector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.physics.collision.algorithm.CollisionAlgorithm;

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
 * CollisionAlgorithms} are implemented that support a small set of shape types.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Shape {
    /**
     * Return the local-space bounds approximation of this Shape. The returned
     * instance should be considered read-only and can be modified by the Shape
     * instance.
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
}
