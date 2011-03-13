package com.ferox.resource;

import com.ferox.renderer.Renderer;

/**
 * <p>
 * VertexAttribute wraps a VertexBufferObject so that it can be used as a vertex
 * attribute in a {@link Renderer}. For example, texture coordinates can be made
 * of 1, 2, 3 or Examples of vertex attributes include the actual vertices,
 * normal vectors and texture coordinates. When using programmable shaders,
 * vertex attributes can contain any type of data on a per-vertex basis.
 * </p>
 * <p>
 * Vertex attributes have two sets of indices: indices into the vertices, and
 * indices into the array data defining the vertices. Array indices can be
 * computed from the vertex index, component, and defined element size, stride
 * and offset. The element size is the number of elements or components of the
 * attribute. Attributes can be made of 1 to 4-tuples. The offset is the number
 * of array indices to skip before accessing the first component of the first
 * attribute. The stride is the number of array indices to skip between
 * consecutive attributes. The components of each attribute are in consecutive
 * array indices.
 * </p>
 * <p>
 * Thus, to get the <tt>jth</tt> component of the <tt>ith</tt> attribute, the
 * array index is computed as
 * <code>offset + (elementSize + stride) * i + j</code>. <tt>i</tt> is an index
 * value suitable for use with an index buffer used in rendering. <tt>j</tt> is
 * a component index ranging from 0 to <code>elementSize - 1</code>.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class VertexAttribute {
    private final int elementSize;
    private final int offset;
    private final int stride;
    
    private final VertexBufferObject buffer;

    /**
     * Construct an immutable VertexAttribute with the given buffer and element
     * size. The offset and stride are both 0, so the attributes are packed
     * together in consecutive array elements starting at the first element of
     * the buffer.
     * 
     * @param buffer The VertexBufferObject containing the data for this vertex
     *            attribute
     * @param elementSize The element size of individual vectors in buffer
     * @throws IllegalArgumentException if elementSize isn't in [1, 4]
     * @throws NullPointerException if buffer is null
     */
    public VertexAttribute(VertexBufferObject buffer, int elementSize) {
        this(buffer, elementSize, 0, 0);
    }

    /**
     * Construct an immutable VertexAttribute with the given buffer, element
     * size, offset and stride. The element size must be between 1 and 4. Offset
     * must be at least 0 and cannot be greater than or equal to the buffer's
     * length. Stride must be at least 0. Beyond this, no validation is done
     * because there are too many ways to pack vertex attributes into a single
     * vertex buffer object. Instead it is assumed that the offsets, strides and
     * element indices are assigned properly so that the proper elements are
     * used.
     * 
     * @param buffer The VertexBufferObject containing the data for this vertex
     *            attribute
     * @param elementSize The element size of individual vectors in buffer
     * @param offset The number of array elements to skip before accessing the
     *            first attribute
     * @param stride The number of array elements between consecutive attributes
     * @throws IllegalArgumentException if elementSize isn't in [1, 4], if the
     *             offset is less than 0 or greater than or equal to the
     *             buffer's length, or if stride is less than 0
     * @throws NullPointerException if buffer is null
     */
    public VertexAttribute(VertexBufferObject buffer, int elementSize, int offset, int stride) {
        if (buffer == null)
            throw new NullPointerException("Buffer cannot be null");
        if (elementSize < 1 || elementSize > 4)
            throw new IllegalArgumentException("Illegal element size, must be in [1, 4], not: " + elementSize);
        if (offset < 0 || offset >= buffer.getData().getLength())
            throw new IllegalArgumentException("Illegal offset, must be in [0, " + buffer.getData().getLength() + "), not: " + offset);
        if (stride < 0)
            throw new IllegalArgumentException("Illegal stride, must be at least 0, not: " + stride);
        
        this.offset = offset;
        this.stride = stride;
        this.elementSize = elementSize;
        this.buffer = buffer;
    }

    /**
     * <p>
     * Compute the array index that can be used to lookup the component value
     * for the specific vertex and component. The component must be between 0
     * and <code>({@link #getElementSize()} - 1)</code>. The vertex index must
     * be at least 0.
     * </p>
     * <p>
     * This method does not check if <tt>vertex</tt> is too large, so it is
     * possible for returned array indices to reference elements past the size
     * of the data array.
     * </p>
     * 
     * @param vertex The vertex index, where each value refers to an entire
     *            attribute vector
     * @param component The component within the attribute to look up
     * @return An array index that can be used with the data array backing this
     *         VertexAttribute to lookup the attribute value for the given
     *         vertex and component
     * @throws IndexOutOfBoundsException if vertex is less than 0, or if
     *             component is not in [0, {@link #getElementSize()} - 1]
     */
    public int getArrayIndex(int vertex, int component) {
        if (vertex < 0)
            throw new IndexOutOfBoundsException("Vertex index must be at least 0, not: " + vertex);
        if (component < 0 || component >= elementSize)
            throw new IndexOutOfBoundsException("Component index is out of range: " + component);
        return offset + (elementSize + stride) * vertex + component;
    }
    
    /**
     * @return The number of array elements to skip before accessing vertex data
     */
    public int getOffset() {
        return offset;
    }
    
    /**
     * @return The number of array elements between vertices
     */
    public int getStride() {
        return stride;
    }

    /**
     * @return The element size of the buffer's vectors
     */
    public int getElementSize() {
        return elementSize;
    }

    /**
     * @return The VertexBufferObject holding the vertex attribute data. Use
     *         {@link BufferData#getDataType()} to determine the array type
     *         stored by the BufferData.
     */
    public VertexBufferObject getData() {
        return buffer;
    }
}