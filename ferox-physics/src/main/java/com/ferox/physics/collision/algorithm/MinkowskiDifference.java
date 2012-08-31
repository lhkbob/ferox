package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.collision.shape.ConvexShape;

/**
 * MinkowskiDifference represents the mathematical concept of a Minkowski sum
 * between two convex shapes. Because {@link GJK} and {@link EPA} are interested
 * in the difference between two shapes, the second shape in the sum is just the
 * negation of one convex shape. This implementation also automatically handles
 * converting the local support functions of {@link ConvexShape ConvexShapes}
 * into world space by applying the world transform of {@link CollisionBody
 * Collidables} involved.
 * 
 * @author Michael Ludwig
 */
public class MinkowskiDifference {
    private static final double CONTACT_NORMAL_ACCURACY = .0001;
    
    private ConvexShape shapeA;
    private ConvexShape shapeB;
    
    private Matrix4 transA; // transforms a to world
    private Matrix4 transB; // transforms b to world
    
    private int appliedMargins;
    
    // final variables to reuse during computations, should be faster than thread-local
//    private final Vector3 supportCache;
//    private final Vector3 dirCache;
//    private final Vector4 transformCache;

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
    public MinkowskiDifference(ConvexShape shapeA, @Const Matrix4 transA,
                               ConvexShape shapeB, @Const Matrix4 transB) {
        if (shapeA == null || shapeB == null || transA == null || transB == null)
            throw new NullPointerException("Arguments cannot be null");
//        supportCache = new Vector3();
//        dirCache = new Vector3();
//        transformCache = new Vector4();
        
        this.shapeA = shapeA;
        this.shapeB = shapeB;
        this.transA = transA;
        this.transB = transB;
        
        appliedMargins = 1;
    }

    public MinkowskiShape asShape() {
        MinkowskiShape s = new MinkowskiShape(shapeA, transA, shapeB, transB);
        s.setAppliedMargins(appliedMargins);
        return s;
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
    public ClosestPair getClosestPair(Simplex simplex, @Const Vector3 zeroNormal) {
        Vector3 pa = new Vector3(transA.m03, transA.m13, transA.m23);
        Vector3 pb = new Vector3(transB.m03, transB.m13, transB.m23);
        
        Vector3 ma = getClosestPointA(simplex);
        Vector3 mb = getClosestPointB(simplex);
        
        return constructPair(ma, mb, pa, pb, zeroNormal);
    }
    
    private ClosestPair constructPair(Vector3 wA, Vector3 wB, 
                                      Vector3 pA, Vector3 pB,
                                      @Const Vector3 zeroNormal) {
        boolean intersecting = (pA.distanceSquared(wB) < pA.distanceSquared(wA)) ||
                               (pB.distanceSquared(wA) < pB.distanceSquared(wB));
        // after this, pA and pB are free vectors to be mutated
        
//        Vector3 normal = wB.sub(wA); // wB becomes the normal here
        Vector3 normal = new Vector3().sub(wB, wA);
        double distance = normal.length() * (intersecting ? -1.0 : 1.0);

        if (Math.abs(distance) < CONTACT_NORMAL_ACCURACY) {
            // special case for very close contact points, where the normal might become inaccurate
            if (zeroNormal != null) {
                normal.normalize(zeroNormal);
                if (intersecting)
                    normal.scale(-1f);
            } else
                return null;
        } else {
            // normalize, and possibly flip the normal based on intersection
            normal.scale(1.0 / distance);
        }
        
        if (appliedMargins != 1) {
            int scale = Math.abs(appliedMargins - 1);
            double distDelta = scale * (shapeA.getMargin() + shapeB.getMargin());
            if (intersecting)
                distDelta *= -1.0;
            
//            wA.add(pA.scale(normal, (1 - appliedMargins) * shapeA.getMargin()));
            wA.add(new Vector3().scale(normal, (1 - appliedMargins) * shapeA.getMargin()));

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


    private Vector3 getClosestPointA(Simplex simplex) {
        Vector3 result = new Vector3();
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            Vector3 s = new Vector3();
            getAffineSupport(shapeA, transA, simplex.getVertex(i).getInputVector(), s);
            s.scale(simplex.getVertex(i).getWeight());
            result.add(s);
//            getAffineSupport(shapeA, transA, simplex.getVertex(i).getInputVector(), supportCache);
//            result.add(supportCache.scale(simplex.getVertex(i).getWeight()));
        }
        return result;
    }

    private Vector3 getClosestPointB(Simplex simplex) {
        Vector3 result = new Vector3();
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            Vector3 s = new Vector3();
            getAffineSupport(shapeB, transB, new Vector3().scale(simplex.getVertex(i).getInputVector(), -1.0), s);
            s.scale(simplex.getVertex(i).getWeight());
            result.add(s);
//            getAffineSupport(shapeB, transB, supportCache.scale(simplex.getVertex(i).getInputVector(), -1.0), supportCache);
//            result.add(supportCache.scale(simplex.getVertex(i).getWeight()));
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
    public Vector3 getSupport(@Const Vector3 d, Vector3 result) {
        if (result == null)
            result = new Vector3();
        
        Vector3 a = new Vector3();
        Vector3 b = new Vector3();
        getAffineSupport(shapeA, transA, d, a);
        getAffineSupport(shapeB, transB, new Vector3().scale(d, -1.0), b);
//        System.out.println("get support " + d + " " + a + " " + b);
        return result.sub(a, b);
    }
    
    private void getAffineSupport(ConvexShape shape, @Const Matrix4 t, @Const Vector3 d, Vector3 result) {
        // first step is to transform d by the transpose of the upper 3x3
        // we do this by wrapping d in a 4-vector and setting w = 0
//        transformCache.set(d.x, d.y, d.z, 0.0).mul(transformCache, t);
        Vector4 transformed = new Vector4(d.x, d.y, d.z, 0.0);
        transformed.mul(transformed, t);
        
        // second step is to compute the local support
        Vector3 dir = new Vector3(transformed.x, transformed.y, transformed.z);
//        dirCache.set(transformCache.x, transformCache.y, transformCache.z);
//        shape.computeSupport(dirCache, result);
        shape.computeSupport(dir, result);
        
        if (appliedMargins > 0) {
            // apply a number of margin offsets, as if a sphere is added to the convex shape
//            result.add(dirCache.scale(appliedMargins * shape.getMargin()));
            result.add(new Vector3().scale(dir, appliedMargins * shape.getMargin()));
        }
        
        // then transform that by the complete affine transform, so w = 1
//        transformCache.set(result.x, result.y, result.z, 1.0).mul(t, transformCache);
        transformed.set(result.x, result.y, result.z, 1.0);
        transformed.mul(t, transformed);
//        result.set(transformCache.x, transformCache.y, transformCache.z);
        result.set(transformed.x, transformed.y, transformed.z);
    }
}