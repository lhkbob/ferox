package com.ferox.resource;

import com.ferox.math.bounds.Boundable;

/**
 * <p>
 * A Geometry represents an abstract representation of something on the screen.
 * A Geometry is the union of a boundable and a resource. By being a boundable,
 * it allows bounding volumes to be created around it. By extending a resource,
 * creating and modifying the geometry must follow the same process as any other
 * resource, allowing optimizations to be performed.
 * </p>
 * <p>
 * There are two implementations of Geometry available that a Framework must
 * support: PolygonGeometry and IndexArrayGeometry. Instead of implementing more
 * types of Geometry, programmers should sub-class one of those two and
 * automatically configure them.
 * </p>
 * <p>
 * Renderers should ignore RenderAtoms that use Geometries with a status equal
 * to ERROR.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Geometry extends Boundable, Resource {
	/**
	 * Return the CompileType requested by this Geometry. A Geometry should use
	 * the same CompileType for its entire life time to allow for potential
	 * Framework operations.
	 * 
	 * @return The CompileType to use, must not be null, should default to NONE
	 */
	public CompileType getCompileType();

	/**
	 * Geometries have the option to specify a CompileType that tells the
	 * Framework how the Geometry is to be used, allowing it to change internal
	 * data for potentially faster rendering.
	 */
	public static enum CompileType {
		/**
		 * No compiling should be done. NONE is special in that changes made to
		 * the Geometry should be immediately reflected in the rendering,
		 * without needing to call renderer.update().
		 */
		NONE,
		/**
		 * Store the geometry in arrays in client memory for batch rendering.
		 * This is faster than NONE, but slightly less dynamic (you have to call
		 * update(), but it should be a faster operation than other types) and
		 * may take up more memory.
		 */
		VERTEX_ARRAY,
		/**
		 * Like VERTEX_ARRAY but the arrays are stored in the graphics card (no
		 * extra copy in client memory). This is one of the fastest compiling
		 * options but it may not be supported on all graphics cards. Its
		 * updates are slower than VERTEX_ARRAYS and should be used for rarely
		 * updated geometry.
		 */
		VBO_STATIC,
		/**
		 * Like VBO_STATIC except the the graphics card memory used is intended
		 * for more frequent updates. This results in slightly slower rendering,
		 * but faster updates.
		 */
		VBO_DYNAMIC,
		/**
		 * Use a display list to compile all low-level operations need to render
		 * the geometry. This has the slowest update. Performance benefits
		 * largely depend on the hardware but can be quite fast.
		 */
		DISPLAY_LIST
	}
}
