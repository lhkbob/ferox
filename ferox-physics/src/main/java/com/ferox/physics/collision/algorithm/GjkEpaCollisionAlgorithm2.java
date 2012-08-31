package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.shape.ConvexShape;

public class GjkEpaCollisionAlgorithm2 implements CollisionAlgorithm<ConvexShape, ConvexShape> {
    private static final int MAX_EPA_CHECKS = 1;
    
    @Override
    public ClosestPair getClosestPair(ConvexShape shapeA, @Const Matrix4 transA, 
                                      ConvexShape shapeB, @Const Matrix4 transB) {
        ClosestPair p = null;
        
        MinkowskiShape shape = new MinkowskiShape(shapeA, transA, shapeB, transB);
        shape.setAppliedMargins(0);
        Vector3 guess = shape.getInitialGuess();
        
        Simplex2 simplex = GJK2.evaluate(shape, guess);
        if (simplex != null && !simplex.isIntersection()) {
            p = shape.getClosestPair(simplex, null);
            if (p != null)
                return p;
        }
        
        for (int i = 1; i <= MAX_EPA_CHECKS; i++) {
            shape.setAppliedMargins(i);
            
            simplex = GJK2.evaluate(shape, guess);
            if (simplex != null) {
                if (!simplex.isIntersection()) {
                    // unlikely but is a possible early escape
                    p = shape.getClosestPair(simplex, null);
                    if (p != null)
                        return p;
                } else {
                    // run epa
//                    Simplex os = new Simplex(simplex);
//                    MinkowskiDifference od = new MinkowskiDifference(shapeA, transA, shapeB, transB);
//                    EPA epa = new EPA(od, os);
//                    if (epa.evaluate(guess) == EPA.Status.VALID) {
//                        p = od.getClosestPair(epa.getSimplex(), epa.getNormal());
//                        if (p != null)
//                            return p;
//                    }
                    p = EPA2.evaluate(simplex);
                    if (p != null)
                        return p;
                }
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
