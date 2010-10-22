package com.ferox.physics.collision.shape;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.physics.collision.algorithm.GjkEpaCollisionAlgorithm;

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
     * in <tt>result</tt>. A NullPointerException is thrown if result is null.
     * This breaks the pattern of most methods that return the result vector
     * because the smoothness of the support function is returned by the
     * function instead.
     * </p>
     * <p>
     * The support of a convex shape is a function <tt>Sc</tt> that maps a
     * vector to a point on the shape, such that <code>dot(Sc, v)</code>
     * maximizes <code>dot(x, v)</code> for all <tt>x</tt> in the shape's
     * surface.
     * </p>
     * 
     * @param v The support input
     * @param result A vector to contain the result
     * @return True if the derivative of the support function is continuous at v
     *         (or true if the support function is smooth)
     * @throws NullPointerException if v or result are null
     */
    public boolean computeSupport(ReadOnlyVector3f v, MutableVector3f result);
}
