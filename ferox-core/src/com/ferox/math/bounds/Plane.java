package com.ferox.math.bounds;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;


/**
 * The Plane class consists of a few static methods that can be used to
 * interpret a {@link Vector4f} as if it were a plane. Often a plane is
 * represented as four values: <A, B, C, D> where
 * <code>Ax + By + Cz + D = 0</code> defines the points on the plane. The four
 * components of a Vector4f: x, y, z and w correspond to A, B, C, and D,
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
    public static void normalize(Vector4f plane) {
        plane.scale(1f / lengthAsVector3f(plane));
    }

    /**
     * Compute the signed distance between the plane stored in <tt>plane</tt>
     * and the given <tt>point</tt>. The Vector4f storing the plane is stored as
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
    public static float getSignedDistance(ReadOnlyVector4f plane, ReadOnlyVector3f point) {
        return getSignedDistance(plane, point, false);
    }

    /**
     * Compute the signed distance between <tt>plane</tt> and <tt>point</tt>. If
     * <tt>assumeNormalized</tt> is false, this functions identically to
     * {@link #getSignedDistance(Vector4f, Vector3f)}. If it is true, this still
     * returns the signed distance but assumes that the given plane has already
     * been normalized via {@link #normalize(Vector4f)}. This avoids a square
     * root and division but can return erroneous results if the plane has not
     * actually been normalized.
     * 
     * @param plane The plane that is having its distance to a point computed
     * @param point The point that is having its distance to a plane computed
     * @param assumeNormalized True if plane has a normal that is unit length
     * @return The signed distance from the plane to the point
     * @throws NullPointerException if plane or point are null
     */
    public static float getSignedDistance(ReadOnlyVector4f plane, ReadOnlyVector3f point, boolean assumeNormalized) {
        float num = point.dot(plane.getX(), plane.getY(), plane.getZ()) + plane.getW();
        return (assumeNormalized ? num : num / lengthAsVector3f(plane));
    }
    
    // FIXME: verify behavior, math and document behavior
    public static void getTangentSpace(ReadOnlyVector3f normal, MutableVector3f tan0, MutableVector3f tan1) {
        // Gratz to Erwin Couman's and Bullet for this code
        
        if (Math.abs(normal.getZ()) > ROOT_2_OVER_2) {
            // choose p in y-z plane
            float a = normal.getY() * normal.getY() + normal.getZ() * normal.getZ();
            float k = 1f / (float) Math.sqrt(a);
            
            tan0.set(0f, -normal.getZ() * k, normal.getY() * k);
            tan1.set(a * k, -normal.getX() * tan0.getZ(), normal.getX() * tan0.getY()); // n x tan0
        } else {
            // choose p in x-y plane
            float a = normal.getX() * normal.getX() + normal.getY() * normal.getY();
            float k = 1f / (float) Math.sqrt(a);
            
            tan0.set(-normal.getY() * k, normal.getX() * k, 0f);
            tan1.set(-normal.getZ() * tan0.getY(), normal.getZ() * tan0.getX(), a * k); // n x tan0
        }
    }
    
    private static float lengthAsVector3f(ReadOnlyVector4f v) {
        return (float) Math.sqrt(v.getX() * v.getX() + v.getY() * v.getY() + v.getZ() * v.getZ());
    }
}
