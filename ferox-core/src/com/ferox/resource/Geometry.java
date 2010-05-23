package com.ferox.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Renderer;

/**
 * <p>
 * Geometry is perhaps one of the most important Resource types that is used by
 * a Framework. A Geometry embodies the shapes and primitives that are rendered
 * onto a {@link Surface} by a {@link Renderer}. The approach taken to
 * describe geometry is as follows:
 * <ul>
 * <li>Every vertex has a set of attributes, which are the same for each vertex
 * of a Geometry instance, but may vary between instances</li>
 * <li>Some examples of attributes may include position, normal, or texture
 * coordinate information. More advanced effects may require tangent vector data
 * or vertex colors.</li>
 * <li>A Geometry has many vertices that are combined together into points,
 * lines, or polygons based on a set of indices.</li>
 * <li>Subsequent elements of a Geometry's indices are combined based on a
 * {@link PolygonType} by looking up the vector data for every attribute at the
 * given index.</li>
 * </ul>
 * </p>
 * <p>
 * This model is a very flexible approach that supports both the legacy
 * fixed-function pipeline and the newer, programmable shader pipeline that
 * allows greater flexibility with defining attributes. Unfortunately this
 * requires that each {@link FixedFunctionRenderer} be configured to lookup the
 * correct attributes for each bindable attribute that's available in the fixed
 * pipeline. To make this easier, Geometry declares three attribute names that
 * are considered 'default' and should be used when a Geometry attribute
 * represents the common concept of a 'vertex', 'normal', or 'texture
 * coordinate'.
 * </p>
 * <p>
 * When a Geometry is updated by a Framework, the Framework must perform some
 * validation of the set of attributes of the Geometry instance. Unless every
 * attribute's data array contains the equivalent number of vectors (after
 * taking into account the element size), the Geometry should have an ERROR
 * status.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Geometry extends Resource {
	/**
	 * CompileType represents the various ways that a Framework can 'compile' a
	 * Geometry resource into something that it can use when rendering them.
	 * There are currently three types, each progressing along the spectrum from
	 * quick updates to faster rendering performance.
	 */
	public static enum CompileType {
		/**
		 * No data is stored on the graphics card. This means that updates are
		 * generally very fast (although a copy may be necessary). Unfortunately
		 * rendering is slower because the data must be resent each render.
		 */
		NONE,
		/**
		 * The Geometry data is stored on the graphics card in specialized
		 * memory designed to be updated frequently. This means that, although
		 * slower than NONE, the updates are faster than RESIDENT_STATIC.
		 * Because it's on the graphics card, rendering times are also much
		 * faster compared to NONE.
		 */
		RESIDENT_DYNAMIC,
		/**
		 * Geometry data is stored on the graphics card in memory designed for
		 * fast read access. This allows rendering to be the most performant,
		 * but updates are slower.
		 */
		RESIDENT_STATIC
	}
	
	/**
	 * The recommended name to use when assigning an attribute that stores
	 * positional information.
	 */
	public static final String DEFAULT_VERTICES_NAME = "vertices";
	/**
	 * The recommended name to use when assigning an attribute that stores
	 * normal vector information.  Here normal refers to the normal vector
	 * of a surface.
	 */
	public static final String DEFAULT_NORMALS_NAME = "normals";
	/**
	 * The recommended name to use when assigning an attribute that stores
	 * texture coordinates.
	 */
	public static final String DEFAULT_TEXCOORD_NAME = "texcoords";
	
	private final CompileType compile;
	private final Map<String, VectorBuffer> attributes;
	private final Map<String, VectorBuffer> readOnlyAttributes;
	
	private int[] indices;
	private PolygonType polyType;
	
	private GeometryDirtyState dirty;

	/**
	 * Create a new Geometry instance that initially has no vertex or index
	 * information. The CompileType of a Geometry is constant and will not
	 * change, so it is required for the constructor.
	 * 
	 * @param compileType The CompileType used when this Geometry is updated by
	 *            a Framework
	 */
	public Geometry(CompileType compileType) {
		compile = (compileType != null ? compileType : CompileType.NONE);
		attributes = new HashMap<String, VectorBuffer>();
		readOnlyAttributes = Collections.unmodifiableMap(attributes);
	}

	/**
	 * <p>
	 * Return the indices used for this Geometry.
	 * </p>
	 * <p>
	 * The vertex information of a Geometry is formed into points, lines, quads
	 * or triangles based on its index information. Each subsequent element in
	 * the returned array looks up a set of vector information from each
	 * relevant vector attribute. Based on the {@link PolygonType} of the
	 * Geometry, this data is then formed into primitive shapes and rendered by
	 * a {@link Renderer}.
	 * </p>
	 * <p>
	 * The indices used to lookup vectors are vector-based values, such that
	 * index 1 is actually taking values from the 3rd, 4th, and 5th elements of
	 * the attribute (assuming that the attribute has an element size of 3). It
	 * is undefined if an index attempts to lookup a vector outside of the range
	 * of an attribute.
	 * </p>
	 * 
	 * @return The indices array for this Geometry
	 */
	public int[] getIndices() {
		return indices;
	}

	/**
	 * Return the PolygonType used by this Geometry. The PolygonType determines
	 * how consecutive indices from {@link #getIndices()} are combined into
	 * primitive shapes. See {@link PolygonType} for more information on each
	 * type.
	 * 
	 * @return The PolygonType used by this Geometry
	 */
	public PolygonType getPolygonType() {
		return polyType;
	}

	/**
	 * Return the number of polygons that will be rendered for this Geometry.
	 * Convenience function for
	 * <code>this.getPolygonType().getPolygonCount(this.getIndices().length);</code>
	 * 
	 * @return The polygon count, or 0 if indices is null
	 */
	public int getPolygonCount() {
		return (indices == null ? 0 : polyType.getPolygonCount(indices.length));
	}

	/**
	 * Set the index information and PolygonType that will be used by this
	 * Geometry. If <tt>type</tt> is null, then {@link PolygonType#POINTS} will
	 * be used instead. It is permissible for the indices to be null, as that is
	 * the initial state that every created Geometry is in. However, a Geometry
	 * that's rendered with null indices will be ignored because it's impossible
	 * to construct meaningful shapes from it.
	 * 
	 * @param indices The new index information for the Geometry
	 * @param type The new PolygonType for the Geometry
	 */
	public void setIndices(int[] indices, PolygonType type) {
		this.indices = indices;
		polyType = (type == null ? PolygonType.POINTS : type);
		markIndicesDirty(0, indices.length);
	}

	/**
	 * Return the current VectorBuffer that's associated with the given
	 * attribute name. A Geometry can have many different vertex attributes that
	 * represents per-vertex vector data. This data is interpreted by a
	 * {@link FixedFunctionRenderer} or a {@link GlslRenderer} based on its
	 * configuration to be used meaningfully.
	 * 
	 * @param name The attribute name, cannot be null
	 * @return The VectorBuffer for the attribute, or null, if no attribute
	 *         exists for this Geometry
	 * @throws NullPointerException if name is null
	 */
	public VectorBuffer getAttribute(String name) {
		if (name == null)
			throw new NullPointerException("Cannot access an attribute with a null name");
		return attributes.get(name);
	}

	/**
	 * <p>
	 * Assign an attribute of the given name the vector values stored in the
	 * VectorBuffer, <tt>values</tt>. If values is null, this is equivalent to
	 * calling {@link #removeAttribute(String)}, otherwise subsequent calls to
	 * {@link #getAttribute(String)} with <tt>name</tt> will return the
	 * specified VectorBuffer.
	 * </p>
	 * <p>
	 * As described in {@link #getIndices()}, vector information is indexed from
	 * a Geometry's set of attributes by vector index. To be meaningful, every
	 * attribute within a Geometry must have the same number of vectors. This is
	 * difficult to verify during each call to
	 * {@link #setAttribute(String, VectorBuffer)}, so it is the Framework's
	 * responsibility to check this during each update.
	 * </p>
	 * 
	 * @param name The attribute name to assign
	 * @param values The VectorBuffer associated with the given name
	 * @throws NullPointerException if name is null
	 */
	public void setAttribute(String name, VectorBuffer values) {
		if (name == null)
			throw new NullPointerException("Cannot assign an attribute with a null name");
		
		if (values != null) {
			VectorBuffer old = attributes.put(name, values);
			
			if (dirty == null)
				dirty = new GeometryDirtyState();
			dirty = dirty.updateAttribute(name, 0, values.getData().length, old == null);
		} else
			removeAttribute(name);
	}

	/**
	 * Remove the VectorBuffer attribute by the given name from this Geometry.
	 * After subsequent updates, this attribute can no longer be used when
	 * rendering the Geometry. If there was no previous attribute by
	 * <tt>name</tt>, then null will be returned, otherwise the old VectorBuffer
	 * is returned.
	 * 
	 * @param name The attribute to remove
	 * @return The VectorBuffer associated with name, or null if there was no
	 *         attribute
	 * @throws NullPointerException if name is null
	 */
	public VectorBuffer removeAttribute(String name) {
		if (name == null)
			throw new NullPointerException("Cannot remove an attribute with a null name");
		
		VectorBuffer rem = attributes.remove(name);
		if (rem != null) {
			if (dirty == null)
				dirty = new GeometryDirtyState();
			dirty = dirty.removeAttribute(name);
		}
		
		return rem;
	}

	/**
	 * Return a read-only Map of all of the attribute names to VectorBuffers for
	 * this Geometry. The returned instance will mirror any calls to
	 * {@link #setAttribute(String, VectorBuffer)} and
	 * {@link #removeAttribute(String)}.
	 * 
	 * @return Every attribute and VectorBuffer of this Geometry
	 */
	public Map<String, VectorBuffer> getAttributes() {
		return readOnlyAttributes;
	}
	
	/**
	 * @return The final CompileType that should inform the Framework how this
	 *         Geometry data should be stored
	 */
	public final CompileType getCompileType() {
		return compile;
	}

	/**
	 * Manually mark a range of this Geometry's indices as dirty. This should be
	 * used if its indices array is the same instance, but some of its values
	 * have been modified. The DirtyState of this Geometry will be updated to
	 * reflect this range.
	 * 
	 * @param offset The offset into indices, this will be clamped to be above 0
	 * @param length The length of the range starting at offset that's been
	 *            modified
	 * @throws IllegalArgumentException if length < 1
	 */
	public void markIndicesDirty(int offset, int length) {
		if (dirty == null)
			dirty = new GeometryDirtyState();
		dirty = dirty.updateIndices(offset, length);
	}

	/**
	 * <p>
	 * Manually mark a range of the given attribute as dirty. This should be
	 * called if the VectorBuffer for the attribute is still used, but some of
	 * its values have been modified. The DirtyState of this Geometry will be
	 * updated to reflect this range. The offset and length refer to actual
	 * array elements of the VectorBuffer's data, and not to the conceptual
	 * vector elements referenced by the Geometry's indices.
	 * </p>
	 * <p>
	 * If <tt>name</tt> is not the name of an attribute within this Geometry,
	 * then nothing is marked as dirty. Because a Geometry cannot have a null
	 * attribute name, a value of null causes this to be a no-op.
	 * </p>
	 * 
	 * @param name The attribute name that's range is marked dirty
	 * @param offset The offset into the attribute, this will be clamped to be
	 *            above 0
	 * @param length The length of the range starting at offset that's been
	 *            modified
	 * @throws IllegalArgumentException if length < 1
	 */
	public void markAttributeDirty(String name, int offset, int length) {
		if (!attributes.containsKey(name))
			return;
		
		if (dirty == null)
			dirty = new GeometryDirtyState();
		dirty = dirty.updateAttribute(name, offset, length, false);
	}

	@Override
	public GeometryDirtyState getDirtyState() {
		GeometryDirtyState d = dirty;
		dirty = null;
		return d;
	}
}
