package com.ferox.util.geom;

import org.openmali.vecmath.Vector3f;

import com.ferox.resource.IndexedArrayGeometry;

/**
 * <p>
 * A Box represents a 6 sided rectangular prism.
 * </p>
 * <p>
 * This code was ported from com.jme.scene.shapes.Box
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Box extends IndexedArrayGeometry {
	private final Vector3f center = new Vector3f();
	private float xExtent, yExtent, zExtent;

	/**
	 * Construct a box centered on its origin, with the given side length. So,
	 * Box(1f) creates a unit cube. Uses CompileType.NONE.
	 * 
	 * @param side The side length of the created cube
	 */
	public Box(float side) {
		this(side, CompileType.VERTEX_ARRAY);
	}

	/**
	 * Construct a new Box with the given minimum and maximum points. These
	 * points are opposite corners of the box. Uses CompileType.NONE.
	 * 
	 * @param min Minimum corner of the box
	 * @param max Maximum corner of the box
	 * @throws NullPointerException if min or max are null
	 */
	public Box(Vector3f min, Vector3f max) {
		this(min, max, CompileType.VERTEX_ARRAY);
	}

	/**
	 * Construct a box centered on its origin, with the given side length. So,
	 * Box(1f) creates a unit cube.
	 * 
	 * @param side The side length of the created cube
	 * @param type The compile type to use for the Box
	 */
	public Box(float side, CompileType type) {
		this(new Vector3f(-side / 2f, -side / 2f, -side / 2f), new Vector3f(
				side / 2f, side / 2f, side / 2f), type);
	}

	/**
	 * Construct a new Box with the given minimum and maximum points. These
	 * points are opposite corners of the box.
	 * 
	 * @param min Minimum corner of the box
	 * @param max Maximum corner of the box
	 * @param type The compile type to use for the Box
	 * @throws NullPointerException if min or max are null
	 */
	public Box(Vector3f min, Vector3f max, CompileType type) {
		super(type);
		// set the data, this will set vertices, normals, etc.
		// as needed in super-class
		setData(min, max);
	}

	/**
	 * Returns the current center of this box, in local space. The result is
	 * stored within store. If store is null, a new vector is created.
	 * 
	 * @param store The vector to hold the center
	 * @return store, or a new Vector3f holding the center
	 */
	public Vector3f getCenter(Vector3f store) {
		if (store == null)
			store = new Vector3f();
		store.set(center);
		return store;
	}

	/**
	 * <p>
	 * Changes the data of the box so that the two opposite corners are minPoint
	 * and maxPoint. The other corners are created from those two points.
	 * </p>
	 * <p>
	 * This assumes that minPoint represents the minimum coordinate point of the
	 * box, and maxPoint is the max. If this isn't true, results are undefined.
	 * </p>
	 * 
	 * @param minPoint Minimum corner of the box
	 * @param maxPoint Maximum corner of the box
	 * @throws NullPointerException if minPoint or maxPoint are null
	 */
	public void setData(Vector3f minPoint, Vector3f maxPoint) {
		if (minPoint == null || maxPoint == null)
			throw new NullPointerException(
					"minPoint and maxPoint cannot be null");

		center.add(minPoint, maxPoint);
		center.scale(.5f);

		this.xExtent = maxPoint.x - center.x;
		this.yExtent = maxPoint.y - center.y;
		this.zExtent = maxPoint.z - center.z;

		updateData();
	}

	/* Set all of the IndexedArrayGeometry's fields to be valid. */
	private void updateData() {
		float[] v = new float[72];
		float[] n = new float[72];
		float[] t = new float[48];

		float minX = center.x - xExtent;
		float maxX = center.x + xExtent;
		float minY = center.y - yExtent;
		float maxY = center.y + yExtent;
		float minZ = center.z - zExtent;
		float maxZ = center.z + zExtent;

		int ti = 0;
		int ni = 0;
		int vi = 0;

		// back
		t[ti++] = 1f;
		t[ti++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		n[ni++] = -1f;
		v[vi++] = minX;
		v[vi++] = minY;
		v[vi++] = minZ;
		t[ti++] = 0f;
		t[ti++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		n[ni++] = -1f;
		v[vi++] = maxX;
		v[vi++] = minY;
		v[vi++] = minZ;
		t[ti++] = 0f;
		t[ti++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		n[ni++] = -1f;
		v[vi++] = maxX;
		v[vi++] = maxY;
		v[vi++] = minZ;
		t[ti++] = 1f;
		t[ti++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		n[ni++] = -1f;
		v[vi++] = minX;
		v[vi++] = maxY;
		v[vi++] = minZ;

		// right
		t[ti++] = 1f;
		t[ti++] = 0f;
		n[ni++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		v[vi++] = maxX;
		v[vi++] = minY;
		v[vi++] = minZ;
		t[ti++] = 0f;
		t[ti++] = 0f;
		n[ni++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		v[vi++] = maxX;
		v[vi++] = minY;
		v[vi++] = maxZ;
		t[ti++] = 0f;
		t[ti++] = 1f;
		n[ni++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		v[vi++] = maxX;
		v[vi++] = maxY;
		v[vi++] = maxZ;
		t[ti++] = 1f;
		t[ti++] = 1f;
		n[ni++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		v[vi++] = maxX;
		v[vi++] = maxY;
		v[vi++] = minZ;

		// front
		t[ti++] = 1f;
		t[ti++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 1f;
		v[vi++] = maxX;
		v[vi++] = minY;
		v[vi++] = maxZ;
		t[ti++] = 0f;
		t[ti++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 1f;
		v[vi++] = minX;
		v[vi++] = minY;
		v[vi++] = maxZ;
		t[ti++] = 0f;
		t[ti++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 1f;
		v[vi++] = minX;
		v[vi++] = maxY;
		v[vi++] = maxZ;
		t[ti++] = 1f;
		t[ti++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 1f;
		v[vi++] = maxX;
		v[vi++] = maxY;
		v[vi++] = maxZ;

		// left
		t[ti++] = 1f;
		t[ti++] = 0f;
		n[ni++] = -1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		v[vi++] = minX;
		v[vi++] = minY;
		v[vi++] = maxZ;
		t[ti++] = 0f;
		t[ti++] = 0f;
		n[ni++] = -1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		v[vi++] = minX;
		v[vi++] = minY;
		v[vi++] = minZ;
		t[ti++] = 0f;
		t[ti++] = 1f;
		n[ni++] = -1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		v[vi++] = minX;
		v[vi++] = maxY;
		v[vi++] = minZ;
		t[ti++] = 1f;
		t[ti++] = 1f;
		n[ni++] = -1f;
		n[ni++] = 0f;
		n[ni++] = 0f;
		v[vi++] = minX;
		v[vi++] = maxY;
		v[vi++] = maxZ;

		// top
		t[ti++] = 1f;
		t[ti++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 1f;
		n[ni++] = 0f;
		v[vi++] = maxX;
		v[vi++] = maxY;
		v[vi++] = maxZ;
		t[ti++] = 0f;
		t[ti++] = 0f;
		n[ni++] = 0f;
		n[ni++] = 1f;
		n[ni++] = 0f;
		v[vi++] = minX;
		v[vi++] = maxY;
		v[vi++] = maxZ;
		t[ti++] = 0f;
		t[ti++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 1f;
		n[ni++] = 0f;
		v[vi++] = minX;
		v[vi++] = maxY;
		v[vi++] = minZ;
		t[ti++] = 1f;
		t[ti++] = 1f;
		n[ni++] = 0f;
		n[ni++] = 1f;
		n[ni++] = 0f;
		v[vi++] = maxX;
		v[vi++] = maxY;
		v[vi++] = minZ;

		// bottom
		t[ti++] = 1f;
		t[ti++] = 0f;
		n[ni++] = 0f;
		n[ni++] = -1f;
		n[ni++] = 0f;
		v[vi++] = minX;
		v[vi++] = minY;
		v[vi++] = maxZ;
		t[ti++] = 0f;
		t[ti++] = 0f;
		n[ni++] = 0f;
		n[ni++] = -1f;
		n[ni++] = 0f;
		v[vi++] = maxX;
		v[vi++] = minY;
		v[vi++] = maxZ;
		t[ti++] = 0f;
		t[ti++] = 1f;
		n[ni++] = 0f;
		n[ni++] = -1f;
		n[ni++] = 0f;
		v[vi++] = maxX;
		v[vi++] = minY;
		v[vi++] = minZ;
		t[ti++] = 1f;
		t[ti++] = 1f;
		n[ni++] = 0f;
		n[ni++] = -1f;
		n[ni++] = 0f;
		v[vi++] = minX;
		v[vi++] = minY;
		v[vi++] = minZ;

		// indices
		int[] indices = new int[] { 2, 1, 0, 3, 2, 0, // back
				6, 5, 4, 7, 6, 4, // right
				10, 9, 8, 11, 10, 8, // front
				14, 13, 12, 15, 14, 12, // left
				18, 17, 16, 19, 18, 16, // top
				22, 21, 20, 23, 22, 20 }; // bottom

		this.setVertices(v);
		this.setIndices(indices, PolygonType.TRIANGLES);
		this.setNormals(n);
		this.setTextureCoordinates(0, new VectorBuffer(t, 2));
	}
}
