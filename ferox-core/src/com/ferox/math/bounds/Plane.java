package com.ferox.math.bounds;

import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;

public class Plane {
	public static void normalize(Vector4f plane) {
		plane.scale(1f / length(plane));
	}
	
	public static float getSignedDistance(Vector4f plane, Vector3f point) {
		return getSignedDistance(plane, point, false);
	}
	
	public static float getSignedDistance(Vector4f plane, Vector3f point, boolean assumeNormalized) {
		float num = point.dot(plane.x, plane.y, plane.z) + plane.w;
		return (assumeNormalized ? num : num / length(plane));
	}
	
	private static float length(Vector4f v) {
		return (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
	}
}
