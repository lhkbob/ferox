/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl;


import com.ferox.renderer.DataType;

import java.nio.*;

/**
 * BufferUtil is a utility class for creating NIO Buffer objects. All created Buffer
 * objects are direct buffers with the native byte ordering.
 *
 * @author Michael Ludwig
 */
public class BufferUtil {
    /**
     * Create a new FloatBuffer of the given capacity.
     *
     * @param size The capacity of the returned buffer
     *
     * @return A new direct FloatBuffer
     */
    public static FloatBuffer newFloatBuffer(int size) {
        return newByteBuffer(size * DataType.FLOAT.getByteCount()).asFloatBuffer();
    }

    /**
     * Create a new IntBuffer of the given capacity.
     *
     * @param size The capacity of the returned buffer
     *
     * @return A new direct IntBuffer
     */
    public static IntBuffer newIntBuffer(int size) {
        return newByteBuffer(size * DataType.INT.getByteCount()).asIntBuffer();
    }

    /**
     * Create a new ShortBuffer of the given capacity.
     *
     * @param size The capacity of the returned buffer
     *
     * @return A new direct ShortBuffer
     */
    public static ShortBuffer newShortBuffer(int size) {
        return newByteBuffer(size * DataType.SHORT.getByteCount()).asShortBuffer();
    }

    /**
     * Create a new ByteBuffer of the given capacity.
     *
     * @param size The capacity of the returned buffer
     *
     * @return A new direct ByteBuffer
     */
    public static ByteBuffer newByteBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    /**
     * Create a new FloatBuffer that will have the same capacity as the length of the
     * given array, and its contents will be equal the array. The returned buffer will
     * have its position at 0 and limit at the capacity.
     *
     * @param data The float[] that fills the returned buffer
     *
     * @return A new direct FloatBuffer
     *
     * @throws NullPointerException if data is null
     */
    public static FloatBuffer newFloatBuffer(float[] data) {
        FloatBuffer buffer = newFloatBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }

    /**
     * Create a new IntBuffer that will have the same capacity as the length of the given
     * array, and its contents will be equal the array. The returned buffer will have its
     * position at 0 and limit at the capacity.
     *
     * @param data The int[] that fills the returned buffer
     *
     * @return A new direct IntBuffer
     *
     * @throws NullPointerException if data is null
     */
    public static IntBuffer newIntBuffer(int[] data) {
        IntBuffer buffer = newIntBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }

    /**
     * Create a new ShortBuffer that will have the same capacity as the length of the
     * given array, and its contents will be equal the array. The returned buffer will
     * have its position at 0 and limit at the capacity.
     *
     * @param data The short[] that fills the returned buffer
     *
     * @return A new direct ShortBuffer
     *
     * @throws NullPointerException if data is null
     */
    public static ShortBuffer newShortBuffer(short[] data) {
        ShortBuffer buffer = newShortBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }

    /**
     * Create a new ByteBuffer that will have the same capacity as the length of the given
     * array, and its contents will be equal the array. The returned buffer will have its
     * position at 0 and limit at the capacity.
     *
     * @param data The byte[] that fills the returned buffer
     *
     * @return A new direct ByteBuffer
     *
     * @throws NullPointerException if data is null
     */
    public static ByteBuffer newByteBuffer(byte[] data) {
        ByteBuffer buffer = newByteBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }

    /**
     * Create a new Buffer based on the given {@link DataType} and size. The returned
     * buffer will have a capacity equal to <var>size</var> and the returned buffer type
     * will equal the Class returned by {@link #getBufferType(DataType)}.
     *
     * @param type The DataType controlling the return type of the Buffer
     * @param size The capacity of the buffer
     *
     * @return A new direct buffer, suitable for holding data of the give type
     *
     * @throws NullPointerException if type is null
     */
    public static Buffer newBuffer(DataType type, int size) {
        switch (type) {
        case FLOAT:
            return newFloatBuffer(size);
        case BYTE:
        case NORMALIZED_BYTE:
        case UNSIGNED_BYTE:
        case UNSIGNED_NORMALIZED_BYTE:
            return newByteBuffer(size);
        case INT:
        case NORMALIZED_INT:
        case UNSIGNED_INT:
        case UNSIGNED_NORMALIZED_INT:
        case INT_BIT_FIELD:
            return newIntBuffer(size);
        case SHORT:
        case NORMALIZED_SHORT:
        case UNSIGNED_SHORT:
        case UNSIGNED_NORMALIZED_SHORT:
            return newShortBuffer(size);
        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Create a new Buffer from the primitive array. The array instance must be a {@code
     * int[]}, {@code short[]}, {@code byte[]}, or {@code float[]}. The exact
     * interpretation of the primitives is irrelevant.
     *
     * @param array The primitive to clone into an NIO buffer
     *
     * @return A new direct Buffer
     *
     * @throws IllegalArgumentException if the array isn't an expected buffer array type
     */
    public static Buffer newBuffer(Object array) {
        if (array instanceof float[]) {
            return newFloatBuffer((float[]) array);
        } else if (array instanceof int[]) {
            return newIntBuffer((int[]) array);
        } else if (array instanceof short[]) {
            return newShortBuffer((short[]) array);
        } else if (array instanceof byte[]) {
            return newByteBuffer((byte[]) array);
        } else {
            throw new IllegalArgumentException("Unsupported array type: " + array);
        }
    }

    /**
     * Get the length of the primitive array. The instance must be one of the array types
     * supported by {@link #newBuffer(Object)}.
     *
     * @param array The primitive array
     *
     * @return The length of the array
     *
     * @throws IllegalArgumentException if the array isn't an expected buffer array type
     */
    public static int getArrayLength(Object array) {
        if (array instanceof float[]) {
            return ((float[]) array).length;
        } else if (array instanceof int[]) {
            return ((int[]) array).length;
        } else if (array instanceof short[]) {
            return ((short[]) array).length;
        } else if (array instanceof byte[]) {
            return ((byte[]) array).length;
        } else {
            throw new IllegalArgumentException("Unsupported array type: " + array);
        }
    }

    /**
     * Return the Class of Buffer that will be created by {@link #newBuffer(DataType,
     * int)} based on the given DataType. A DataType of FLOAT creates FloatBuffers; a
     * DataType of BYTE creates ByteBuffers; a type of INT creates IntBuffers; and a type
     * of SHORT creates ShortBuffers.
     *
     * @param type The DataType
     *
     * @return The class of buffer matching the given DataType
     *
     * @throws NullPointerException if type is null
     */
    public static Class<? extends Buffer> getBufferType(DataType type) {
        switch (type) {
        case FLOAT:
            return FloatBuffer.class;
        case BYTE:
        case NORMALIZED_BYTE:
        case UNSIGNED_BYTE:
        case UNSIGNED_NORMALIZED_BYTE:
            return ByteBuffer.class;
        case INT:
        case NORMALIZED_INT:
        case UNSIGNED_INT:
        case UNSIGNED_NORMALIZED_INT:
        case INT_BIT_FIELD:
            return IntBuffer.class;
        case SHORT:
        case NORMALIZED_SHORT:
        case UNSIGNED_SHORT:
        case UNSIGNED_NORMALIZED_SHORT:
            return ShortBuffer.class;
        }

        return null; // won't happen
    }
}
