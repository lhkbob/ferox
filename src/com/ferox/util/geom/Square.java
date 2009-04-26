package com.ferox.util.geom;

import org.openmali.vecmath.Vector3f;

import com.ferox.resource.geometry.AbstractBufferedGeometryDescriptor;
import com.ferox.resource.geometry.BufferedGeometry;
import com.ferox.resource.geometry.BufferedGeometry.PolygonType;

public class Square extends AbstractBufferedGeometryDescriptor {
	private float left;
	private float right;
	private float top;
	private float bottom;

	private final Vector3f yAxis;
	private final Vector3f xAxis;

	private final float[] vertices;
	private final float[] normals;
	private final float[] texCoords;

	private final int[] indices;

	/**
	 * Create a Square with standard basis vectors with its lower left corner at
	 * the origin, extending to <width, height>.
	 */
	public Square(float width, float height) {
		this(0f, width, 0f, height);
	}

	/**
	 * Create a Square with the standard basis vectors with the given edge
	 * dimensions.
	 */
	public Square(float left, float right, float bottom, float top) {
		this(null, null, left, right, bottom, top);
	}

	/**
	 * Create a Square with the given basis vectors and edge dimensions, see
	 * setData().
	 */
	public Square(Vector3f xAxis, Vector3f yAxis, float left, float right,
					float bottom, float top) {
		this.yAxis = new Vector3f();
		this.xAxis = new Vector3f();

		vertices = new float[12];
		normals = new float[12];
		texCoords = new float[8];

		indices = new int[4];

		setData(xAxis, yAxis, left, right, bottom, top);
	}

	/**
	 * Return the local x-axis for this square, it points in the positive x
	 * direction.
	 */
	public Vector3f getXAxis(Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		store.set(xAxis);
		return store;
	}

	/**
	 * Return the local y-axis for this square, it points in the positive y
	 * direction.
	 */
	public Vector3f getYAxis(Vector3f store) {
		if (store == null) {
			store = new Vector3f();
		}
		store.set(yAxis);
		return store;
	}

	/**
	 * Return the left edge's position, relative to this Squares local x-axis.
	 */
	public float getLeft() {
		return left;
	}

	/**
	 * Return the right edge's position, relative to this Squares local x-axis.
	 */
	public float getRight() {
		return right;
	}

	/**
	 * Return the bottom edge's position, relative to this Squares local y-axis.
	 */
	public float getBottom() {
		return bottom;
	}

	/**
	 * Return the top edge's position, relative to this Squares local y-axis.
	 */
	public float getTop() {
		return top;
	}

	/**
	 * Set the data of this Square so that it represents a planar square face
	 * with the given left, right, bottom and top dimensions aligned with the
	 * basis vectors xAxis and yAxis. Throws an exception if left > right or
	 * bottom > top. If xAxis is null, <1, 0, 0> is used. If yAxis is null, <0,
	 * 1, 0> is used.
	 */
	public void setData(Vector3f xAxis, Vector3f yAxis, float left,
					float right, float bottom, float top)
					throws IllegalArgumentException {
		if (left > right || bottom > top) {
			throw new IllegalArgumentException(
							"Side positions of the square are incorrect");
		}

		if (xAxis == null) {
			this.xAxis.set(1f, 0f, 0f);
		} else {
			this.xAxis.set(xAxis);
		}
		if (yAxis == null) {
			this.yAxis.set(0f, 1f, 0f);
		} else {
			this.yAxis.set(yAxis);
		}

		Vector3f normal = new Vector3f();
		normal.cross(this.xAxis, this.yAxis);

		// lower-left
		vertices[0] = this.xAxis.x * left + this.yAxis.x * bottom;
		vertices[1] = this.xAxis.y * left + this.yAxis.y * bottom;
		vertices[2] = this.xAxis.z * left + this.yAxis.z * bottom;

		normals[0] = normal.x;
		normals[1] = normal.y;
		normals[2] = normal.z;

		texCoords[0] = 0f;
		texCoords[1] = 0f;

		// lower-right
		vertices[3] = this.xAxis.x * right + this.yAxis.x * bottom;
		vertices[4] = this.xAxis.y * right + this.yAxis.y * bottom;
		vertices[5] = this.xAxis.z * right + this.yAxis.z * bottom;

		normals[3] = normal.x;
		normals[4] = normal.y;
		normals[5] = normal.z;

		texCoords[2] = 1f;
		texCoords[3] = 0f;

		// uppper-right
		vertices[6] = this.xAxis.x * right + this.yAxis.x * top;
		vertices[7] = this.xAxis.y * right + this.yAxis.y * top;
		vertices[8] = this.xAxis.z * right + this.yAxis.z * top;

		normals[6] = normal.x;
		normals[7] = normal.y;
		normals[8] = normal.z;

		texCoords[4] = 1f;
		texCoords[5] = 1f;

		// upper-left
		vertices[9] = this.xAxis.x * left + this.yAxis.x * top;
		vertices[10] = this.xAxis.y * left + this.yAxis.y * top;
		vertices[11] = this.xAxis.z * left + this.yAxis.z * top;

		normals[9] = normal.x;
		normals[10] = normal.y;
		normals[11] = normal.z;

		texCoords[6] = 0f;
		texCoords[7] = 1f;

		// indices
		indices[0] = 0;
		indices[1] = 1;
		indices[2] = 2;
		indices[3] = 3;

		this.left = left;
		this.right = right;
		this.bottom = bottom;
		this.top = top;
	}

	@Override
	protected float[] internalVertices() {
		return vertices;
	}

	@Override
	protected float[] internalNormals() {
		return normals;
	}

	@Override
	protected float[] internalTexCoords() {
		return texCoords;
	}

	@Override
	protected int[] internalIndices() {
		return indices;
	}

	@Override
	public PolygonType getPolygonType() {
		return PolygonType.QUADS;
	}
}
