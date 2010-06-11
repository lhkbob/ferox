package com.ferox.resource;

import java.nio.FloatBuffer;

/**
 * VectorBuffer represents a FloatBuffer used by Geometry that can have variable
 * sized vectors. For example, texture coordinates can be made of 1, 2, 3 or
 * 4-tuple vectors, while vertices can be made of 2, 3, or 4-tuple vectors. Like
 * the indices used in Geometry, the vertices are taken from position 0 to the
 * capacity of the buffer; position and limit are ignored. Similarly, the
 * FloatBuffers used by VectorBuffer must be direct.
 * 
 * @author Michael Ludwig
 */
public class VectorBuffer {
	final int elementSize;
	private final FloatBuffer buffer;

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
	public VectorBuffer(FloatBuffer buffer, int elementSize) {
		if (elementSize < 1 || elementSize > 4)
			throw new IllegalArgumentException("Illegal element size, must be in [1, 4], not: " + elementSize);
		if (buffer == null)
			throw new NullPointerException("Buffer cannot be null");
		if (buffer.capacity() % elementSize != 0)
			throw new IllegalArgumentException("Buffer length is not divisible by elementSize");
		if (!buffer.isDirect())
		    throw new IllegalArgumentException("Buffer must be direct");
		
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
	 * @return The FloatBuffer holding the vector data
	 */
	public FloatBuffer getData() {
		return buffer;
	}
}