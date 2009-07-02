package com.ferox.util.geom;

import com.ferox.math.Vector3f;
import com.ferox.resource.IndexedArrayGeometry;

/**
 * A Rectangle is a single quad aligned with a specified x and y axis, in three
 * dimensions. It is very useful for fullscreen effects that require rendering a
 * rectangle across the entire screen.
 * 
 * @author Michael Ludwig
 */
public class Rectangle extends IndexedArrayGeometry {
	private float left;
	private float right;
	private float top;
	private float bottom;

	private final Vector3f yAxis;
	private final Vector3f xAxis;

	/**
	 * Create a Rectangle with standard basis vectors with its lower left corner
	 * at the origin, extending to <width, height>.
	 * 
	 * @param width The width of the rectangle
	 * @param height The height of the rectangle
	 * @throws IllegalArgumentException if width or height < 0
	 */
	public Rectangle(float width, float height) {
		this(0f, width, 0f, height);
	}

	/**
	 * Create a Rectangle with the standard basis vectors with the given edge
	 * dimensions.
	 * 
	 * @param left The left edge of the rectangle
	 * @param right The right edge of the rectangle
	 * @param bottom The bottom edge of the rectangle
	 * @param top The top edge of the rectangle
	 * @throws IllegalArgumentException if left > right or bottom > top
	 */
	public Rectangle(float left, float right, float bottom, float top) {
		this(null, null, left, right, bottom, top);
	}

	/**
	 * Create a Rectangle with the given basis vectors and edge dimensions.
	 * 
	 * @see #setData(Vector3f, Vector3f, float, float, float, float)
	 * @param xAxis Local x-axis of the rectangle, null = <1, 0, 0>
	 * @param yAxis Local y-axis of the rectangle, null = <0, 1, 0>
	 * @param left The left edge of the rectangle
	 * @param right The right edge of the rectangle
	 * @param bottom The bottom edge of the rectangle
	 * @param top The top edge of the rectangle
	 * @throws IllegalArgumentException if left > right or bottom > top
	 */
	public Rectangle(Vector3f xAxis, Vector3f yAxis, 
					 float left, float right, float bottom, float top) {
		super(CompileType.NONE);
		this.yAxis = new Vector3f();
		this.xAxis = new Vector3f();

		setData(xAxis, yAxis, left, right, bottom, top);
	}

	/**
	 * Return the local x-axis for this square, it points in the positive x
	 * direction.
	 * 
	 * @param store The vector to hold the x axis, or null if a new vector is to
	 *            be used
	 * @return The x axis, held in store or a new vector if it was null
	 */
	public Vector3f getXAxis(Vector3f store) {
		if (store == null)
			store = new Vector3f();
		store.set(xAxis);
		return store;
	}

	/**
	 * Return the local y-axis for this square, it points in the positive y
	 * direction.
	 * 
	 * @param store The vector to hold the y axis, or null if a new vector is to
	 *            be used
	 * @return The y axis, held in store or a new vector if it was null
	 */
	public Vector3f getYAxis(Vector3f store) {
		if (store == null)
			store = new Vector3f();
		store.set(yAxis);
		return store;
	}

	/**
	 * @return the left edge's position, relative to this Rectangle's local
	 *         x-axis.
	 */
	public float getLeft() {
		return left;
	}

	/**
	 * @return the right edge's position, relative to this Rectangle's local
	 *         x-axis.
	 */
	public float getRight() {
		return right;
	}

	/**
	 * @return the bottom edge's position, relative to this Rectangle's local
	 *         y-axis.
	 */
	public float getBottom() {
		return bottom;
	}

	/**
	 * @return the top edge's position, relative to this Rectangle's local
	 *         y-axis.
	 */
	public float getTop() {
		return top;
	}

	/**
	 * Set the data of this Rectangle so that it represents a planar square face
	 * with the given left, right, bottom and top dimensions aligned with the
	 * basis vectors xAxis and yAxis.
	 * 
	 * @param xAxis Local x-axis of the rectangle, null = <1, 0, 0>
	 * @param yAxis Local y-axis of the rectangle, null = <0, 1, 0>
	 * @param left The left edge of the rectangle
	 * @param right The right edge of the rectangle
	 * @param bottom The bottom edge of the rectangle
	 * @param top The top edge of the rectangle
	 * @throws IllegalArgumentException if left > right or bottom > top
	 */
	public void setData(Vector3f xAxis, Vector3f yAxis, 
						float left, float right, float bottom, float top) {
		if (left > right || bottom > top)
			throw new IllegalArgumentException("Side positions of the square are incorrect");

		if (xAxis == null)
			this.xAxis.set(1f, 0f, 0f);
		else
			this.xAxis.set(xAxis);
		if (yAxis == null)
			this.yAxis.set(0f, 1f, 0f);
		else
			this.yAxis.set(yAxis);

		Vector3f normal = this.xAxis.cross(this.yAxis, null);

		float[] vertices = new float[12];
		float[] normals = new float[12];
		float[] texCoords = new float[8];
		int[] indices = new int[4];

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

		setVertices(vertices);
		setNormals(normals);
		setTextureCoordinates(0, new VectorBuffer(texCoords, 2));
		setIndices(indices, PolygonType.QUADS);

		this.left = left;
		this.right = right;
		this.bottom = bottom;
		this.top = top;
	}
}
