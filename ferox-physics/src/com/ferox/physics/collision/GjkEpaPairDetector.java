package com.ferox.physics.collision;


import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.Vector3f;

public class GjkEpaPairDetector implements PairDetector {
    @Override
    public ClosestPair getClosestPair(Collidable objA, Collidable objB) {
        Vector3f temp = new Vector3f();

        MinkowskiDifference shape = new MinkowskiDifference(objA, objB);
        GJK gjk = new GJK(shape);
        
        ReadOnlyMatrix4f ta = objA.getWorldTransform();
        ReadOnlyMatrix4f tb = objB.getWorldTransform();
        
        Vector3f guess = temp.set(tb.get(0, 3) - ta.get(0, 3), tb.get(1, 3) - ta.get(1, 3), tb.get(2, 3) - ta.get(2, 3));
        gjk.evaluate(guess);
        
        if (gjk.getStatus() == GJK.Status.VALID) {
            // non-intersecting pair
            Vector3f wA = new Vector3f();
            Vector3f wB = new Vector3f();
            Simplex simplex = gjk.getSimplex();
            for (int i = 0; i < simplex.getRank(); i++) {
                shape.getSupportA(simplex.getSupportInput(i), temp).scaleAdd(simplex.getWeight(i), wA, wA);
                shape.getSupportB(simplex.getSupportInput(i).scale(-1f, temp), temp).scaleAdd(simplex.getWeight(i), wB, wB);
            }
            
            Vector3f normal = wB.sub(wA); // wB becomes the normal here
            float distance = normal.length();
            normal.scale(1f / distance);
            
            return new ClosestPair(wA, normal, distance);
        } else if (gjk.getStatus() == GJK.Status.INSIDE) {
            // intersection, fall back onto EPA
            EPA epa = new EPA(gjk);
            epa.evaluate(guess);
            
            if (epa.getStatus() != EPA.Status.FAILED) {
                Simplex simplex = epa.getSimplex();
                Vector3f w0 = new Vector3f(0f, 0f, 0f);
                for (int i = 0; i < simplex.getRank(); i++)
                    shape.getSupportA(simplex.getSupportInput(i), temp).scaleAdd(simplex.getWeight(i), w0, w0);
                
                return new ClosestPair(w0, epa.getNormal(), -epa.getDepth());
            } else
                return null;
        } else
            return null;
    }
}
