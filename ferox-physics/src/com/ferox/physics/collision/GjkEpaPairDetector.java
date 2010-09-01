package com.ferox.physics.collision;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

/**
 * <p>
 * The GjkEpaPairDetector is a PairDetector implementation that uses {@link GJK}
 * and {@link EPA} to compute accurate closest pair information between two
 * convex hulls. If the two objects are separated, it can rely solely on the GJK
 * algorithm otherwise it will automatically use the EPA algorithm and report an
 * intersection.
 * </p>
 * <p>
 * This pair detector implementation also uses a trick to improve efficiency.
 * The GJK algorithm is much faster than the EPA algorithm. When testing pairs
 * of objects, the detector 'shrinks' each object so that in many cases
 * intersection between the original pair of objects is a separation between the
 * smaller pair. This separation can be accurately computed with only GJK and
 * the GjkEpaPairDetector can quickly transform the pair back to the original
 * collision space.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class GjkEpaPairDetector implements PairDetector {
    private static final float CONTACT_NORMAL_ACCURACY = .001f;
    
    private final float margin;

    /**
     * Create a new GjkEpaPairDetector that's configured to use a margin of
     * <code>.05</code>.
     */
    public GjkEpaPairDetector() {
        this(.05f);
    }

    /**
     * Create a new GjkEpaPairDetector that's configured to use the specified
     * margin. If the margin is less than or equal to 0 disables the automated
     * shrinking of shapes that improves performance. This can be useful or
     * needed if collision shapes are smaller than the margin.
     * 
     * @param margin The margin to use when calling
     *            {@link #getClosestPair(Collidable, Collidable)}
     */
    public GjkEpaPairDetector(float margin) {
        this.margin = margin;
    }
    
    @Override
    public ClosestPair getClosestPair(Collidable objA, Collidable objB) {
        if (objA == null || objB == null)
            throw new NullPointerException("Collidable objects cannot be null");
        
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
        
        if (Math.abs(distance) < CONTACT_NORMAL_ACCURACY) {
            // special case for very close contact points, where the normal might become inaccurate
            if (contactNormal != null) {
                // if provided, use computed normal from before
                contactNormal.normalize(normal).scale(intersecting ? -1f : 1f);
            } else {
                // compute normal from within-margin closest pair
                normal = shape.getClosestPointB(simplex, true, normal)
                              .sub(shape.getClosestPointA(simplex, true, null));
                normal.normalize().scale(intersecting ? -1f : 1f);
            }
        } else {
            // normalize, and possibly flip the normal based on intersection
            normal.scale(1f / distance);
        }
        
        return new ClosestPair(wA, normal, distance);
    }
}
