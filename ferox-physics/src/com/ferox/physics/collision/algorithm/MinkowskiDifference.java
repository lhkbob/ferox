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
    private static final float CONTACT_NORMAL_ACCURACY = .001f;
    
    private final ConvexShape shapeA;
    private final ConvexShape shapeB;
    
    private final ReadOnlyMatrix4f transA; // transforms a to world
    private final ReadOnlyMatrix4f transB; // transforms b to world
    
    private final float margin;
    private boolean ignoreMarginOnEval;

    /**
     * Create a new MinkowskiDifference between the two Shape objects. The
     * MinkowskiDifference will automatically apply the provided world transforms
     * associated with each shape, and apply the configured scale. If the scale is less than
     * or equal to 0, no scale will be applied to the convex shapes.
     * 
     * @param shapeA The first shape involved in the minkowski difference
     * @param transA The first shape's world transform
     * @param shapeB The second object involved in the minkowski difference
     * @param transB The second shape's world transform
     * @param scale An automatic scale factor to shrink each convex shape by,
     *            locally, unless it is negative
     * @throws NullPointerException if any arguments are null
     */
    public MinkowskiDifference(ConvexShape shapeA, ReadOnlyMatrix4f transA,
                               ConvexShape shapeB, ReadOnlyMatrix4f transB, float scale) {
        if (shapeA == null || shapeB == null || transA == null || transB == null)
            throw new NullPointerException("Arguments cannot be null");
        
        this.shapeA = shapeA;
        this.shapeB = shapeB;
        this.transA = transA;
        this.transB = transB;
        
        this.margin = scale;
        ignoreMarginOnEval = false;
    }
    
    public void setIgnoreMargin(boolean ignore) {
        ignoreMarginOnEval = ignore;
    }
    
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
            if (zeroNormal != null)
                zeroNormal.normalize(normal);
            return null;
        } else {
            // normalize, and possibly flip the normal based on intersection
            normal.scale(1f / distance);
        }
        
        if (ignoreMarginOnEval) {
            distance -= 2f * margin;
            normal.scaleAdd(margin, wA, wA); 
        }

        return new ClosestPair(wA, normal, distance);
    }


    private Vector3f getClosestPointA(Simplex simplex) {
        Vector3f result = new Vector3f();
        Vector3f t = supportCache.get();
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            getAffineSupport(shapeA, transA, simplex.getSupportInput(i), t);
            t.scaleAdd((float) simplex.getWeight(i), result, result);
        }
        return result;
    }

    private Vector3f getClosestPointB(Simplex simplex) {
        Vector3f result = new Vector3f();
        Vector3f t = supportCache.get();
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            getAffineSupport(shapeB, transB, simplex.getSupportInput(i).scale(-1f, t), t);
            t.scaleAdd((float) simplex.getWeight(i), result, result);
        }
        return result;
    }

    /**
     * <p>
     * Compute the support function of this MinkowskiDifference. The support of
     * a minkowski difference is <code>Sa(d) - Sb(-d)</code> where <tt>Sa</tt>
     * and <tt>Sb</tt> are the support functions of the convex shapes after
     * applying their world transforms. If the MinkowskiDifference is configured
     * to have a scale, the scale will be applied when evaluating the local
     * support function.
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
        Vector3f sb = supportCache.get();
        
        getAffineSupport(shapeA, transA, d, result);
        getAffineSupport(shapeB, transB, d.scale(-1f, sb), sb);
        return result.sub(sb);
    }
    
    private void getAffineSupport(ConvexShape shape, ReadOnlyMatrix4f t, ReadOnlyVector3f d, MutableVector3f result) {
        Vector4f s = transformCache.get();
        
        // first step is to transform d by the transpose of the upper 3x3
        // we do this by wrapping d in a 4-vector and setting w = 0
        s.set(d.getX(), d.getY(), d.getZ(), 0f);
        t.mulPre(s);
        
        // second step is to compute the actual support
        result.set(s.getX(), s.getY(), s.getZ());
        shape.computeSupport(result, result);
        
        // apply scale factor, and check if input is a vertex
        if (!ignoreMarginOnEval)
            d.scaleAdd(margin, result, result);
        
        // then transform that by the complete affine transform, so w = 1
        s.set(result.getX(), result.getY(), result.getZ(), 1f);
        t.mul(s);
        result.set(s.getX(), s.getY(), s.getZ());
    }
    
    private static final ThreadLocal<Vector3f> supportCache = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    private static final ThreadLocal<Vector4f> transformCache = new ThreadLocal<Vector4f>() {
        @Override
        protected Vector4f initialValue() { return new Vector4f(); }
    };
}