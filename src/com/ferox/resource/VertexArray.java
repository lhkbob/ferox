package com.ferox.resource;

import com.ferox.state.StateException;

/** A vertex array provides a flexible indexing system allowing for offsets, 
 * strides and vector element size.
 * 
 * A vector element is a sequence of 'element size' primitive data elements
 * that represent a vector.  This vector may be used for vertices, normals, or colors, etc.
 * 
 * For vertex arrays, stride is the number of primitive data elements
 * between the start of each vector element.  A stride of 0 means that the
 * vector elements are packed together.
 * 
 * Offset is measured as the number of primitive data elements to skip over (not vector elements).
 * 
 * A vertex array holding vertex positions with 3 coords, where each coord is packed 
 * next to each others, would have element size = 3, and a stride of 0.
 * 
 * It is also possible to create interleaved access into a single buffer data using multiple vertex arrays:
 * For example, 3-component normal vectors and interleaved 3-component position vectors could be accessed with,
 * norm = new VertexArray(data, 3, 0, 3)
 * vert = new VertexArray(data, 3, 3, 3)
 * This would represent the pattern nx, ny, nz, vx, vy, vz, ...
 * 
 * @author Michael Ludwig
 *
 */
public class VertexArray {
	private int elementSize;
	private int offset;
	private int stride;
	
	// cached value
	private int accessor;
	
	/** Create a vertex array with the element size, with offset and stride = 0. */
	public VertexArray(int elementSize) {
		this(elementSize, 0, 0);
	}
	
	/** Create a vertex array with the element size, offset and stride. 
	 * ElementSize is clamped to be above 1; offset and stride are clamped
	 * to be above 0. */
	public VertexArray(int elementSize, int offset, int stride) throws StateException {
		this.elementSize = Math.max(1, elementSize);
		this.offset = Math.max(0, offset);
		this.stride = Math.max(0, stride);
		
		this.accessor = this.elementSize + this.stride;
	}
	
	/** Get the offset in primitive elements before the first vector element 
	 * of the vertex array begins. */
	public int getOffset() {
		return this.offset;
	}
	
	/** Get the number of primitive elements between successive vector
	 * elements of this vertex array. */
	public int getStride() {
		return this.stride;
	}
	
	/** Get the number of consecutive primitive elements in each vector element. */
	public int getElementSize() {
		return this.elementSize;
	}
	
	/** Get the number of vector elements that a buffer would hold of the
	 * given primitive count. */
	public int getNumElements(int bufferCapacity) {
		return (bufferCapacity - this.offset) / this.accessor;
	}
	
	/** Compute the primitive index (index into actual buffer data) of the first primitive 
	 * of the given vector element. Subsequent primitives can be accessed from the buffer data
	 * by adding 1 to n to the returned value, to get the 2nd to n+1 component, where n+1 is 
	 * the element size. */
	public int getIndex(int element) {
		switch(this.accessor) {
		case 1:
			return this.offset + element;
		case 2:
			return this.offset + (element << 1);
		case 4:
			return this.offset + (element << 2);
		case 8:
			return this.offset + (element << 3);
		case 16:
			return this.offset + (element << 4);
		default:
			return this.offset + element * this.accessor;
		}
	}
}
