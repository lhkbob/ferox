package com.ferox.math.bounds;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;



/**
 * The Plane class consists of a few static methods that can be used to
 * interpret a {@link Vector4} as if it were a plane. Often a plane is
 * represented as four values: <A, B, C, D> where
 * <code>Ax + By + Cz + D = 0</code> defines the points on the plane. The four
 * components of a Vector4: x, y, z and w correspond to A, B, C, and D,
 * respectively. The normal vector of a plane is stored within <A, B, C>.
 * 
 * @author Michael Ludwig
 */
public class Plane {
    private static final float ROOT_2_OVER_2 = .7071067811865f;
    
    /**
     * Interpret <tt>plane</tt> as a plane within the 3D coordinate space. The
     * plane is normalized by dividing all four coordinates by the magnitude of
     * the planes normal vector. The plane is normalized in place.
     * 
     * @param plane The plane to be normalized
     * @throws NullPointerException if plane is null
     */
    public static void normalize(Vector4 plane) {
        plane.scale(1.0 / lengthAsVector3(plane));
    }

    /**
     * Compute the signed distance between the plane stored in <tt>plane</tt>
     * and the given <tt>point</tt>. The Vector4 storing the plane is stored as
     * described above. If the returned distance is less than 0, the point is
     * "behind" the plane, if it is 0 it lies on the plane, and if it is
     * positive, the point lies in front of the plane. In front of and behind
     * depend on the direction which the normal of the plane is facing.
     * 
     * @param plane The plane that is having its distance to a point computed
     * @param point The point that is having its distance to a plane computed
     * @return The signed distance from the plane to the point
     * @throws NullPointerException if plane or point are null
     */
    public static double getSignedDistance(@Const Vector4 plane, @Const Vector3 point) {
        return getSignedDistance(plane, point, false);
    }

    /**
     * Compute the signed distance between <tt>plane</tt> and <tt>point</tt>. If
     * <tt>assumeNormalized</tt> is false, this functions identically to
     * {@link #getSignedDistance(Vector4, Vector3)}. If it is true, this still
     * returns the signed distance but assumes that the given plane has already
     * been normalized via {@link #normalize(Vector4)}. This avoids a square
     * root and division but can return erroneous results if the plane has not
     * actually been normalized.
     * 
     * @param plane The plane that is having its distance to a point computed
     * @param point The point that is having its distance to a plane computed
     * @param assumeNormalized True if plane has a normal that is unit length
     * @return The signed distance from the plane to the point
     * @throws NullPointerException if plane or point are null
     */
    public static double getSignedDistance(@Const Vector4 plane, @Const Vector3 point, boolean assumeNormalized) {
        double num = point.x * plane.x + point.y * plane.y + point.z * plane.z + plane.w;
        return (assumeNormalized ? num : num / lengthAsVector3(plane));
    }
    
    // FIXME: verify behavior, math and document behavior
    // FIXME: this doesn't have much to do with Planes, so we should move it somewhere
    // else
    public static void getTangentSpace(@Const Vector3 normal, Vector3 tan0, Vector3 tan1) {
        // Gratz to Erwin Couman's and Bullet for this code
        
        if (Math.abs(normal.z) > ROOT_2_OVER_2) {
            // choose p in y-z plane
            double a = normal.y * normal.y + normal.z * normal.z;
            double k = 1 / Math.sqrt(a);
            
            tan0.set(0, -normal.z * k, normal.y * k);
            tan1.set(a * k, -normal.x * tan0.z, normal.x * tan0.y); // n x tan0
        } else {
            // choose p in x-y plane
            double a = normal.x * normal.x + normal.y * normal.y;
            double k = 1 / Math.sqrt(a);
            
            tan0.set(-normal.y * k, normal.x * k, 0);
            tan1.set(-normal.z * tan0.y, normal.z * tan0.x, a * k); // n x tan0
        }
    }
    
    private static double lengthAsVector3(Vector4 v) {
        return Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
    }
}
