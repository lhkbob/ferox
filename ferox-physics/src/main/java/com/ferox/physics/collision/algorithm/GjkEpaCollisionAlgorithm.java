package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
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
    public ClosestPair getClosestPair(ConvexShape shapeA, @Const Matrix4 transA,
                                      ConvexShape shapeB, @Const Matrix4 transB) {
        // MinkowskiDifference does the error checking for GjkEpaCollisionAlgorithm
        MinkowskiDifference support = new MinkowskiDifference(shapeA, transA, shapeB, transB);
        support.setNumAppliedMargins(0);
        GJK gjk = new GJK(support);
        
        // FIXME this code is duplicated in a number of places, particularly MinkowskiDifference,
        // but in MD it needs to mutate the vectors, another place is in SphereSphereCollisionAlgorithm
        Vector3 pa = new Vector3(transA.m03, transA.m13, transA.m23);
        Vector3 pb = new Vector3(transB.m03, transB.m13, transB.m23);
        
        ClosestPair p = null;
        Vector3 guess = new Vector3().sub(pb, pa);
        if (gjk.evaluate(guess) == GJK.Status.VALID) {
            // non-intersecting pair
           p = support.getClosestPair(gjk.getSimplex(), null);
           if (p != null)
               return p;
        } 
        
        EPA epa = new EPA(gjk);
        for (int i = 1; i < MAX_EPA_CHECKS; i++) {
            // intersection or failure, fall back onto EPA
            // must re-run the GJK with scaling so that the simplex is in the correct space
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
