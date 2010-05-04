package com.ferox.math.bounds;

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
    /**
     * Interpret <tt>plane</tt> as a plane within the 3D coordinate space. The
     * plane is normalized by dividing all four coordinates by the magnitude of
     * the planes normal vector. The plane is normalized in place.
     * 
     * @param plane The plane to be normalized
     * @throws NullPointerException if plane is null
     */
	public static void normalize(Vector4f plane) {
		plane.scale(1f / length(plane));
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
	public static float getSignedDistance(Vector4f plane, Vector3f point) {
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
	public static float getSignedDistance(Vector4f plane, Vector3f point, boolean assumeNormalized) {
		float num = point.dot(plane.x, plane.y, plane.z) + plane.w;
		return (assumeNormalized ? num : num / length(plane));
	}
	
	private static float length(Vector4f v) {
		return (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
	}
}
