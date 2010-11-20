package com.ferox.physics.collision.algorithm;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.shape.ConvexShape;

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
    private static final int MAX_EPA_CHECKS = 4;
    
    @Override
    public ClosestPair getClosestPair(ConvexShape shapeA, ReadOnlyMatrix4f transA,
                                      ConvexShape shapeB, ReadOnlyMatrix4f transB) {
        // MinkowskiDifference does the error checking for GjkEpaCollisionAlgorithm
        MinkowskiDifference support = new MinkowskiDifference(shapeA, transA, shapeB, transB);
        support.setNumAppliedMargins(0);
        GJK gjk = new GJK(support);
        
        ReadOnlyVector3f pa = transA.getCol(3).getAsVector3f();
        ReadOnlyVector3f pb = transB.getCol(3).getAsVector3f();
        
        ClosestPair p = null;
        MutableVector3f guess = pb.sub(pa, null);
        if (gjk.evaluate(guess) == GJK.Status.VALID) {
            // non-intersecting pair
           p = support.getClosestPair(gjk.getSimplex(), null);
           if (p != null)
               return p;
        } 
        
        EPA epa = new EPA(gjk);
        for (int i = 1; i < MAX_EPA_CHECKS; i++) {
            // intersection or failure, fall back onto EPA
            // must re-run the GJK without scaling so that the simplex is in the correct space
            support.setNumAppliedMargins(i);

            if (gjk.evaluate(guess) == GJK.Status.VALID) {
                p = support.getClosestPair(gjk.getSimplex(), null);
                if (p != null)
                    return p;
            }

            EPA.Status status = epa.evaluate(guess);
            if (status == EPA.Status.VALID) {
                // epa successfully determined an intersection
                p = support.getClosestPair(epa.getSimplex(), epa.getNormal());
                if (p != null)
                    return p;
            }
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
