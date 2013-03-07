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
package com.ferox.resource.data;

/**
 * <p/>
 * BufferData represents a block of primitive memory that can be pushed to the GPU to
 * store texture data, vertex data, or more. Each BufferData instance has a fixed length.
 * <p/>
 * BufferData's store their data in Java primitive arrays. Depending on the data type,
 * different array types must be used. The concrete subclasses of BufferData will expose
 * methods to interact with the arrays.
 * <p/>
 * BufferData is not thread safe, it is assumed that mutations of a BufferData object will
 * be done within the synchronization block of whatever resource owns it.
 *
 * @author Michael Ludwig
 */
public abstract class BufferData {
    /**
     * <p/>
     * DataType represents the supported set of primitive data types that can be
     * transfered to the GPU. Depending on how the data is to be used, some types might be
     * invalid (e.g. a VBO for indices cannot use FLOAT).
     * <p/>
     * For the integral types, signed and unsigned data can be stored using the same data
     * type. The most significant bit that stores sign information in Java is treated by
     * the GPU like any other bit when an unsigned type is required.
     */
    public static enum DataType {
        /**
         * Primitive data is stored in a <code>float[]</code>.
         */
        FLOAT(4),
        /**
         * Primitive data is stored in a <code>int[]</code>. The 32-bit values may be
         * interpreted as signed or unsigned depending on the type of buffer.
         */
        INT(4),
        /**
         * Primitive data is stored in a <code>short[]</code>.   The 16-bit values may be
         * interpreted as signed or unsigned depending on the type of buffer.
         */
        SHORT(2),
        /**
         * Primitive data is stored in a <code>byte[]</code>. The 8-bit values may be
         * interpreted as signed or unsigned depending on the type of buffer.
         */
        BYTE(1);

        private final int byteCount;

        private DataType(int byteCount) {
            this.byteCount = byteCount;
        }

        /**
         * @return The number of bytes used by each primitive
         */
        public int getByteCount() {
            return byteCount;
        }
    }

    private final DataType type;
    private final int length;

    // key is an identity specifier that does not prevent the BufferData from
    // being GC'ed, so the Framework can store the keys instead of the actual
    // BufferData when it tracks a resource's state
    private final Object key = new Object();

    /**
     * <p/>
     * Create a BufferData that will store <var>length</var> primitives of the given data
     * type, <var>type</var>. This constructor verifies that the length is at least 0, but
     * subclasses may further restrict to be greater.
     *
     * @param type   The data type of this BufferData
     * @param length The length of the buffer data
     *
     * @throws NullPointerException     if type is null
     * @throws IllegalArgumentException if length is less than 0
     */
    public BufferData(DataType type, int length) {
        if (type == null) {
            throw new NullPointerException("DataType cannot be null");
        }
        if (length < 0) {
            throw new IllegalArgumentException(
                    "Length must be at least 0, not: " + length);
        }

        this.length = length;
        this.type = type;
    }

    /**
     * <p/>
     * Get a simple object whose identity mirrors the reference identity of this
     * BufferData object. The relationship <code>(buffer1 == buffer2) == (buffer1.getKey()
     * == buffer2.getKey())</code> will always be true. If two keys are the same instance,
     * the producing BufferData objects are the same instance.
     * <p/>
     * This is primarily intended for Framework implementations, so they can store the
     * identify of a BufferData used by a resource without preventing the BufferData from
     * being garbage collected.
     *
     * @return An identity key for this data instance
     */
    public Object getKey() {
        return key;
    }

    /**
     * Get the length of this BufferData. All arrays stored by this buffer data will have
     * this length. This allows BufferData to describe an effective block of memory but
     * have the actual array instance change as needed.
     *
     * @return The size of this BufferData
     */
    public int getLength() {
        return length;
    }

    /**
     * Get the data type of this BufferData. After creation, the data type cannot be
     * changed. This allows the BufferData to describe the data type stored in its
     * effective memory block but have the actual array instance change as needed.
     *
     * @return The data type of this BufferData
     */
    public DataType getDataType() {
        return type;
    }
}
