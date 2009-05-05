package com.ferox.util.geom;

import org.openmali.vecmath.Vector2f;
import org.openmali.vecmath.Vector3f;

/**
 * <p>
 * TangentGenerator is a utility to generate arrays of tangent and bitangent
 * vectors from a set vertices, texture coordinates and indices. For the moment,
 * it only supports triangles and floating-point vertices/texcoords.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class TangentGenerator {
	/**
	 * <p>
	 * Fill the tangents and bitangents arrays with appropriate values based on
	 * the given vertices, texture coordinates, and an indices array to access
	 * them. This index array is also used to access the tangents and bitangents
	 * arrays.
	 * </p>
	 * <p>
	 * All arguments must be non-null. tangents and bitangents must have the
	 * same length as vertices.
	 * </p>
	 * <p>
	 * It is assumed that vertices are packed together, every 3 primitives being
	 * a vertex. Similarly, every 2 primitives in texCoords is a texture
	 * coordinate. Every 3 ints in indices represent the 3 coordinates of a
	 * triangle.
	 * </p>
	 * 
	 * @param vertices The vertex coordinates for the triangles
	 * @param texCoords The texture coordinates for the triangles
	 * @param indices The indices that describe the triangles
	 * @param tangents The array storage for computed tangent vectors
	 * @param bitangents The array storage for the computed bitangent vectors
	 * @throws NullPointerException if any parameter is null
	 * @throws IllegalArgumentException if the element counts in vertices,
	 *             texCoords, tangent, and bitangents don't match
	 */
	public static void generate(float[] vertices, float[] texCoords,
			int[] indices, float[] tangents, float[] bitangents) {
		if (vertices == null || texCoords == null || indices == null
				|| tangents == null || bitangents == null)
			throw new NullPointerException("All arguments must be non-null");
		if (texCoords.length / 2 != vertices.length / 3)
			throw new IllegalArgumentException(
					"Tex coords don't have same number of elements as vertices");
		if (tangents.length != vertices.length)
			throw new IllegalArgumentException(
					"Tangents array doesn't have the same length as vertices");
		if (bitangents.length != vertices.length)
			throw new IllegalArgumentException(
					"Bitangents array doesn't have the same length as vertices");

		Vector3f tangent = new Vector3f();
		Vector3f bitangent = new Vector3f();

		// for a triangle
		Vector3f verts[] = new Vector3f[3];
		Vector2f tcs[] = new Vector2f[3];

		int v, i;
		for (i = 0; i < 3; i++) {
			verts[i] = new Vector3f();
			tcs[i] = new Vector2f();
		}

		int[] index = new int[3];

		int loop = indices.length / 3;
		for (int t = 0; t < loop; t++) {
			for (v = 0; v < 3; v++) {
				i = indices[t * 3 + v];
				index[v] = i;

				verts[v].x = vertices[i * 3];
				verts[v].y = vertices[i * 3 + 1];
				verts[v].z = vertices[i * 3 + 2];

				tcs[v].x = texCoords[i * 2];
				tcs[v].y = texCoords[i * 2 + 1];
			}

			computeTriangleTangentSpace(tangent, bitangent, verts, tcs);

			for (v = 0; v < 3; v++) {
				i = index[v] * 3;
				tangents[i] = tangent.x;
				tangents[i + 1] = tangent.y;
				tangents[i + 2] = tangent.z;

				bitangents[i] = bitangent.x;
				bitangents[i + 1] = bitangent.y;
				bitangents[i + 2] = bitangent.z;
			}
		}
	}

	// modifies v and t vectors, but that's okay since they aren't used after
	// this method is called
	private static void computeTriangleTangentSpace(Vector3f tangent,
			Vector3f bitangent, Vector3f v[], Vector2f t[]) {
		Vector3f edge1 = v[1];
		edge1.sub(v[0]);
		Vector3f edge2 = v[2];
		edge2.sub(v[0]);

		Vector2f edge1uv = t[1];
		edge1uv.sub(t[0]);
		Vector2f edge2uv = t[2];
		edge2uv.sub(t[0]);

		float cp = edge1uv.y * edge2uv.x - edge1uv.x * edge2uv.y;

		if (cp != 0.0f) {
			float mul = 1.0f / cp;
			tangent.scale(-edge2uv.y, edge1);
			tangent.scaleAdd(edge1uv.y, edge2, tangent);
			tangent.scale(mul);
			tangent.normalize();

			bitangent.scale(-edge2uv.x, edge1);
			bitangent.scaleAdd(edge1uv.x, edge2, bitangent);
			bitangent.scale(mul);
			bitangent.normalize();
		}
	}
}
