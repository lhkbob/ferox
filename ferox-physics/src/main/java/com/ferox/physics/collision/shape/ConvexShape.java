package com.ferox.physics.collision.shape;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.Shape;
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
public abstract class ConvexShape implements Shape {
    private float margin;
    private final ReadOnlyAxisAlignedBox bounds;
    
    public ConvexShape() {
        bounds = new ReadOnlyAxisAlignedBox();
        margin = .05f; // avoid setter so we don't call updateBounds()
    }

    /**
     * <p>
     * Compute and return the evaluation of this convex shape's support
     * function, on input <tt>v</tt>. The support should be stored and returned
     * in <tt>result</tt>. If result is null, a new vector should be created and
     * returned. The support function should not include the margin.
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
     * @return result, or a new vector, if result was null
     * @throws NullPointerException if v is null
     */
    public abstract Vector3 computeSupport(@Const Vector3 v, @Const Vector3 result);

    @Override
    public ReadOnlyAxisAlignedBox getBounds() {
        return bounds;
    }

    @Override
    public void setMargin(float margin) {
        if (margin < 0f)
            throw new IllegalArgumentException("Margin must be at least 0, not: " + margin);
        this.margin = margin;
        updateBounds();
    }

    @Override
    public float getMargin() {
        return margin;
    }

    /**
     * Recomputes the new bounds by evaluating the support function along the
     * six principal axis. Subclasses should call this any time their parameters
     * affecting the bounds are changed.
     */
    protected void updateBounds() {
        Vector3f d = new Vector3f();
        Vector3f t = new Vector3f();
        
        computeSupport(d.set(1f, 0f, 0f), t);
        float maxX = t.getX() + 2 * margin;
        computeSupport(d.set(-1f, 0f, 0f), t);
        float minX = t.getX() - 2 * margin;
        
        computeSupport(d.set(0f, 1f, 0f), t);
        float maxY = t.getY() + 2 * margin;
        computeSupport(d.set(0f, -1f, 0f), t);
        float minY = t.getY() - 2 * margin;
        
        computeSupport(d.set(0f, 0f, 1f), t);
        float maxZ = t.getZ() + 2 * margin;
        computeSupport(d.set(0f, 0f, -1f), t);
        float minZ = t.getZ() - 2 * margin;
        
        bounds.getMax().set(maxX, maxY, maxZ);
        bounds.getMin().set(minX, minY, minZ);
    }
}
