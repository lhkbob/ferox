package com.ferox.physics.collision;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.narrow.GjkEpaCollisionAlgorithm;

/**
 * ConvexShape is a Shape type that represents a convex hull. It itself is not a
 * concrete implementation, but instead, declares that all convex shapes can be
 * described implicitly by a support function. This function can be used by
 * various collision algorithms, such as GJK or EPA, to report collisions
 * between any correctly implemented convex shape.
 * 
 * @author Michael Ludwig
 * @see GjkEpaCollisionAlgorithm
 */
public interface ConvexShape extends Shape {
    /**
     * <p>
     * Compute and return the evaluation of this convex shape's support
     * function, on input <tt>v</tt>. The support should be stored and returned
     * in <tt>result</tt> if <tt>result</tt> is not null. If <tt>result</tt> is
     * null, a new Vector3f is created and returned instead.
     * </p>
     * <p>
     * The support of a convex shape is a function <tt>Sc</tt> that maps a
     * vector to a point on the shape, such that <code>dot(Sc, v)</code>
     * maximizes <code>dot(x, v)</code> for all <tt>x</tt> in the shape's
     * surface.
     * </p>
     * 
     * @param v The support input
     * @param result A vector to contain the result, may be null
     * @return The computed support in result, or a new vector if result is null
     * @throws NullPointerException if v is null
     */
    public Vector3f computeSupport(ReadOnlyVector3f v, Vector3f result);
}
