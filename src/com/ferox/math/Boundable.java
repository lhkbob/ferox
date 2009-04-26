package com.ferox.math;

/**
 * A Boundable object has some notion of bounds attached to it and provides a
 * method for retrieving those bounds into a result BoundVolume.
 * 
 * @author Michael Ludwig
 * 
 */
public interface Boundable {
	public static final int X_COORD = 0;
	public static final int Y_COORD = 1;
	public static final int Z_COORD = 2;

	/**
	 * Store this Boundable's bounds into result. It is recommended to use some
	 * caching mechanism so bounds don't have to be recomputed each frame.
	 * 
	 * @param result Instance to place computed bounds in, do nothing if result
	 *            is null or unsupported
	 */
	public void getBounds(BoundVolume result);

	/**
	 * Get the total number of vertices represented by this Boundable.
	 * 
	 * @return Number of vertices
	 */
	public int getVertexCount();

	/**
	 * Get the vertex coordinate (coord) at the given vertex index. If the
	 * Boundable has no 3rd dimension, use 0 when Z_COORD is requested.
	 * 
	 * @param index Vertex whose coordinate requested; in 0 to getVertexCount()
	 *            - 1
	 * @param coord Coordinate of the vertex to return; X_COORD, Y_COORD, or
	 *            Z_COORD
	 * 
	 * @return Float value for requested vertex and coordinate.
	 * @throws IllegalArgumentException if coord or index are out of range
	 */
	public float getVertex(int index, int coord)
					throws IllegalArgumentException;
}
