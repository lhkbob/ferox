package com.ferox.physics.collision;

import com.ferox.math.ReadOnlyVector3f;

/**
 * ClosestPair is a data-storage class that contains the closest pair of points
 * between two Collidables, A and B. It can differentiate between separated
 * objects and intersecting objects. It is used by a {@link PairDetector} to
 * compute accurate collision information between pairs of objects.
 * 
 * @author Michael Ludwig
 */
public class ClosestPair {
    private final ReadOnlyVector3f contactNormalFromA;
    private final ReadOnlyVector3f closestPointOnA;
    private final ReadOnlyVector3f closestPointOnB;

    private final float distance;

    /**
     * Create a new ClosestPair. <tt>pointOnA</tt> represents the point on the
     * first object's surface. The point on the second object's surface is
     * reconstructed from <tt>pointOnA</tt>, the <tt>contactNormal</tt>, and the
     * distance along the normal. It is assumed that the contact normal has
     * already been normalized.
     * 
     * @param pointOnA The closest point on the A's surface
     * @param contactNormal The normal from pointOnA to the point on B's
     *            surface, normalized
     * @param distance The distance along contactNormal to get to the point on
     *            B's surface, negative for an intersection situation
     * @throws NullPointerException if pointOnA or contactNormal are null
     */
    public ClosestPair(ReadOnlyVector3f pointOnA, ReadOnlyVector3f contactNormal, float distance) {
        // FIXME: validate
        this.distance = distance;

        contactNormalFromA = contactNormal;
        closestPointOnA = pointOnA;
        closestPointOnB = contactNormal.scaleAdd(distance, pointOnA, null);
    }

    /**
     * Return the normalized contact normal. The normal points in the direction
     * of object A to object B. In the case of intersections, the normal's
     * direction remains consistent with the direction it would be pointing were
     * the two objects to slide apart. This avoids a sudden negation of the
     * contact normal between two objects that approach each other and then
     * intersect; instead, the distance becomes negated.
     * 
     * @return The contact normal
     */
    public ReadOnlyVector3f getContactNormal() {
        return contactNormalFromA;
    }

    /**
     * Return the world-space closest point to object B that is on the surface
     * of object A.
     * 
     * @return The closest point in this pair on the surface of A
     */
    public ReadOnlyVector3f getClosestPointOnA() {
        return closestPointOnA;
    }

    /**
     * Return the world-space closest point to object A that is on the surface
     * of object B.
     * 
     * @return The closest point in this pair on the surface of B
     */
    public ReadOnlyVector3f getClosestPointOnB() {
        return closestPointOnB;
    }

    /**
     * Return the distance between the two points of this closest pair. If the
     * returned value is positive, the two objects are separated. If the
     * distance is negative, the two objects are intersecting. A value of 0
     * implies that the objects are exactly touching each other.
     * 
     * @return The contact distance
     */
    public float getDistance() {
        return distance;
    }

    /**
     * Convenience function to return whether or not this pair represents the
     * closest point pair between two intersecting convex hulls. This returns
     * true if and only if {@link #getDistance()} is less than or equal to 0.
     * 
     * @return True if the two involved objects are intersecting
     */
    public boolean isIntersecting() {
        return distance <= 0f;
    }
}
