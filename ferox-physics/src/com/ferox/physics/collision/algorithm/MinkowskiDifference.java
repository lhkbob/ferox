package com.ferox.physics.collision.algorithm;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.shape.ConvexShape;

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
    private static final float CONTACT_NORMAL_ACCURACY = .0001f;
    
    private final ConvexShape shapeA;
    private final ConvexShape shapeB;
    
    private final ReadOnlyMatrix4f transA; // transforms a to world
    private final ReadOnlyMatrix4f transB; // transforms b to world
    
    private int appliedMargins;
    
    // final variables to reuse during computations, should be faster than thread-local
    private final Vector3f supportCache;
    private final Vector3f dirCache;
    private final Vector4f transformCache;

    /**
     * Create a new MinkowskiDifference between the two Shape objects. The
     * MinkowskiDifference will automatically apply the provided world
     * transforms associated with each shape, and take into account the margins
     * of the Shapes.
     * 
     * @param shapeA The first shape involved in the minkowski difference
     * @param transA The first shape's world transform
     * @param shapeB The second object involved in the minkowski difference
     * @param transB The second shape's world transform
     * @throws NullPointerException if any arguments are null
     */
    public MinkowskiDifference(ConvexShape shapeA, ReadOnlyMatrix4f transA,
                               ConvexShape shapeB, ReadOnlyMatrix4f transB) {
        if (shapeA == null || shapeB == null || transA == null || transB == null)
            throw new NullPointerException("Arguments cannot be null");
        
        supportCache = new Vector3f();
        dirCache = new Vector3f();
        transformCache = new Vector4f();
        
        this.shapeA = shapeA;
        this.shapeB = shapeB;
        this.transA = transA;
        this.transB = transB;
        
        appliedMargins = 1;
    }

    /**
     * Set the number of times the MinkowskiDifference will apply each
     * ConvexShape's margin when
     * {@link #getSupport(ReadOnlyVector3f, MutableVector3f)} is called. This
     * number does not affect the result of
     * {@link #getClosestPair(Simplex, ReadOnlyVector3f)}, which always applies
     * a single margin for each shape.
     * 
     * @param num The number of margin applications
     * @throws IllegalArgumentException if num is less than 0
     */
    public void setNumAppliedMargins(int num) {
        if (num < 0)
            throw new IllegalArgumentException("Number of applied margins must be at least 0, not: " + num);
        appliedMargins = num;
    }

    /**
     * Build a closest pair from the given Simplex and this MinkowskiDifference.
     * The specified simplex should have been built by the GJK or EPA algorithms
     * using this MinkowskiDifference. The returned pair will always applied a
     * single margin to each ConvexShape, regardless of
     * {@link #setNumAppliedMargins(int)}.
     * 
     * @param simplex The simplex used to build the closest pair
     * @param zeroNormal An optional normal vector to use when the closest
     *            pair's distance is 0
     * @return A ClosestPair formed by the simplex, or null if it could not be
     *         computed
     * @throws NullPointerException if simplex is null
     */
    public ClosestPair getClosestPair(Simplex simplex, ReadOnlyVector3f zeroNormal) {
        ReadOnlyVector3f pa = transA.getCol(3).getAsVector3f();
        ReadOnlyVector3f pb = transB.getCol(3).getAsVector3f();
        
        Vector3f ma = getClosestPointA(simplex);
        Vector3f mb = getClosestPointB(simplex);
        
        return constructPair(ma, mb, pa, pb, zeroNormal);
    }
    
    private ClosestPair constructPair(MutableVector3f wA, MutableVector3f wB, 
                                      ReadOnlyVector3f pA, ReadOnlyVector3f pB,
                                      ReadOnlyVector3f zeroNormal) {
        boolean intersecting = (pA.distanceSquared(wB) < pA.distanceSquared(wA)) && 
                               (pB.distanceSquared(wA) < pB.distanceSquared(wB));
        MutableVector3f normal = wB.sub(wA); // wB becomes the normal here
        float distance = normal.length() * (intersecting ? -1f : 1f);

        if (Math.abs(distance) < CONTACT_NORMAL_ACCURACY) {
            // special case for very close contact points, where the normal might become inaccurate
            if (zeroNormal != null) {
                zeroNormal.normalize(normal);
                if (intersecting)
                    normal.scale(-1f);
            } else
                return null;
        } else {
            // normalize, and possibly flip the normal based on intersection
            normal.scale(1f / distance);
        }
        
        if (appliedMargins != 1) {
            int scale = Math.abs(appliedMargins - 1);
            float distDelta = scale * shapeA.getMargin() + scale * shapeB.getMargin();
            if (intersecting)
                distDelta *= -1f;
            
            normal.scaleAdd(-(appliedMargins - 1) * shapeA.getMargin(), wA, wA);
            if ((appliedMargins == 0 && intersecting) || (appliedMargins > 1 && !intersecting)) {
                // moving to one margin increases distance
                distance += distDelta;
            } else {
                // moving to one margin decreases distance
                distance -= distDelta;
            }
        }

        return new ClosestPair(wA, normal, distance);
    }


    private Vector3f getClosestPointA(Simplex simplex) {
        Vector3f result = new Vector3f();
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            getAffineSupport(shapeA, transA, simplex.getVertex(i).getInputVector(), supportCache);
            supportCache.scaleAdd(simplex.getVertex(i).getWeight(), result, result);
        }
        return result;
    }

    private Vector3f getClosestPointB(Simplex simplex) {
        Vector3f result = new Vector3f();
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            getAffineSupport(shapeB, transB, simplex.getVertex(i).getInputVector().scale(-1f, supportCache), supportCache);
            supportCache.scaleAdd(simplex.getVertex(i).getWeight(), result, result);
        }
        return result;
    }

    /**
     * <p>
     * Compute the support function of this MinkowskiDifference. The support of
     * a minkowski difference is <code>Sa(d) - Sb(-d)</code> where <tt>Sa</tt>
     * and <tt>Sb</tt> are the support functions of the convex shapes after
     * applying their world transforms. Each ConvexShape will have their
     * configured margins applied a number of times depending on the last call
     * to {@link #setNumAppliedMargins(int)}.
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
    public MutableVector3f getSupport(ReadOnlyVector3f d, MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        
        getAffineSupport(shapeA, transA, d, result);
        getAffineSupport(shapeB, transB, d.scale(-1f, supportCache), supportCache);
        return result.sub(supportCache);
    }
    
    private void getAffineSupport(ConvexShape shape, ReadOnlyMatrix4f t, ReadOnlyVector3f d, MutableVector3f result) {
        // first step is to transform d by the transpose of the upper 3x3
        // we do this by wrapping d in a 4-vector and setting w = 0
        transformCache.set(d.getX(), d.getY(), d.getZ(), 0f);
        t.mulPre(transformCache);
        
        // second step is to compute the actual support
        result.set(transformCache.getX(), transformCache.getY(), transformCache.getZ());
        dirCache.set(result);
        
        shape.computeSupport(result, result);
        
        if (appliedMargins > 0) {
            // apply a number of margin offsets, as if a sphere is added to the convex shape
            dirCache.scaleAdd(appliedMargins * shape.getMargin(), result, result);
        }
        
        // then transform that by the complete affine transform, so w = 1
        transformCache.set(result.getX(), result.getY(), result.getZ(), 1f);
        t.mul(transformCache);
        result.set(transformCache.getX(), transformCache.getY(), transformCache.getZ());
    }
}