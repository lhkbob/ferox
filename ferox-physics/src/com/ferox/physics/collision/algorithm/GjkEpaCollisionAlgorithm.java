package com.ferox.physics.collision.algorithm;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.physics.collision.shape.ConvexShape;
import com.ferox.physics.collision.shape.Sphere;

/**
 * <p>
 * The GjkEpaCollisionAlgorithm is a CollisionAlgorithm implementation that uses
 * {@link GJK} and {@link EPA} to compute accurate closest pair information
 * between two convex hulls. If the two objects are separated, it can rely
 * solely on the GJK algorithm otherwise it will automatically use the EPA
 * algorithm and report an intersection.
 * </p>
 * <p>
 * This pair detector implementation also uses a trick to improve efficiency.
 * The GJK algorithm is much faster than the EPA algorithm. When testing pairs
 * of objects, the detector 'shrinks' each object so that in many cases
 * intersection between the original pair of objects is a separation between the
 * smaller pair. This separation can be accurately computed with only GJK and
 * the GjkEpaCollisionAlgorithm can quickly transform the pair back to the
 * original collision space.
 * </p>
 * <p>
 * This CollisionAlgorithm only supports collisions between {@link ConvexShape
 * ConvexShapes}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class GjkEpaCollisionAlgorithm implements CollisionAlgorithm<ConvexShape, ConvexShape> {
    public static int NUM_GJK_CHECKS = 0;
    public static int NUM_EPA_CHECKS = 0;
    
    private final float scale;

    /**
     * Create a new GjkEpaCollisionAlgorithm that's configured to use a scale of
     * <code>.9</code>.
     */
    public GjkEpaCollisionAlgorithm() {
        this(0.95f);
    }

    /**
     * Create a new GjkEpaCollisionAlgorithm that's configured to use the
     * specified margin. If the scale is less than or equal to 0 disables the
     * automated shrinking of shapes that improves performance.
     * 
     * @param scale The scale to use when calling
     *            {@link #getClosestPair(ConvexShape, ReadOnlyMatrix4f, ConvexShape, ReadOnlyMatrix4f)

     */
    public GjkEpaCollisionAlgorithm(float scale) {
        if (scale > 1f)
            throw new IllegalArgumentException("Scale factor must be less than 1, not: " + scale);
        this.scale = scale;
    }
    
    @Override
    public ClosestPair getClosestPair(ConvexShape shapeA, ReadOnlyMatrix4f transA,
                                      ConvexShape shapeB, ReadOnlyMatrix4f transB) {
        // MinkowskiDifference does the error checking for GjkEpaCollisionAlgorithm
        MinkowskiDifference support = new MinkowskiDifference(shapeA, transA, shapeB, transB, .05f);
        support.setIgnoreMargin(true);
        GJK gjk = new GJK(support);
        
        ReadOnlyVector3f pa = transA.getCol(3).getAsVector3f();
        ReadOnlyVector3f pb = transB.getCol(3).getAsVector3f();
        
        MutableVector3f guess = pb.sub(pa, null);
        gjk.evaluate(guess);
        NUM_GJK_CHECKS++;
        if (gjk.getStatus() == GJK.Status.VALID) {
            // non-intersecting pair
           ClosestPair p = support.getClosestPair(gjk.getSimplex(), null);
           if (p != null)
               return p;
        } 
        
        // intersection or failure, fall back onto EPA
        // must re-run the GJK without scaling so that the simplex is in the correct space
        support.setIgnoreMargin(false);
        // FIXME: use a better guess based on the last run
        gjk.evaluate(guess);
        if (gjk.getStatus() == GJK.Status.VALID)
            return support.getClosestPair(gjk.getSimplex(), null);
//        else if (gjk.getStatus() == GJK.Status.FAILED)
//            return support.getClosestPair(gjk.getSimplex(), null); // double failure maybe?

        NUM_EPA_CHECKS++;
        EPA epa = new EPA(gjk);
        epa.evaluate(guess);

        if (epa.getStatus() == EPA.Status.VALID || epa.getStatus() == EPA.Status.ACCURACY_REACHED
            || epa.getStatus() == EPA.Status.FALLBACK) {
            // epa successfully determined an intersection
            return support.getClosestPair(epa.getSimplex(), epa.getNormal());
        }
        return null;
    }

    @Override
    public Class<ConvexShape> getShapeTypeA() {
        return ConvexShape.class;
    }

    @Override
    public Class<ConvexShape> getShapeTypeB() {
        return ConvexShape.class;
    }
}
