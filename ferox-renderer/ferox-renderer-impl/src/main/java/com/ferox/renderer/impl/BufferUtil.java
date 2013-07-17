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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * BufferUtil is a utility class for creating NIO Buffer objects. All created Buffer objects are direct
 * buffers with the native byte ordering.
 *
 * @author Michael Ludwig
 */
public class BufferUtil {
    /**
     * Create a new ByteBuffer of the given number of typed primitives. The actual byte buffer will have a
     * capacity equal to {@code size * type.getByteCount()}. The buffer will be direct with native byte
     * ordering. Although it will be sized for the given type, the correct put() methods on the ByteBuffer
     * must be used as well.
     *
     * @param size The number of primitives for the returned buffer
     *
     * @return A new byte buffer
     */
    public static ByteBuffer newByteBuffer(DataType type, int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(type.getByteCount() * size);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    /**
     * Create a new ByteBuffer that will have the same capacity as the 4 * length of the given array, and its
     * contents will be equal the array. The returned buffer will have its position at 0 and limit at the
     * capacity.
     *
     * @param data The float[] that fills the returned buffer
     *
     * @return A new direct ByteBuffer
     *
     * @throws NullPointerException if data is null
     */
    public static ByteBuffer newFloatBuffer(float[] data) {
        ByteBuffer buffer = newByteBuffer(DataType.FLOAT, data.length);
        buffer.asFloatBuffer().put(data);
        return buffer;
    }

    /**
     * Create a new ByteBuffer that will have the same capacity as the 4 * length of the given array, and its
     * contents will be equal the array. The returned buffer will have its position at 0 and limit at the
     * capacity.
     *
     * @param data The int[] that fills the returned buffer
     *
     * @return A new direct ByteBuffer
     *
     * @throws NullPointerException if data is null
     */
    public static ByteBuffer newIntBuffer(int[] data) {
        ByteBuffer buffer = newByteBuffer(DataType.INT, data.length);
        buffer.asIntBuffer().put(data);
        return buffer;
    }

    /**
     * Create a new ByteBuffer that will have the same capacity as the 2 * length of the given array, and its
     * contents will be equal the array. The returned buffer will have its position at 0 and limit at the
     * capacity.
     *
     * @param data The short[] that fills the returned buffer
     *
     * @return A new direct ByteBuffer
     *
     * @throws NullPointerException if data is null
     */
    public static ByteBuffer newShortBuffer(short[] data) {
        ByteBuffer buffer = newByteBuffer(DataType.SHORT, data.length);
        buffer.asShortBuffer().put(data);
        return buffer;
    }

    /**
     * Create a new ByteBuffer that will have the same capacity as the length of the given array, and its
     * contents will be equal the array. The returned buffer will have its position at 0 and limit at the
     * capacity.
     *
     * @param data The byte[] that fills the returned buffer
     *
     * @return A new direct ByteBuffer
     *
     * @throws NullPointerException if data is null
     */
    public static ByteBuffer newByteBuffer(byte[] data) {
        ByteBuffer buffer = newByteBuffer(DataType.BYTE, data.length);
        buffer.put(data).rewind();
        return buffer;
    }

    /**
     * Create a new ByteBuffer from the primitive array. The array instance must be a {@code int[]}, {@code
     * short[]}, {@code byte[]}, or {@code float[]}. The exact interpretation of the primitives is
     * irrelevant.
     *
     * @param array The primitive to clone into an NIO buffer
     *
     * @return A new direct ByteBuffer
     *
     * @throws IllegalArgumentException if the array isn't an expected buffer array type
     */
    public static ByteBuffer newBuffer(Object array) {
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
     * Get the length of the primitive array. The instance must be one of the array types supported by {@link
     * #newBuffer(Object)}.
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
}
