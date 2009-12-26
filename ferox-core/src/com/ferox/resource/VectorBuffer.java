package com.ferox.resource;

/**
 * VectorBuffer represents a float[] buffer used by Geometry that can have
 * variable sized vectors. For example, texture coordinates can be made of 1, 2,
 * 3 or 4-tuple vectors, while vertices can be made of 2, 3, or 4-tuple vectors.
 * 
 * @author Michael Ludwig
 */
public class VectorBuffer {
	final int elementSize;
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
			throw new IllegalArgumentException("Illegal element size, must be in [1, 4], not: " + elementSize);
		if (buffer == null)
			throw new NullPointerException("Buffer cannot be null");
		if (buffer.length % elementSize != 0)
			throw new IllegalArgumentException("Buffer length is not divisible by elementSize");

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
	public float[] getData() {
		return buffer;
	}
}