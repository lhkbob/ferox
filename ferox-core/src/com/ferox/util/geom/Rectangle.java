package com.ferox.util.geom;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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

		FloatBuffer vertices = newFloatBuffer(12);
		FloatBuffer normals = newFloatBuffer(12);
		FloatBuffer texCoords = newFloatBuffer(8);
		IntBuffer indices = newIntBuffer(4);

		// lower-left
		vertices.put(0, xAxis.x * left + yAxis.x * bottom);
		vertices.put(1, xAxis.y * left + yAxis.y * bottom);
		vertices.put(2, xAxis.z * left + yAxis.z * bottom);

		normals.put(0, normal.x);
		normals.put(1, normal.y);
		normals.put(2, normal.z);

		texCoords.put(0, 0f);
		texCoords.put(1, 0f);

		// lower-right
		vertices.put(3, xAxis.x * right + yAxis.x * bottom);
		vertices.put(4, xAxis.y * right + yAxis.y * bottom);
		vertices.put(5, xAxis.z * right + yAxis.z * bottom);

		normals.put(3, normal.x);
		normals.put(4, normal.y);
		normals.put(5, normal.z);

		texCoords.put(2, 1f);
		texCoords.put(3, 0f);

		// uppper-right
		vertices.put(6, xAxis.x * right + yAxis.x * top);
		vertices.put(7, xAxis.y * right + yAxis.y * top);
		vertices.put(8, xAxis.z * right + yAxis.z * top);

		normals.put(6, normal.x);
		normals.put(7, normal.y);
		normals.put(8, normal.z);

		texCoords.put(4, 1f);
		texCoords.put(5, 1f);

		// upper-left
		vertices.put(9, xAxis.x * left + yAxis.x * top);
		vertices.put(10, xAxis.y * left + yAxis.y * top);
		vertices.put(11, xAxis.z * left + yAxis.z * top);

		normals.put(9, normal.x);
		normals.put(10, normal.y);
		normals.put(11, normal.z);

		texCoords.put(6, 0f);
		texCoords.put(7, 1f);

		// indices
		indices.put(0, 0).put(1, 1).put(2, 2).put(3, 3);
		
		setAttribute(getVertexName(), new VectorBuffer(vertices, 3));
		setAttribute(getNormalName(), new VectorBuffer(normals, 3));
		setAttribute(getTextureCoordinateName(), new VectorBuffer(texCoords, 2));
		setIndices(indices, PolygonType.QUADS);
	}
}
