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
	
	/** Store this Boundable's bounds into result. It is recommended to
	 * use some caching mechanism so bounds don't have to be recomputed each frame. */
	public void getBounds(BoundVolume result);
	
	/** Get the total number of vertices represented by this Boundable. */
	public int getVertexCount();
	
	/** Get the vertex coordinate (coord) at the given vertex index.
	 * index is a number from 0 to getVertexCount() - 1, and coord is a number
	 * from 0 to getElementSize() - 1:
	 * x = 0
	 * y = 1
	 * z = 2 (if present) default = 0
	 * 
	 * coord must be between 0-3, if not fail.  If this Boundable does
	 * not have a 3rd dimension, return the default value for that coord. 
	 * 
	 * Fail if index isn't between 0 and getVertexCount() - 1, or if coord isn't 0, 1, or 2. */
	public float getVertex(int index, int coord) throws IllegalArgumentException;
}
