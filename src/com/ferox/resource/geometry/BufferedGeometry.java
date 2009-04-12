package com.ferox.resource.geometry;

import java.util.List;

import com.ferox.math.BoundVolume;
import com.ferox.math.Boundable;
import com.ferox.resource.BufferData;
import com.ferox.resource.Geometry;
import com.ferox.resource.GeometryBoundsCache;
import com.ferox.resource.UnitList;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.UnitList.Unit;

/** BufferedGeometry represents the abstract class for geometry that
 * can be represented as blocks/buffers of primitive data, accessed
 * by an array of indices.  This is then interpreted into polygons based
 * on the polygon type of the BufferedGeometry.
 * 
 * None of the methods validate the number of elements in each buffer.
 * This is because if they're are adjusted in odd orders, it may be temporarily
 * broken.  It is the renderer's duty to make sure that the element counts
 * match and set a status of ERROR when updated if they don't.
 * 
 * It is theoretically possible to arrange the bound data and VertexArray's
 * so that its components are all in the same data object with different
 * offsets and strides.  In many simple cases of this, the renderer can
 * provide even more efficient rendering.
 * 
 * BufferedGeometry doesn't use a dirty descriptor by default.
 * 
 * When a buffer is set/bound, a new GeometryArray is created instead of
 * modifying the original array.  This allows the renderer to more easily
 * remember the state of the Geometry when it was updated.
 * 
 * Subclasses should provide a public constructor that takes a BufferedGeometryDescriptor
 * as an argument.
 * 
 * @author Michael Ludwig
 *
 */
public abstract class BufferedGeometry<T> implements Geometry {
	/** Represents how consecutive elements in the geometry's
	 * indices form polygons. */
	public static enum PolygonType {
		POINTS, LINES, TRIANGLES, QUADS, TRIANGLE_STRIP, QUAD_STRIP;
		
		/** Compute the number of polygons, based on the input
		 * vertices.  This assumes that numVertices > 0. */
		public int getPolygonCount(int numVertices) {
			switch(this) {
			case POINTS: return numVertices;
			case LINES: return numVertices >> 1;
			case TRIANGLES: return numVertices / 3;
			case QUADS: return numVertices >> 2;
			
			case TRIANGLE_STRIP: return numVertices - 2;
			case QUAD_STRIP: return (numVertices - 2) >> 1;
			}
			
			return -1;
		}
	}

	/** Store the intersection of a T and its accessing VertexArray,
	 * as well some metadata to make it easier to analyze a BufferedGeometry
	 * without knowing the type T. */
	public static class GeometryArray<T> {
		private T data;
		private DataType type;
		private VertexArray accessor;
		private int elementCount;
		
		private GeometryArray(T data, DataType type, VertexArray accessor, int elementCount) {
			this.data = data;
			this.accessor = accessor;
			this.type = type;
			this.elementCount = elementCount;
		}
		
		/** Return the geometry data array. This will not be null. */
		public T getArray() {
			return this.data;
		}
		
		/** Return the VertexArray accessor into the data array.
		 * This will not be null. */
		public VertexArray getAccessor() {
			return this.accessor;
		}
		
		/** Return the "effective" type of the data represented
		 * in the array of this GeometryArray. */
		public DataType getType() {
			return this.type;
		}
		
		/** Return the number of elements present in the
		 * array, based off of its accessor. */
		public int getElementCount() {
			return this.elementCount;
		}
	}
	
	private final GeometryBoundsCache boundsCache;
	
	private GeometryArray<T> vertices;
	private GeometryArray<T> normals;
	private GeometryArray<T> fogCoords;
	
	private UnitList<GeometryArray<T>> texCoords;
	private UnitList<GeometryArray<T>> vertexAttribs;
	
	private GeometryArray<T> indices;
	private PolygonType type;
	
	// cached counts
	private int polyCount;
	
	private Object resourceData;

	/** Construct a BufferedGeometry with the given vertices and indices.
	 * The constructor requires the vertices and indices because a BufferedGeometry
	 * is not allowed to have null vertices or indices.  The same rules
	 * apply as in setVertices() and setIndices().
	 * 
	 * Any other geometry data must be set after construction time. */
	public BufferedGeometry(T vertices, VertexArray vertAccessor, T indices, VertexArray indexAccessor, PolygonType type) {
		this();
		this.setVertices(vertices, vertAccessor);
		this.setIndices(indices, indexAccessor, type);
	}
	
	/** An internal constructor allowing sub-classes to remove the
	 * argument requirements of vertices and indices. */
	protected BufferedGeometry() {
		this.texCoords = new UnitList<GeometryArray<T>>();
		this.vertexAttribs = new UnitList<GeometryArray<T>>();
		this.boundsCache = new GeometryBoundsCache(this);
	}
	
	/*
	 * Vertices: must not be null, must have an element size of 2, 3, or 4
	 * Type of FLOAT, INT, or SHORT
	 */
	
	/** Get the GeometryArray that holds the vertex information, and
	 * how to access it for this Geometry. */
	public GeometryArray<T> getVertices() {
		return this.vertices;
	}
	
	/** Set the vertex data and its accessor on this Geometry.  The following
	 * rules must be met:
	 * 1. vertices and accessor cannot be null
	 * 2. accessor must have an element size of 2, 3, or 4
	 * 3. vertices' effective type must be FLOAT, INT, or SHORT
	 * 
	 * If the element size is 4, the fourth coordinate represents
	 * the homogenous coordinate of all vertices.  Each other component
	 * will be divided by it, before making the final vertex value.
	 * It should never have a value of 0.
	 * 
	 * This method invokes clearBoundsCache().
	 * 
	 * If these rules aren't met, then an exception is thrown. */
	public void setVertices(T vertices, VertexArray accessor) throws NullPointerException, IllegalArgumentException {
		if (vertices == null)
			throw new NullPointerException("Cannot have have a BufferedGeometry with a null vertices");
		if (accessor == null)
			throw new NullPointerException("Must specify a non-null VertexArray accessor");
		
		DataType type = this.getData(vertices).getType();
		if (type != DataType.FLOAT && type != DataType.INT && type != DataType.SHORT)
			throw new IllegalArgumentException("Invalid type for vertices, expected an effective type of FLOAT, INT, or SHORT; not " + type);
		if (accessor.getElementSize() != 2 && accessor.getElementSize() != 3 && accessor.getElementSize() != 4)
			throw new IllegalArgumentException("VertexArray can only have element sizes of 2, 3, or 4; not: " + accessor.getElementSize());
		
		this.vertices = new GeometryArray<T>(vertices, type, accessor, this.getNumElements(vertices, accessor));
		this.clearBoundsCache();
	}
	
	/*
	 * Normals: can be null, must have an element size of 3
	 * Type of FLOAT, INT, SHORT, BYTE
	 */
	
	/** Get the GeometryArray that holds the normal information, and
	 * how to access it for this Geometry. 
	 * 
	 * If null is returned, it means that this geometry has no normal
	 * information specified. */
	public GeometryArray<T> getNormals() {
		return this.normals;
	}
	
	/** Set the normal data and its accessor on this Geometry.  The following
	 * rules must be met if normals is not null:
	 * 1. accessor can't be null
	 * 2. accessor must have an element size of 3
	 * 3. normal's effective type must be FLOAT, INT, SHORT, or BYTE
	 * 
	 * If normals is null, then accessor is ignored and any previous normal data
	 * is discarded.
	 * 
	 * It is is assumed that the normals have already been normalized.  Undefined
	 * results occur if not (likely it will be odd lighting effects).
	 * 
	 * If these rules aren't met, then an exception is thrown. */
	public void setNormals(T normals, VertexArray accessor) throws NullPointerException, IllegalArgumentException {
		if (normals != null) {
			if (accessor == null)
				throw new NullPointerException("accessor must be non-null if normals isn't null");

			DataType type = this.getData(normals).getType();
			if (type != DataType.FLOAT && type != DataType.INT && type != DataType.SHORT && type != DataType.BYTE)
				throw new IllegalArgumentException("Invalid type for vertices, expected an effective type of FLOAT, INT, SHORT or BYTE; not " + type);
			if (accessor.getElementSize() != 3)
				throw new IllegalArgumentException("VertexArray can only have an element size of 3; not: " + accessor.getElementSize());
			
			this.normals = new GeometryArray<T>(normals, type, accessor, this.getNumElements(normals, accessor));
		} else
			this.normals = null;
	}
	
	/*
	 * Fog coordinates: can be null, must have an element size of 1
	 * Type must be FLOAT
	 */
	
	/** Get the GeometryArray that holds the fog coordinate information, and
	 * how to access it for this Geometry. 
	 * 
	 * If null is returned, it means that this geometry has no fog coordinate
	 * information specified. */
	public GeometryArray<T> getFogCoordinates() {
		return this.fogCoords;
	}
	
	/** Set the fog coordinate data and its accessor on this Geometry.  The following
	 * rules must be met if fog is not null:
	 * 1. accessor can't be null
	 * 2. accessor must have an element size of 1
	 * 3. fog's effective type must be FLOAT
	 * 
	 * If fog is null, then accessor is ignored and any previous normal data
	 * is discarded.
	 * 
	 * For fog coordinates to have any effect, the fog depth source must be
	 * set to FOG_COORDINATE in the active FogReceiver.  If its FRAGMENT_DEPTH,
	 * it will behave as if fog coordinates were null.  It is undefined what happens
	 * if the Geometry has no fog coordinates, and the depth source is FOG_COORDINATE.
	 * 
	 * It is is assumed that the normals have already been normalized.  Undefined
	 * results occur if not (likely it will be odd lighting effects).
	 * 
	 * If these rules aren't met, then an exception is thrown. */
	public void setFogCoordinates(T fog, VertexArray accessor) throws NullPointerException, IllegalArgumentException {
		if (fog != null) {
			if (accessor == null)
				throw new NullPointerException("accessor must be non-null if fog isn't null");

			DataType type = this.getData(fog).getType();
			if (type != DataType.FLOAT)
				throw new IllegalArgumentException("Invalid type for vertices, expected an effective type of FLOAT; not " + type);
			if (accessor.getElementSize() != 1)
				throw new IllegalArgumentException("VertexArray can only have an element size of 1; not: " + accessor.getElementSize());
			
			this.fogCoords = new GeometryArray<T>(fog, type, accessor, this.getNumElements(fog, accessor));
		} else
			this.fogCoords = null;
	}
	
	/*
	 * Texture coordinates: can be null, must have an element size of 1, 2, 3, or 4
	 * Unit must be >= 0. Type of FLOAT, INT, or SHORT.
	 */
	
	/** Get the GeometryArray that holds texture coordinate data for
	 * the given texture unit.  This unit matches the units in a MultiTexture.
	 * 
	 * If null is returned, then there are no texture coordinates for the given
	 * unit.  
	 * 
	 * Throws an exception if unit is < 0. */
	public GeometryArray<T> getTextureCoordinates(int unit) throws IllegalArgumentException {
		return this.texCoords.getItem(unit);
	}
	
	/** Return a list of all bound texture coordinates and the units that they
	 * are bound to.  This will not be null.  If no texture coordinates are bound,
	 * an empty list is returned. 
	 * 
	 * The returned list is immutable. */
	public List<Unit<GeometryArray<T>>> getTextureCoordinates() {
		return this.texCoords.getItems();
	}
	
	/** Set the texture coordinate data and its accessor on this Geometry for
	 * the given texture unit.  The following rules must be met if tcs is not null:
	 * 1. accessor can't be null
	 * 2. accessor must have an element size of 1, 2, or 3
	 * 3. normal's effective type must be FLOAT, INT, or SHORT
	 * 
	 * If tcs is null, then accessor is ignored and any previous texture data
	 * is discarded for the given unit.
	 * 
	 * There is a maximum supported unit, depending on the hardware.  If a coordinate
	 * unit is set that is above that unit, it will be ignored.
	 * 
	 * If these rules aren't met, then an exception is thrown. 
	 * Also, if unit is < 0, then an exception is thrown. */
	public void setTextureCoordinates(int unit, T tcs, VertexArray accessor) throws NullPointerException, IllegalArgumentException {
		if (tcs != null) {
			if (accessor == null)
				throw new NullPointerException("accessor must be non-null if tcs isn't null");
			
			DataType type = this.getData(tcs).getType();
			if (type != DataType.FLOAT && type != DataType.INT && type != DataType.SHORT)
				throw new IllegalArgumentException("Invalid type for vertices, expected an effective type of FLOAT, INT, or SHORT; not " + type);
			if (accessor.getElementSize() != 1 && accessor.getElementSize() != 2 && accessor.getElementSize() != 3)
				throw new IllegalArgumentException("VertexArray can only have an element size of 1, 2, or 3; not: " + accessor.getElementSize());

			this.texCoords.setItem(unit, new GeometryArray<T>(tcs, type, accessor, this.getNumElements(tcs, accessor)));
		} else {
			this.texCoords.setItem(unit, null);
		}
	}
	
	/*
	 * Vertex attributes: can be null, must have an element size of 1, 2, 3, or 4
	 * Unit must be >= 0.
	 */
	
	/** Get the GeometryArray that holds vertex attribute data for
	 * the given vertex attribute.  This unit matches the units of any active
	 * glsl attributes.
	 * 
	 * If null is returned, then there are no vertex attribtues for the given
	 * unit.  
	 * 
	 * Throws an exception if unit is < 0. */
	public GeometryArray<T> getVertexAttributes(int unit) throws IllegalArgumentException {
		return this.vertexAttribs.getItem(unit);
	}
	
	/** Return a list of all bound vertex attributes and the units that they
	 * are bound to.  This will not be null.  If no vertex attributes are bound,
	 * an empty list is returned. 
	 * 
	 * The returned list is immutable. */
	public List<Unit<GeometryArray<T>>> getVertexAttributes() {
		return this.vertexAttribs.getItems();
	}
	
	/** Set the vertex attribute data and its accessor on this Geometry for
	 * the given unit.  The following rules must be met if vas is not null:
	 * 1. accessor can't be null
	 * 2. accessor must have an element size of 1, 2, 3 or 4
	 * 
	 * If vas is null, then accessor is ignored and any previous vertex attribute data
	 * is discarded for the given unit.
	 * 
	 * There is a maximum supported unit, depending on the hardware.  If a coordinate
	 * unit is set that is above that unit, it will be ignored.
	 * 
	 * Matrices can be loaded into shader attributes, too.  An attribute slot corresponds
	 * to a column of the matrix.  The matrix is loaded in column-major order, and columns
	 * must be in consecutive units.
	 * 
	 * If these rules aren't met, then an exception is thrown. 
	 * Also, if unit is < 0, then an exception is thrown. */
	public void setVertexAttributes(int unit, T vas, VertexArray accessor) throws NullPointerException, IllegalArgumentException {
		if (vas != null) {
			if (accessor == null)
				throw new NullPointerException("accessor must be non-null if vas isn't null");
			
			if (accessor.getElementSize() != 1 && accessor.getElementSize() != 2 && accessor.getElementSize() != 3 && accessor.getElementSize() != 4)
				throw new IllegalArgumentException("VertexArray can only have an element size of 1, 2, 3, or 4; not: " + accessor.getElementSize());

			this.vertexAttribs.setItem(unit, new GeometryArray<T>(vas, this.getData(vas).getType(), accessor, this.getNumElements(vas, accessor)));
		} else {
			this.vertexAttribs.setItem(unit, null);
		}
	}
	
	/*
	 * Indices: must not be null, must have an element size of 1, 
	 * Type must be one of UNSIGNED_xyz.
	 */
	
	/** Get the GeometryArray that holds the index information, and
	 * how to access it for this Geometry. */
	public GeometryArray<T> getIndices() {
		return this.indices;
	}
	
	/** Set the index data and its accessor on this Geometry.  The following
	 * rules must be met:
	 * 1. indices and accessor can't be null
	 * 2. accessor must have an element size of 1 and a stride of 0
	 * 3. indices's effective type must be UNSIGNED_INT, UNSIGNED_SHORT, or UNSIGNED_BYTE
	 * 4. polyType cannot be null
	 * 
	 * If these rules aren't met, then an exception is thrown. */
	public void setIndices(T indices, VertexArray accessor, PolygonType polyType) throws NullPointerException, IllegalArgumentException {
		if (indices == null)
			throw new NullPointerException("Cannot have have a BufferedGeometry with a null indices");
		if (accessor == null)
			throw new NullPointerException("Must specify a non-null VertexArray accessor");
		if (polyType == null)
			throw new NullPointerException("Must specify a non-null polygon type");
		
		DataType type = this.getData(indices).getType();
		if (type != DataType.UNSIGNED_INT && type != DataType.UNSIGNED_SHORT && type != DataType.UNSIGNED_BYTE)
			throw new IllegalArgumentException("Invalid type for vertices, expected an effective type of UNSIGNED_INT, UNSIGNED_SHORT, or UNSIGNED_BYTE; not " + type);
		if (accessor.getElementSize() != 1)
			throw new IllegalArgumentException("VertexArray can only have an element size of 1; not: " + accessor.getElementSize());
		if (accessor.getStride() != 0)
			throw new IllegalArgumentException("VertexArray must have a stride of 0; not: " + accessor.getStride());
		
		this.indices = new GeometryArray<T>(indices, type, accessor, this.getNumElements(indices, accessor));
		this.type = polyType;
		this.polyCount = polyType.getPolygonCount(this.indices.getElementCount());
	}
	
	/*
	 * Misc methods
	 */
	
	/** Return the PolygonType for this BufferedGeometry. */
	public PolygonType getPolygonType() {
		return this.type;
	}
	
	/** Return the number of polygons that are present in this BufferedGeometry. */
	public int getPolygonCount() {
		return this.polyCount;
	}
	
	/** Clear the cached bounding volumes.  The next time
	 * getBounds is called with either a AxisAlignedBox,
	 * or BoundSphere, the cached bounds for that type will
	 * be re-computed from scratch. */
	public void clearBoundsCache() {
		this.boundsCache.setCacheDirty();
	}
	
	/*
	 * Internal methods for sub-classes
	 */
	
	/** Return the BufferData associated with the given wrapper
	 * for sub-classes of BufferedGeometry. */
	protected abstract BufferData getData(T data);
	
	/* Return a floating point value stored at the given index in data.
	 * If the data doesn't hold a floating value, it should just cast it
	 * (and not use multiple primitives to create a fp value). 
	 * 
	 * It can be assumed that the data is meant for vertices, and thus
	 * not unsigned. */
	private float get(BufferData data, int index) {
		Object array = data.getData();
		
		if (array != null) {
			switch(data.getType()) {
			case BYTE: case UNSIGNED_BYTE:
				return ((byte[]) array)[index];
			case SHORT: case UNSIGNED_SHORT:
				return ((short[]) array)[index];
			case INT: case UNSIGNED_INT:
				return ((int[]) array)[index];
			case FLOAT:
				return ((float[]) array)[index];
			}
		}
		
		// this is the best we can do
		return 0f;
	}
	
	/* Return the number of vector elements present in the given data,
	 * if were accessed by the given VertexArray. */
	private int getNumElements(T data, VertexArray accessor) {
		return accessor.getNumElements(this.getData(data).getCapacity());
	}
	
	/*
	 * The implementation of Geometry and Boundable
	 */
	
	@Override
	public void getBounds(BoundVolume result) {
		this.boundsCache.getBounds(result);
	}

	@Override
	public float getVertex(int index, int coord) throws IllegalArgumentException {
		if (index < 0 || index >= this.getVertexCount())
			throw new IllegalArgumentException("Illegal vertex index: " + index + " must be in [0, " + this.getVertexCount() + "]");
		if (coord < 0 || coord > 3)
			throw new IllegalArgumentException("Illegal vertex coordinate: " + coord + " must be in [0, 3]");
		
		int vertexSize = this.vertices.accessor.getElementSize();
		
		if (coord >= vertexSize) {
			if (coord == Boundable.Z_COORD)
				return 0f; // missing z
			else
				return 1f; // missing w
		} else {
			// coord is present in this geometry, so we can access the value directly
			return this.get(this.getData(this.vertices.data), this.vertices.accessor.getIndex(index) + coord);
		}
	}

	@Override
	public int getVertexCount() {
		return this.vertices.getElementCount();
	}

	/** Returns null by default. */
	@Override
	public Object getDirtyDescriptor() {
		return null;
	}

	@Override
	public void clearDirtyDescriptor() {
		// do nothing in base class
	}
	
	@Override
	public Object getResourceData() {
		return this.resourceData;
	}

	@Override
	public void setResourceData(Object data) {
		this.resourceData = data;
	}
}
