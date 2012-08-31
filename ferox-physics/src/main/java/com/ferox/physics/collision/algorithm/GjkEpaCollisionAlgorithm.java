package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.shape.Box;
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
    
    public static int gjkChecks = 0;
    public static int epaChecks = 0;
    
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
        gjkChecks++;
        Simplex2 s = GJK2.evaluate(support.asShape(), guess);
//        if (gjk.evaluate(guess) == GJK.Status.VALID) {
        if (s != null && !s.isIntersection()) {
            // non-intersecting pair
//            System.out.println("no margin, valid GJK simplex: " + gjk.getSimplex());
//            p = support.asShape().getClosestPair(new Simplex2(support.asShape(), gjk.getSimplex()), null);
            p = s.getShape().getClosestPair(s, null);
//           p = support.getClosestPair(gjk.getSimplex(), null);
           if (p != null)
               return p;
        } 
        
//        System.out.println("no margin, invalid GJK simplex: " + gjk.getSimplex());

        epaChecks++;
//        EPA epa = new EPA(gjk);
        for (int i = 1; i <= MAX_EPA_CHECKS; i++) {
            // intersection or failure, fall back onto EPA
            // must re-run the GJK with scaling so that the simplex is in the correct space
            support.setNumAppliedMargins(i);

            gjkChecks++;
            s = GJK2.evaluate(support.asShape(), guess);
            if (s != null && !s.isIntersection()) {
//            if (gjk.evaluate(guess) == GJK.Status.VALID) {
                p = s.getShape().getClosestPair(s, null);
//                p = support.getClosestPair(gjk.getSimplex(), null);
                if (p != null)
                    return p;
            }

//            System.out.println("margin " + i + ", invalid GJK simplex: " + gjk.getSimplex());
            
            if (s != null && s.isIntersection()) {
                epaChecks++;
//                p = EPA2.evaluate(s);
//                if (p != null)
//                    return p;
                
                EPA epa = new EPA(support, new Simplex(s));
//                EPA epa = new EPA(gjk);
                EPA.Status status = epa.evaluate(guess);
                if (status == EPA.Status.VALID) {
                    // epa successfully determined an intersection
                    //                System.out.println("margin " + i + ", valid EPA simplex: " + epa.getSimplex());
                    p = support.getClosestPair(epa.getSimplex(), epa.getNormal());
                    if (p != null)
                        return p;
                }
            }
            
//            System.out.println("margin " + i + ", invalid EPA simplex: " + epa.getSimplex());
        }
        
//        System.err.println("EPA iterations failed");
        
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
