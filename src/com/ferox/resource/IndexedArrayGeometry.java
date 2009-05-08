package com.ferox.resource;

import java.util.List;

import com.ferox.util.UnitList;
import com.ferox.util.UnitList.Unit;

/**
 * <p>
 * IndexedArrayGeometry represents geometry that can be represented as blocks of
 * primitive float data, accessed by an array of indices. This is then
 * interpreted into polygons based on the polygon type of the
 * IndexedArrayGeometry. A vertex (or other element) is represented as X
 * consecutive primitives, where X is usage dependent.<br>
 * Here are the accepted element sizes and descriptions of the available
 * buffers:
 * <ul>
 * <li>Vertex - float - 3D position - 3</li>
 * <li>Normal - float - Normal vector at a vertex - 3</li>
 * <li>Texture Coordinate - float - Variable-sized vector to access textures -
 * 1, 2, or 3</li>
 * <li>Vertex Attribute - float - Generic, variable-sized vector for shader
 * programs - 1, 2, 3 or 4</li>
 * <li>Index - int - Element index into the V buffers - 1</li>
 * </ul>
 * </p>
 * <p>
 * None of the setX() methods validate the element/vector count in each buffer.
 * This is because it may take multiple calls to get it into a stable state. It
 * is the renderer's duty to make sure that the element counts match and set a
 * status of ERROR when updated if they don't.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class IndexedArrayGeometry extends AbstractGeometry {
	/**
	 * Represents how consecutive elements in the geometry's indices form
	 * polygons.
	 */
	public static enum PolygonType {
		/** Every index is treated as a single point. */
		POINTS,
		/**
		 * Every two indices form a line, so [i0, i1, i2, i3] creates 2 lines,
		 * one from i0 to i1 and another from i2 to i3.
		 */
		LINES,
		/**
		 * Every three indices form an individual triangle.
		 */
		TRIANGLES,
		/**
		 * Every four indices form a quadrilateral (should be planar and
		 * convex).
		 */
		QUADS,
		/**
		 * The first three indices form a triangle, and then every subsequent
		 * indices forms a triangle with the previous two indices.
		 */
		TRIANGLE_STRIP,
		/**
		 * The first four indices form a quad, and then every two indices form a
		 * quad with the previous two indices.
		 */
		QUAD_STRIP;

		/**
		 * Compute the number of polygons, based on the number of indices. This
		 * assumes that numVertices > 0.
		 * 
		 * @param The number of indices that build a shape with this
		 *            PolygonType.
		 * @return The polygon count
		 */
		public int getPolygonCount(int numIndices) {
			switch (this) {
			case POINTS:
				return numIndices;
			case LINES:
				return numIndices >> 1;
			case TRIANGLES:
				return numIndices / 3;
			case QUADS:
				return numIndices >> 2;

			case TRIANGLE_STRIP:
				return numIndices - 2;
			case QUAD_STRIP:
				return (numIndices - 2) >> 1;
			}

			return -1;
		}
	}

	/**
	 * VectorBuffer represents a float[] buffer used by IndexedArrayGeometry
	 * that can have variable sized vectors. For example, texture coordinates
	 * can be made of 1, 2, or 3 tuple vectors, so it uses VectorBuffer to store
	 * the element size. In contrast, normals and vertices must always be 3
	 * tuples, so we do not need the extra storage of a VectorBuffer.
	 */
	public static class VectorBuffer {
		private final int elementSize;
		private final float[] buffer;

		/**
		 * Construct an immutable VectorBuffer with the given float[] buffer and
		 * vector element size.
		 * 
		 * @param buffer The float data to store
		 * @param elementSize The element size of individual vectors in buffer
		 * @throws IllegalArgumentException if elementSize isn't in [1, 4] or if
		 *             the buffer's length isn't divisible by elementSize
		 * @throws NullPointerException if buffer is null
		 */
		public VectorBuffer(float[] buffer, int elementSize) {
			if (elementSize < 1 || elementSize > 4)
				throw new IllegalArgumentException(
					"Illegal element size, must be in [1, 4], not: "
						+ elementSize);
			if (buffer == null)
				throw new NullPointerException("Buffer cannot be null");
			if (buffer.length % elementSize != 0)
				throw new IllegalArgumentException(
					"Buffer length is not divisible by elementSize");

			this.elementSize = elementSize;
			this.buffer = buffer;
		}

		/**
		 * @return The element size of the buffer's vectors
		 */
		public int getElementSize() {
			return elementSize;
		}

		/**
		 * @return The float[] buffer holding the vector data
		 */
		public float[] getBuffer() {
			return buffer;
		}
	}

	private float[] vertices;
	private float[] normals;

	private final UnitList<VectorBuffer> texCoords;
	private final UnitList<VectorBuffer> vertexAttribs;

	private int[] indices;
	private PolygonType type;

	// cached counts
	private int polyCount;
	private int vertexCount;

	/**
	 * <p>
	 * Construct a IndexedArrayGeometry with the given vertices and indices. The
	 * constructor requires the vertices and indices because a
	 * IndexedArrayGeometry is not allowed to have null vertices or indices. The
	 * same rules apply as in setVertices() and setIndices().
	 * </p>
	 * <p>
	 * Any other geometry data must be set after construction time.
	 * </p>
	 * 
	 * @param vertices The vertices to use, can't be null
	 * @param normals The normals to use
	 * @param indices The indices to use, can't be null
	 * @param type The type to use for accessing indices
	 * @throws NullPointerException if indices or vertices are null
	 */
	public IndexedArrayGeometry(float[] vertices, float[] normals,
		int[] indices, PolygonType type, CompileType compileType) {
		this(compileType);
		setVertices(vertices);
		setNormals(normals);
		setIndices(indices, type);
	}

	/**
	 * An internal constructor allowing sub-classes to remove the argument
	 * requirements of vertices and indices. If they use this constructor, they
	 * must set vertices and indices before the sub-classed constructor
	 * completes.
	 */
	protected IndexedArrayGeometry(CompileType compileType) {
		super(compileType);

		texCoords = new UnitList<VectorBuffer>();
		vertexAttribs = new UnitList<VectorBuffer>();
	}

	/**
	 * Get the vertex data for this geometry.
	 * 
	 * @return A non-null V holding the data
	 */
	public float[] getVertices() {
		return vertices;
	}

	/**
	 * <p>
	 * Set the vertex data and for this Geometry. Each vertex is represented as
	 * 3 consecutive primitives, starting at the beginning of vertices.
	 * </p>
	 * <p>
	 * This method invokes clearBoundsCache().
	 * </p>
	 * 
	 * @param vertices The new vertex data, can't be null
	 */
	public void setVertices(float[] vertices) {
		if (vertices == null)
			throw new NullPointerException(
				"Cannot have have a IndexedArrayGeometry with a null vertices");

		this.vertices = vertices;
		vertexCount = vertices.length / 3;
		clearBoundsCache();
	}

	/**
	 * <p>
	 * Get the normal data for this geometry.
	 * </p>
	 * <p>
	 * If null is returned, it means that this Geometry is not intended to be
	 * lit. It is the programmer's responsibility to not light the geometry, or
	 * undefined results will occur (since the normals to use are undefined).
	 * </p>
	 * 
	 * @return The normal data, or null if none was specified
	 */
	public float[] getNormals() {
		return normals;
	}

	/**
	 * <p>
	 * Set the normal data that is used by this Geometry. If normals is null,
	 * then this geometry should not be lit, since the normals are undefined.
	 * </p>
	 * <p>
	 * Normals are represented as 3 consecutive primitives, starting at the
	 * beginning of normals.
	 * </p>
	 * <p>
	 * It is is assumed that the normals have already been normalized. Undefined
	 * results occur if not (likely it will be odd lighting effects).
	 * </p>
	 * 
	 * @param normals The new normal data, or null
	 */
	public void setNormals(float[] normals) {
		this.normals = normals;
	}

	/**
	 * <p>
	 * Get the VectorBuffer for the given texture unit. The VectorBuffer holds
	 * the texture coordinate data and the length of the texture coordinate
	 * tuple.
	 * </p>
	 * <p>
	 * If null is returned, then there are no texture coordinates for the given
	 * unit.
	 * </p>
	 * 
	 * @param unit The unit that the texture coordinates apply to, corresponds
	 *            to the units used in multitexturing.
	 * @return The VectorBuffer holding texture coordinate info for unit
	 * @throws IllegalArgumentException if unit < 0
	 */
	public VectorBuffer getTextureCoordinates(int unit) {
		return texCoords.getItem(unit);
	}

	/**
	 * <p>
	 * Set the texture coordinate data (including the element size of a texture
	 * coordinate) for the given unit. If data is null, then this Geometry will
	 * no longer have texture coordinates for the given unit.
	 * </p>
	 * <p>
	 * There is a maximum supported texture unit, depending on the current
	 * hardware. If unit is larger than this, it is stored in the Geometry, but
	 * it should be ignored by the Renderer.
	 * </p>
	 * 
	 * @param unit The texture unit that will hold onto data
	 * @param data The VectorBuffer storing the data and vector size
	 * @throws IllegalArgumentException if unit < 0 or if the vector size is 4
	 */
	public void setTextureCoordinates(int unit, VectorBuffer data) {
		if (data != null && data.elementSize == 4)
			throw new IllegalArgumentException(
				"Texture coordinates cannot be 4-element vectors");
		texCoords.setItem(unit, data);
	}

	/**
	 * Get all currently bound texture coordinate buffers and the units they are
	 * applied to. The returned list is unmodified, and the unit values are in
	 * no particular order.
	 * 
	 * @return Unmodifiable list of all currently bound texture coords
	 */
	public List<Unit<VectorBuffer>> getTextureCoordinates() {
		return texCoords.getItems();
	}

	/**
	 * <p>
	 * Get the VectorBuffer that holds vertex attribute data for the given
	 * vertex attribute. This unit matches the units of any active glsl
	 * attributes.
	 * </p>
	 * <p>
	 * If null is returned, then there are no vertex attribtues for the given
	 * unit.
	 * </p>
	 * 
	 * @param unit The generic glsl vertex attribute
	 * @return The VectorBuffer assigned to that unit
	 * @throws IllegalArgumentException if unit < 1
	 */
	public VectorBuffer getVertexAttributes(int unit) {
		if (unit < 1)
			throw new IllegalArgumentException(
				"Glsl attributes start at 1, unit is invalid: " + unit);
		return vertexAttribs.getItem(unit);
	}

	/**
	 * Get all currently bound vertex attribute buffers and the units they are
	 * applied to. The returned list is unmodified, and the unit values are in
	 * no particular order.
	 * 
	 * @return Unmodifiable list of all currently bound texture coords
	 */
	public List<Unit<VectorBuffer>> getVertexAttributes() {
		return vertexAttribs.getItems();
	}

	/**
	 * <p>
	 * Set the vertex attribute data (including the element size of an
	 * attribute) for the given unit. If data is null, then this Geometry will
	 * no longer have an attribute for the given unit.
	 * </p>
	 * <p>
	 * Matrices can be loaded into shader attributes, too. An attribute slot
	 * corresponds to a column of the matrix. The matrix is loaded in
	 * column-major order, and columns must be in consecutive units.
	 * </p>
	 * <p>
	 * There is a maximum supported attribute unit, depending on the current
	 * hardware. If unit is larger than this, it is stored in the Geometry, but
	 * it should be ignored by the Renderer.
	 * </p>
	 * 
	 * @param unit The attribute unit that will hold onto data
	 * @param data The VectorBuffer storing the data and vector size
	 * @throws IllegalArgumentException if unit < 1
	 */
	public void setVertexAttributes(int unit, VectorBuffer data) {
		vertexAttribs.setItem(unit, data);
	}

	/**
	 * Get the indices array that describes how to access the float[] buffers of
	 * this geometry to build meaningful shapes (depending on the polygon type).
	 * 
	 * @return The indices int[] array
	 */
	public int[] getIndices() {
		return indices;
	}

	/**
	 * <p>
	 * Set the index data and polygon type for this IndexedArrayGeometry.
	 * Consecutive indices will build primitive shapes (triangles, quads, lines
	 * or points) depending on polyType.
	 * </p>
	 * <p>
	 * The values in indices are treated as unsigned integers when accessing the
	 * array.
	 * </p>
	 * 
	 * @param indices The new index array to use
	 * @param polyType The polygon type that determines how indices are used
	 * @throws NullPointerException if indices or polyType are null
	 */
	public void setIndices(int[] indices, PolygonType polyType) {
		if (indices == null)
			throw new NullPointerException(
				"Cannot have have a IndexedArrayGeometry with a null indices");
		if (polyType == null)
			throw new NullPointerException(
				"Must specify a non-null polygon type");

		this.indices = indices;
		type = polyType;
		polyCount = polyType.getPolygonCount(indices.length);
	}

	/*
	 * Misc methods
	 */

	/** @return The PolygonType for this IndexedArrayGeometry. */
	public PolygonType getPolygonType() {
		return type;
	}

	/**
	 * @return The number of polygons that are present in this
	 *         IndexedArrayGeometry.
	 */
	public int getPolygonCount() {
		return polyCount;
	}

	@Override
	public float getVertex(int index, int coord) {
		if (index < 0 || index >= getVertexCount())
			throw new IllegalArgumentException("Illegal vertex index: " + index
				+ " must be in [0, " + getVertexCount() + "]");
		if (coord < 0 || coord > 2)
			throw new IllegalArgumentException("Illegal vertex coordinate: "
				+ coord + " must be in [0, 2]");

		return vertices[index * 3 + coord];
	}

	@Override
	public int getVertexCount() {
		return vertexCount;
	}
}
