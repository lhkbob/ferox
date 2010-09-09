package com.ferox.physics.collision.narrow;

import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.ConvexShape;

/**
 * MinkowskiDifference represents the mathematical concept of a Minkowski sum
 * between two convex shapes. Because {@link GJK} and {@link EPA} are interested
 * in the difference between two shapes, the second shape in the sum is just the
 * negation of one convex shape. This implementation also automatically handles
 * converting the local support functions of {@link ConvexShape ConvexShapes}
 * into world space by applying the world transform of {@link Collidable
 * Collidables} involved.
 * 
 * @author Michael Ludwig
 */
public class MinkowskiDifference {
    private static final ThreadLocal<Vector3f> supportCache = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    private static final ThreadLocal<Vector3f> marginCache = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    private static final ThreadLocal<Vector4f> transformCache = new ThreadLocal<Vector4f>() {
        @Override
        protected Vector4f initialValue() { return new Vector4f(); }
    };
    
    private final ConvexShape shapeA;
    private final ConvexShape shapeB;
    
    private final ReadOnlyMatrix4f transA; // transforms a to world
    private final ReadOnlyMatrix4f transB; // transforms b to world
    
    private final float margin;

    /**
     * Create a new MinkowskiDifference between the two Collidable objects. The
     * MinkowskiDifference will automatically apply the world transforms of each
     * Collidable, and apply the configured margin. If the margin is less than
     * or equal to 0, no margin will be applied to the convex shapes of the
     * Collidables.
     * 
     * @param objA The first object involved in the minkowski difference
     * @param objB The second object involved in the minkowski difference
     * @param margin An automatic distance to shrink each convex shape by,
     *            locally, unless it is negative
     * @throws NullPointerException if objA or objB are null
     * @throws UnsupportedOperationException if objA or objB have a Shape that's
     *             not a ConvexShape
     */
    public MinkowskiDifference(Collidable objA, Collidable objB, float margin) {
        if (objA == null || objB == null)
            throw new NullPointerException("Collidable objects cannot be null");
        if (!(objA.getShape() instanceof ConvexShape))
            throw new UnsupportedOperationException("Collidable does not have a ConvexShape, instead it has: " + objA.getShape().getClass());
        if (!(objB.getShape() instanceof ConvexShape))
            throw new UnsupportedOperationException("Collidable does not have a ConvexShape, instead it has: " + objB.getShape().getClass());
        
        shapeA = (ConvexShape) objA.getShape();
        transA = objA.getWorldTransform();
        
        shapeB = (ConvexShape) objB.getShape();
        transB = objB.getWorldTransform();
        
        this.margin = margin;
    }

    /**
     * Compute and return the closest point on first Collidable, A, of this
     * MinkowskiDifference. The point is based off of the provided simplex,
     * which is assumed to have been formed from a successful evaluation of the
     * GJK or EPA algorithms. If <tt>useMargin</tt> is false, the returned
     * vector is in the coordinate space of the original Collidable, objA,
     * passed to the constructor of this difference. If it is true, it is in the
     * coordinate space of the Collidable after applying the margin to its
     * convex shape. If useMargin is true but the configured margin is negative,
     * no margin is used.</p>
     * <p>
     * The closet point is stored within the vector, <tt>result</tt>. If result
     * is null a new vector is created and returned. Otherwise, result is
     * updated and returned without allocating a new vector.
     * </p>
     * 
     * @param simplex The Simplex used to construct the closest point in A
     * @param useMargin Whether or not to apply a margin to A's convex shape
     * @param result The vector to contain the result, or null
     * @return The closest point on A, stored in result, or a new vector if
     *         result was null
     * @throws NullPointerException if simplex is null
     */
    public Vector3f getClosestPointA(Simplex simplex, boolean useMargin, Vector3f result) {
        if (simplex == null)
            throw new NullPointerException("Simplex cannot be null");
        Vector3f t = supportCache.get();
        
        if (result == null)
            result = new Vector3f();
        result.set(0f, 0f, 0f);
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            getAffineSupport(shapeA, transA, simplex.getSupportInput(i), useMargin, t)
                            .scaleAdd(simplex.getWeight(i), result, result);
        }
        
        return result;
    }

    /**
     * As {@link #getClosestPointA(Simplex, boolean, Vector3f)} but the closest
     * point is the point on the second object, B, passed to the constructor of
     * this MinkowskiDifference.
     * 
     * @param simplex The Simplex used to construct the closest point in B
     * @param useMargin Whether or not to apply a margin to B's convex shape
     * @param result The vector to contain the result, or null
     * @return The closest point on B, stored in result, or a new vector if
     *         result was null
     * @throws NullPointerException if simplex is null
     */
    public Vector3f getClosestPointB(Simplex simplex, boolean useMargin, Vector3f result) {
        if (simplex == null)
            throw new NullPointerException("Simplex cannot be null");
        Vector3f t = supportCache.get();
        
        if (result == null)
            result = new Vector3f();
        result.set(0f, 0f, 0f);
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            getAffineSupport(shapeB, transB, simplex.getSupportInput(i).scale(-1f, t), useMargin, t)
                            .scaleAdd(simplex.getWeight(i), result, result);
        }
        
        return result;
    }

    /**
     * <p>
     * Compute the support function of this MinkowskiDifference. The support of
     * a minkowski difference is <code>Sa(d) - Sb(-d)</code> where <tt>Sa</tt>
     * and <tt>Sb</tt> are the support functions of the convex shapes be
     * subtracted from each other (after applying their world transforms). If
     * the MinkowskiDifference is configured to have a margin, the margin will
     * be applied when evaluating the support function.
     * </p>
     * <p>
     * The support is stored in result if result is non-null. If result is null
     * a new vector is created and returned instead. When result is non-null,
     * result is returned.
     * </p>
     * 
     * @param d The input to the support function
     * @param result The result to store the support in, may be null
     * @return The support, stored in result or a new vector if result was null
     * @throws NullPointerException if d is null
     */
    public Vector3f getSupport(ReadOnlyVector3f d, Vector3f result) {
        Vector3f t = supportCache.get();
        
        Vector3f sa = getAffineSupport(shapeA, transA, d, true, result);
        Vector3f sb = getAffineSupport(shapeB, transB, d.scale(-1f, t), true, t);
        return sa.sub(sb);
    }
    
    private Vector3f getAffineSupport(ConvexShape shape, ReadOnlyMatrix4f t, ReadOnlyVector3f d, boolean useMargin, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        
        Vector3f m = marginCache.get();
        Vector4f s = transformCache.get();
        
        // first step is to transform d by the transpose of the upper 3x3
        // we do this by wrapping d in a 4-vector and setting w = 0
        s.set(d.getX(), d.getY(), d.getZ(), 0f);
        t.mulPre(s);
        
        // second step is to compute the actual support
        result.set(s.getX(), s.getY(), s.getZ());
        shape.computeSupport(result, result);
        if (useMargin && margin > 0f)
            result.sub(result.normalize(m).scale(margin));
        
        // then transform that by the complete affine transform
        s.set(result.getX(), result.getY(), result.getZ(), 1f);
        t.mul(s);
        
        return result.set(s.getX(), s.getY(), s.getZ());
    }
}