package com.ferox.math.bounds;

import org.openmali.vecmath.Vector3f;

/**
 * A utility class to compute enclosing AxisAlignedBox's for Boundables. This is
 * to be used for Boundables when they implement their getBounds() method. All
 * that is necessary is for them to correctly implement their getVertex() and
 * getVertexCount() methods.
 * 
 * @author Michael Ludwig
 */
public class AabbBoundableUtil {
	/**
	 * Utility method to compute the AABB of vertices and store it in box. Does
	 * nothing if box or vertices is null, or if vertices has a vertex count of
	 * 0.
	 * 
	 * @param vertices The Boundable whose aabb will be computed
	 * @param box The AxisAlignedBox who will hold the computed results
	 */
	public static void getBounds(Boundable vertices, AxisAlignedBox box) {
		if (vertices == null || box == null)
			return;
		int vertexCount = vertices.getVertexCount();
		if (vertexCount == 0)
			return;

		Vector3f worldMax = box.getMax();
		Vector3f worldMin = box.getMin();

		worldMax.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		worldMin.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

		for (int i = 0; i < vertexCount; i++)
			enclosePoint(worldMin, worldMax, vertices.getVertex(i, 0), vertices
				.getVertex(i, 1), vertices.getVertex(i, 2));
	}

	private static void enclosePoint(Vector3f worldMin, Vector3f worldMax,
		float x, float y, float z) {
		worldMax.x = Math.max(worldMax.x, x);
		worldMax.y = Math.max(worldMax.y, y);
		worldMax.z = Math.max(worldMax.z, z);

		worldMin.x = Math.min(worldMin.x, x);
		worldMin.y = Math.min(worldMin.y, y);
		worldMin.z = Math.min(worldMin.z, z);
	}
}
