package com.ferox.renderer.builder;

import com.ferox.renderer.ElementBuffer;

/**
 * ElementBufferBuilder is the builder for {@link ElementBuffer}.
 *
 * @author Michael Ludwig
 */
public interface ElementBufferBuilder extends BufferBuilder<ElementBufferBuilder>, Builder<ElementBuffer> {
    /**
     * Provide the data array that will back the element buffer. The data type will be {@link
     * com.ferox.renderer.DataType#UNSIGNED_INT}. The length of the buffer will be the length of the array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public ElementBufferBuilder fromUnsigned(int[] data);

    /**
     * Provide the data array that will back the element buffer. The data type will be {@link
     * com.ferox.renderer.DataType#UNSIGNED_SHORT}. The length of the buffer will be the length of the array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public ElementBufferBuilder fromUnsigned(short[] data);

    /**
     * Provide the data array that will back the element buffer. The data type will be {@link
     * com.ferox.renderer.DataType#UNSIGNED_BYTE}. The length of the buffer will be the length of the array.
     *
     * @param data The data array
     *
     * @return This builder
     *
     * @throws NullPointerException if data is null
     */
    public ElementBufferBuilder fromUnsigned(byte[] data);
}
