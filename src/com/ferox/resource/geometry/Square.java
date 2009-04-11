package com.ferox.resource.geometry;

import org.openmali.vecmath.Vector3f;

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
	
	/** Create a Square with standard basis vectors with its
	 * lower left corner at the origin, extending to <width, height>. */
	public Square(float width, float height) {
		this(0f, width, 0f, height);
	}
	
	/** Create a Square with the standard basis vectors 
	 * with the given edge dimensions. */
	public Square(float left, float right, float bottom, float top) {
		this(null, null, left, right, bottom, top);
	}

	/** Create a Square with the given basis vectors and
	 * edge dimensions, see setData(). */
	public Square(Vector3f xAxis, Vector3f yAxis, float left, float right, float bottom, float top) {
		this.yAxis = new Vector3f();
		this.xAxis = new Vector3f();
		
		this.vertices = new float[12];
		this.normals = new float[12];
		this.texCoords = new float[8];
		
		this.indices = new int[4];
		
		this.setData(xAxis, yAxis, left, right, bottom, top);
	}
	
	/** Return the local x-axis for this square, it points in
	 * the positive x direction. */
	public Vector3f getXAxis(Vector3f store) {
		if (store == null)
			store = new Vector3f();
		store.set(this.xAxis);
		return store;
	}
	
	/** Return the local y-axis for this square, it points in
	 * the positive y direction. */
	public Vector3f getYAxis(Vector3f store) {
		if (store == null)
			store = new Vector3f();
		store.set(this.yAxis);
		return store;
	}
	
	/** Return the left edge's position, relative to this Squares
	 * local x-axis. */
	public float getLeft() {
		return this.left;
	}
	
	/** Return the right edge's position, relative to this Squares
	 * local x-axis. */
	public float getRight() {
		return this.right;
	}
	
	/** Return the bottom edge's position, relative to this Squares
	 * local y-axis. */
	public float getBottom() {
		return this.bottom;
	}
	
	/** Return the top edge's position, relative to this Squares
	 * local y-axis. */
	public float getTop() {
		return this.top;
	}
	
	/** Set the data of this Square so that it represents a planar square face with the given left, right, bottom and
	 * top dimensions aligned with the basis vectors xAxis and yAxis.  Throws an exception if left > right or bottom > top.
	 * If xAxis is null, <1, 0, 0> is used.  If yAxis is null, <0, 1, 0> is used. */
	public void setData(Vector3f xAxis, Vector3f yAxis, float left, float right, float bottom, float top) throws IllegalArgumentException {
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
		
		Vector3f normal = new Vector3f();
		normal.cross(this.xAxis, this.yAxis);
		
		// lower-left
		this.vertices[0] = this.xAxis.x * left + this.yAxis.x * bottom;
		this.vertices[1] = this.xAxis.y * left + this.yAxis.y * bottom;
		this.vertices[2] = this.xAxis.z * left + this.yAxis.z * bottom;
		
		this.normals[0] = normal.x;
		this.normals[1] = normal.y;
		this.normals[2] = normal.z;
		
		this.texCoords[0] = 0f;
		this.texCoords[1] = 0f;
		
		// lower-right
		this.vertices[3] = this.xAxis.x * right + this.yAxis.x * bottom;
		this.vertices[4] = this.xAxis.y * right + this.yAxis.y * bottom;
		this.vertices[5] = this.xAxis.z * right + this.yAxis.z * bottom;
		
		this.normals[3] = normal.x;
		this.normals[4] = normal.y;
		this.normals[5] = normal.z;
		
		this.texCoords[2] = 1f;
		this.texCoords[3] = 0f;
		
		// uppper-right
		this.vertices[6] = this.xAxis.x * right + this.yAxis.x * top;
		this.vertices[7] = this.xAxis.y * right + this.yAxis.y * top;
		this.vertices[8] = this.xAxis.z * right + this.yAxis.z * top;
		
		this.normals[6] = normal.x;
		this.normals[7] = normal.y;
		this.normals[8] = normal.z;
		
		this.texCoords[4] = 1f;
		this.texCoords[5] = 1f;
		
		// upper-left
		this.vertices[9] = this.xAxis.x * left + this.yAxis.x * top;
		this.vertices[10] = this.xAxis.y * left + this.yAxis.y * top;
		this.vertices[11] = this.xAxis.z * left + this.yAxis.z * top;
		
		this.normals[9] = normal.x;
		this.normals[10] = normal.y;
		this.normals[11] = normal.z;
		
		this.texCoords[6] = 0f;
		this.texCoords[7] = 1f;
		
		// indices
		this.indices[0] = 0;
		this.indices[1] = 1;
		this.indices[2] = 2;
		this.indices[3] = 3;
		
		this.left = left;
		this.right = right;
		this.bottom = bottom;
		this.top = top;
	}
	
	@Override
	protected float[] internalVertices() {
		return this.vertices;
	}

	@Override
	protected float[] internalNormals() {
		return this.normals;
	}

	@Override
	protected float[] internalTexCoords() {
		return this.texCoords;
	}

	@Override
	protected int[] internalIndices() {
		return this.indices;
	}
	
	@Override
	public PolygonType getPolygonType() {
		return PolygonType.QUADS;
	}
}
