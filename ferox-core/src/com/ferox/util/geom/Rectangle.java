package com.ferox.util.geom;

import com.ferox.math.Vector3f;
import com.ferox.resource.Geometry;
import com.ferox.resource.PolygonType;
import com.ferox.resource.VectorBuffer;

/**
 * <p>
 * A Rectangle is a single quad aligned with a specified x and y axis, in three
 * dimensions. It is very useful for fullscreen effects that require rendering a
 * rectangle across the entire screen.
 * </p>
 * <p>
 * By default, a Rectangle is configured to have its vertices, normals and texture
 * coordinates use the default attribute names defined in Geometry.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Rectangle extends PrimitiveGeometry {
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
		this(xAxis, yAxis, left, right, bottom, top, CompileType.NONE,
			 Geometry.DEFAULT_VERTICES_NAME, Geometry.DEFAULT_NORMALS_NAME, Geometry.DEFAULT_TEXCOORD_NAME);
	}

	/**
	 * Create a Rectangle with the given basis vectors and edge dimensions. It
	 * will also use the given basis vectors and edge dimensions. Unlike the
	 * other constructors which use the default Geometry attribute names, this
	 * one allows you to specify them.
	 * 
	 * @param xAxis
	 * @param yAxis
	 * @param left
	 * @param right
	 * @param bottom
	 * @param top
	 * @param type The compile type to use
	 * @param vertexName The name for the vertex attribute
	 * @param normalName The name for the normals attribute
	 * @param tcName The name for the texture coordinates attribute
	 */
	public Rectangle(Vector3f xAxis, Vector3f yAxis, 
				     float left, float right, float bottom, float top, 
					 CompileType type, String vertexName, String normalName, String tcName) {
		super(type, vertexName, normalName, tcName);
		setData(xAxis, yAxis, left, right, bottom, top);
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
			xAxis = new Vector3f(1f, 0f, 0f);
		if (yAxis == null)
			yAxis = new Vector3f(0f, 1f, 0f);

		Vector3f normal = xAxis.cross(yAxis, null);

		float[] vertices = new float[12];
		float[] normals = new float[12];
		float[] texCoords = new float[8];
		int[] indices = new int[4];

		// lower-left
		vertices[0] = xAxis.x * left + yAxis.x * bottom;
		vertices[1] = xAxis.y * left + yAxis.y * bottom;
		vertices[2] = xAxis.z * left + yAxis.z * bottom;

		normals[0] = normal.x;
		normals[1] = normal.y;
		normals[2] = normal.z;

		texCoords[0] = 0f;
		texCoords[1] = 0f;

		// lower-right
		vertices[3] = xAxis.x * right + yAxis.x * bottom;
		vertices[4] = xAxis.y * right + yAxis.y * bottom;
		vertices[5] = xAxis.z * right + yAxis.z * bottom;

		normals[3] = normal.x;
		normals[4] = normal.y;
		normals[5] = normal.z;

		texCoords[2] = 1f;
		texCoords[3] = 0f;

		// uppper-right
		vertices[6] = xAxis.x * right + yAxis.x * top;
		vertices[7] = xAxis.y * right + yAxis.y * top;
		vertices[8] = xAxis.z * right + yAxis.z * top;

		normals[6] = normal.x;
		normals[7] = normal.y;
		normals[8] = normal.z;

		texCoords[4] = 1f;
		texCoords[5] = 1f;

		// upper-left
		vertices[9] = xAxis.x * left + yAxis.x * top;
		vertices[10] = xAxis.y * left + yAxis.y * top;
		vertices[11] = xAxis.z * left + yAxis.z * top;

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
		
		setAttribute(getVertexName(), new VectorBuffer(vertices, 3));
		setAttribute(getNormalName(), new VectorBuffer(normals, 3));
		setAttribute(getTextureCoordinateName(), new VectorBuffer(texCoords, 2));
		setIndices(indices, PolygonType.QUADS);
	}
}
