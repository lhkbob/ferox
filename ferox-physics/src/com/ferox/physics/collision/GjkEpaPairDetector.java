package com.ferox.physics.collision;


import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class GjkEpaPairDetector implements PairDetector {
    private final float margin;
    
    public GjkEpaPairDetector() {
        this(.05f);
    }
    
    public GjkEpaPairDetector(float margin) {
        this.margin = margin;
    }
    
    @Override
    public ClosestPair getClosestPair(Collidable objA, Collidable objB) {
        MinkowskiDifference shape = new MinkowskiDifference(objA, objB, margin);
        GJK gjk = new GJK(shape);
        
        ReadOnlyVector3f pa = objA.getWorldTransform().getCol(3).getAsVector3f();
        ReadOnlyVector3f pb = objB.getWorldTransform().getCol(3).getAsVector3f();
        
        Vector3f guess = pb.sub(pa, null);
        gjk.evaluate(guess);
        
        if (gjk.getStatus() == GJK.Status.VALID) {
            // non-intersecting pair
            return constructPair(shape, gjk.getSimplex(), null, pa, pb);
        } else if (gjk.getStatus() == GJK.Status.INSIDE) {
            // intersection, fall back onto EPA
            EPA epa = new EPA(gjk);
            epa.evaluate(guess);
            
            if (epa.getStatus() != EPA.Status.FAILED) {
                // epa successfully determined an intersection
                return constructPair(shape, epa.getSimplex(), epa.getNormal(), pa, pb);
            } else
                return null;
        } else
            return null;
    }
    
    private ClosestPair constructPair(MinkowskiDifference shape, Simplex simplex, ReadOnlyVector3f contactNormal, 
                                      ReadOnlyVector3f pA, ReadOnlyVector3f pB) {
        Vector3f wA = shape.getClosestPointA(simplex, false, null);
        Vector3f wB = shape.getClosestPointB(simplex, false, null);
        
        boolean intersecting = (pA.distanceSquared(wB) < pA.distanceSquared(wA)) && 
                               (pB.distanceSquared(wA) < pB.distanceSquared(wB));
        Vector3f normal = wB.sub(wA); // wB becomes the normal here
        float distance = normal.length() * (intersecting ? -1f : 1f);
        
        if (Math.abs(distance) < .00001f) {
            // special case for very close contact points - essentially touching contacts
            if (contactNormal != null) {
                // if provided, use computed normal from before
                contactNormal.normalize(normal).scale(intersecting ? -1f : 1f);
                distance = 0f;
            } else {
                // compute normal from within-margin closest pair
                normal = shape.getClosestPointB(simplex, true, normal)
                              .sub(shape.getClosestPointA(simplex, true, null));
                normal.normalize().scale(intersecting ? -1f : 1f);
                distance = 0f;
            }
        } else {
            // normalize, and possibly flip the normal based on intersection
            normal.scale(1f / distance);
        }
        
        return new ClosestPair(wA, normal, distance);
    }
}
