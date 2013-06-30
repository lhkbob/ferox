package com.ferox.renderer.builder;

import com.ferox.renderer.VertexBuffer;

/**
 * VertexBufferBuilder is the builder for {@link VertexBuffer}.
 *
 * @author Michael Ludwig
 */
public interface VertexBufferBuilder
        extends BufferBuilder<VertexBufferBuilder>, Builder<VertexBuffer> {
    /**
     * Provide the data array that will back the vertex buffer. The data type will be
     * {@link com.ferox.renderer.DataType#FLOAT}. The length of the buffer will be the
     * length of the array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder from(float[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be
     * {@link com.ferox.renderer.DataType#NORMALIZED_INT}. The length of the buffer will
     * be the length of the array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder fromNormalized(int[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be
     * {@link com.ferox.renderer.DataType#NORMALIZED_SHORT}. The length of the buffer will
     * be the length of the array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder fromNormalized(short[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be
     * {@link com.ferox.renderer.DataType#NORMALIZED_BYTE}. The length of the buffer will
     * be the length of the array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder fromNormalized(byte[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be
     * {@link com.ferox.renderer.DataType#INT}. The length of the buffer will be the
     * length of the array. Buffers with this type should only be used with shader
     * attributes of an appropriate type.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder from(int[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be
     * {@link com.ferox.renderer.DataType#SHORT}. The length of the buffer will be the
     * length of the array. Buffers with this type should only be used with shader
     * attributes of an appropriate type.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder from(short[] data);

    /**
     * Provide the data array that will back the vertex buffer. The data type will be
     * {@link com.ferox.renderer.DataType#BYTE}. The length of the buffer will be the
     * length of the array. Buffers with this type should only be used with shader
     * attributes of an appropriate type.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public VertexBufferBuilder from(byte[] data);
}
