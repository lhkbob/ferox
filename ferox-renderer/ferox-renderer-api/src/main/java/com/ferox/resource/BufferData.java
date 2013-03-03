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
package com.ferox.resource;

/**
 * <p/>
 * BufferData represents a block of primitive memory that can be pushed to the GPU to
 * store texture data, vertex data, or more. Each BufferData instance has a fixed length
 * and data type to allow Frameworks to safely allocate internal or GPU storage.
 * <p/>
 * BufferData's store their data in Java primitive arrays. Depending on the data type,
 * different array types must be used.
 * <p/>
 * BufferData is not thread safe, it is assumed that mutations of a BufferData object will
 * be done within the synchronization block of whatever resource owns it.
 *
 * @author Michael Ludwig
 */
public class BufferData {
    public static double MAX_UBYTE = (double) Byte.MAX_VALUE - (double) Byte.MIN_VALUE;
    public static double MAX_USHORT = (double) Short.MAX_VALUE - (double) Short.MIN_VALUE;
    public static double MAX_UINT =
            (double) Integer.MAX_VALUE - (double) Integer.MIN_VALUE;

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
         * Primitive data is stored in a <code>int[]</code>. When the data is used to
         * encode colors for texture data or indices for rendering, the values are treated
         * as unsigned 32-bit integers. You can use the data views provided by {@link
         * BufferData#getTextureView()} and {@link BufferData#getUnsignedView()} to help
         * with this.
         */
        INT(4),
        /**
         * Primitive data is stored in a <code>short[]</code>. When the data is used to
         * encode colors for texture data or indices for rendering, the values are treated
         * as unsigned 16-bit integers. You can use the data views provided by {@link
         * BufferData#getTextureView()} and {@link BufferData#getUnsignedView()} to help
         * with this.
         */
        SHORT(2),
        /**
         * Primitive data is stored in a <code>byte[]</code>. When the data is used to
         * encode colors for texture data or indices for rendering, the values are treated
         * as unsigned 8-bit integers. You can use the data views provided by {@link
         * BufferData#getTextureView()} and {@link BufferData#getUnsignedView()} to help
         * with this.
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

    private Object data; // actually a primitive array, of different types
    private final int length;
    private final DataType type;

    // key is an identity specifier that does not prevent the BufferData from
    // being GC'ed, so the Framework can store the keys instead of the actual
    // BufferData when it tracks a resource's state
    private final Object key = new Object();

    /**
     * <p/>
     * Create a BufferData that uses the given float[] as its data store. The DataType of
     * the BufferData will be FLOAT, and its length will equal that of the array.
     * <p/>
     * Any changes to the primitives in the array will affect the BufferData. Although the
     * array reference can be changed later with {@link #setData(float[])}, the length and
     * type of the BufferData cannot.
     *
     * @param data The initial float data
     *
     * @throws NullPointerException if data is null
     */
    public BufferData(float[] data) {
        if (data == null) {
            throw new NullPointerException("Array cannot be null");
        }
        this.data = data;
        length = data.length;
        type = DataType.FLOAT;
    }

    /**
     * <p/>
     * Create a BufferData that uses the given int[] as its data store. The DataType of
     * the BufferData will be INT, and its length will equal that of the array.
     * <p/>
     * Any changes to the values in the array will affect the BufferData. Although the
     * array reference can be changed later with {@link #setData(int[])}, the length and
     * type of the BufferData cannot.
     *
     * @param data The initial unsigned int data
     *
     * @throws NullPointerException if data is null
     */
    public BufferData(int[] data) {
        if (data == null) {
            throw new NullPointerException("Array cannot be null");
        }
        this.data = data;
        length = data.length;
        type = DataType.INT;
    }

    /**
     * <p/>
     * Create a BufferData that uses the given short[] as its data store. The DataType of
     * the BufferData will be SHORT, and its length will equal that of the array.
     * <p/>
     * Any changes to the primitives in the array will affect the BufferData. Although the
     * array reference can be changed later with {@link #setData(short[])}, the length and
     * type of the BufferData cannot.
     *
     * @param data The initial unsigned short data
     *
     * @throws NullPointerException if data is null
     */
    public BufferData(short[] data) {
        if (data == null) {
            throw new NullPointerException("Array cannot be null");
        }
        this.data = data;
        length = data.length;
        type = DataType.SHORT;
    }

    /**
     * <p/>
     * Create a BufferData that uses the given byte[] as its data store. The DataType of
     * the BufferData will be BYTE, and its length will equal that of the array.
     * <p/>
     * Any changes to the primitives in the array will affect the BufferData. Although the
     * array reference can be changed later with {@link #setData(byte[])}, the length and
     * type of the BufferData cannot.
     *
     * @param data The initial unsigned byte data
     *
     * @throws NullPointerException if data is null
     */
    public BufferData(byte[] data) {
        if (data == null) {
            throw new NullPointerException("Array cannot be null");
        }
        this.data = data;
        length = data.length;
        type = DataType.BYTE;
    }

    /**
     * <p/>
     * Create a BufferData with a null internal array, but will be configured to have the
     * given DataType and length. These parameters will control the type of array that can
     * be assigned later when actual data is available.
     * <p/>
     * The constructor can be used when the actual data never resides in memory but exists
     * only on the GPU, such as with an FBO used in render-to-texture features.
     *
     * @param type   The data type of this BufferData
     * @param length The length of the buffer data
     *
     * @throws NullPointerException     if type is null
     * @throws IllegalArgumentException if length is less than 1
     */
    public BufferData(DataType type, int length) {
        if (type == null) {
            throw new NullPointerException("DataType cannot be null");
        }
        if (length < 1) {
            throw new IllegalArgumentException(
                    "Length must be at least 1, not: " + length);
        }

        this.length = length;
        this.type = type;
        data = null;
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
     * Get the length or size of this BufferData. All arrays stored by this buffer data
     * will have this length. This allows BufferData to describe an effective block of
     * memory but have the actual array underneath change as needed.
     *
     * @return The size of this BufferData
     */
    public int getLength() {
        return length;
    }

    /**
     * Get the data type of this BufferData. After creation, the data type cannot be
     * changed. This allows the BufferData to describe the data type stored in its
     * effective memory block but have the actual array underneath change as needed.
     *
     * @return The data type of this BufferData
     */
    public DataType getDataType() {
        return type;
    }

    /**
     * <p/>
     * Get the current data array that this BufferData wraps. If the returned array is not
     * null, its length will equal the return value of {@link #getLength()}.
     * <p/>
     * This method uses an unchecked generic cast to make it more convenient to get at the
     * array. Care must be given to use the proper type parameter based on the data type
     * of the buffer data. <ul> <li>If T = float[], then data type must be FLOAT</li>
     * <li>If T = int[], then data type must be INT</li> <li>If T = short[], then data
     * type must be SHORT</li> <li>If T = byte[], then data type must be BYTE</li> </ul>
     *
     * @return The backing data array, which might be null
     *
     * @throws ClassCastException if the improper array type is expected
     */
    @SuppressWarnings("unchecked")
    public <T> T getArray() {
        return (T) data;
    }

    /**
     * Set the data this BufferData wraps to the given array. The given array must have a
     * length equal to the value returned by {@link #getLength()}. The data type of this
     * BufferData must be FLOAT.
     *
     * @param data The new float array
     *
     * @throws IllegalStateException    if this BufferData's type is not FLOAT
     * @throws IllegalArgumentException if the array length is not equal to the buffer's
     *                                  length
     */
    public void setData(float[] data) {
        setData(data, (data == null ? length : data.length), DataType.FLOAT);
    }

    /**
     * Set the data this BufferData wraps to the given array. The given array must have a
     * length equal to the value returned by {@link #getLength()}. The data type of this
     * BufferData must be INT.
     *
     * @param data The new int array
     *
     * @throws IllegalStateException    if this BufferData's type is not INT
     * @throws IllegalArgumentException if the array length is not equal to the buffer's
     *                                  length
     */
    public void setData(int[] data) {
        setData(data, (data == null ? length : data.length), DataType.INT);
    }

    /**
     * Set the data this BufferData wraps to the given array. The given array must have a
     * length equal to the value returned by {@link #getLength()}. The data type of this
     * BufferData must be SHORT.
     *
     * @param data The new short array
     *
     * @throws IllegalStateException    if this BufferData's type is not SHORT
     * @throws IllegalArgumentException if the array length is not equal to the buffer's
     *                                  length
     */
    public void setData(short[] data) {
        setData(data, (data == null ? length : data.length), DataType.SHORT);
    }

    /**
     * Set the data this BufferData wraps to the given array. The given array must have a
     * length equal to the value returned by {@link #getLength()}. The data type of this
     * BufferData must be BYTE.
     *
     * @param data The new byte array
     *
     * @throws IllegalStateException    if this BufferData's type is not BYTE
     * @throws IllegalArgumentException if the array length is not equal to the buffer's
     *                                  length
     */
    public void setData(byte[] data) {
        setData(data, (data == null ? length : data.length), DataType.BYTE);
    }

    /**
     * <p/>
     * Return a view over this data that will convert the data to and from doubles in the
     * range of 0 to 1. This is intended for texture data storage where primitives hold
     * color component values from 0 to 1.
     * <p/>
     * For the integral data types, it treats the bits as unsigned integers and converts
     * them to floating point according to the OpenGL spec for converting integers to
     * floating points. Note that this is dependent on the number of bits in the data.
     * <p/>
     * As an example, a byte data buffer can hold values from 0 to 255 (unsigned), and
     * during conversion, 0 becomes 0.0 and 255 becomes 1.0. An int data buffer would not
     * consider 255 to be 1.0.
     * <p/>
     * Note that this view assumes that each primitive represents a single color
     * component, and will not function as expected when used with texture formats that
     * pack multiple components into a single primitive, or are compressed
     *
     * @return A data view to make accessing/mutating data more convenient when the data
     *         stores texture color data
     *
     * @see TextureFormat
     */
    public TextureDataView getTextureView() {
        switch (type) {
        case BYTE:
            return new TextureDataView() {
                @Override
                public BufferData getBufferData() {
                    return BufferData.this;
                }

                @Override
                public double get(int index) {
                    int unsigned = 0xff & ((byte[]) data)[index];
                    return unsigned / MAX_UBYTE;
                }

                @Override
                public void set(int index, double value) {
                    int discrete = (int) (value * MAX_UBYTE);
                    ((byte[]) data)[index] = (byte) discrete;
                }
            };
        case SHORT:
            return new TextureDataView() {
                @Override
                public BufferData getBufferData() {
                    return BufferData.this;
                }

                @Override
                public double get(int index) {
                    int unsigned = 0xffff & ((short[]) data)[index];
                    return unsigned / MAX_USHORT;
                }

                @Override
                public void set(int index, double value) {
                    int discrete = (int) (value * MAX_USHORT);
                    ((short[]) data)[index] = (short) discrete;
                }
            };
        case INT:
            return new TextureDataView() {
                @Override
                public BufferData getBufferData() {
                    return BufferData.this;
                }

                @Override
                public double get(int index) {
                    long unsigned = 0xffffffffL & ((int[]) data)[index];
                    return unsigned / MAX_UINT;
                }

                @Override
                public void set(int index, double value) {
                    long discrete = (long) (value * MAX_UINT);
                    ((int[]) data)[index] = (int) discrete;
                }
            };
        case FLOAT:
            return new TextureDataView() {
                @Override
                public BufferData getBufferData() {
                    return BufferData.this;
                }

                @Override
                public double get(int index) {
                    return ((float[]) data)[index];
                }

                @Override
                public void set(int index, double value) {
                    ((float[]) data)[index] = (float) value;
                }
            };
        default:
            throw new UnsupportedOperationException("Unsupported data type");
        }
    }

    /**
     * <p/>
     * Return a view over the buffer data that automatically converts the signed Java
     * primitives into <code>long</code>'s. By converting them to 64-bits, all values for
     * unsigned bytes, shorts, and longs can be properly represented. The view correctly
     * casts and masks values coming into the buffer so that you can easily store unsigned
     * integers as well.
     * <p/>
     * As an example, if a byte data buffer held the value <code>(byte) -1</code>, the
     * view would report a value of <code>255</code> , which is the maximum unsigned byte
     * value and has the same bit representation as a signed -1.
     *
     * @return A view over 'unsigned' data
     *
     * @throws IllegalStateException if the buffer is a float buffer, because that cannot
     *                               become an unsigned integer
     */
    public UnsignedDataView getUnsignedView() {
        switch (type) {
        case BYTE:
            return new UnsignedDataView() {
                @Override
                public BufferData getBufferData() {
                    return BufferData.this;
                }

                @Override
                public long get(int index) {
                    return 0xffL & ((byte[]) data)[index];
                }

                @Override
                public void set(int index, long value) {
                    ((byte[]) data)[index] = (byte) value;
                }
            };
        case SHORT:
            return new UnsignedDataView() {
                @Override
                public BufferData getBufferData() {
                    return BufferData.this;
                }

                @Override
                public long get(int index) {
                    return 0xffffL & ((short[]) data)[index];
                }

                @Override
                public void set(int index, long value) {
                    ((short[]) data)[index] = (short) value;
                }
            };
        case INT:
            return new UnsignedDataView() {
                @Override
                public BufferData getBufferData() {
                    return BufferData.this;
                }

                @Override
                public long get(int index) {
                    return 0xffffffffL & ((int[]) data)[index];
                }

                @Override
                public void set(int index, long value) {
                    ((int[]) data)[index] = (int) value;
                }
            };
        case FLOAT:
            throw new IllegalStateException(
                    "UnsignedDataView only supports BYTE, SHORT, and INT types");
        default:
            throw new UnsupportedOperationException("Unsupported data type");
        }
    }

    private void setData(Object array, int arrayLength, DataType expectedType) {
        if (type != expectedType) {
            throw new IllegalStateException(
                    "BufferData's type is " + type + ", but " + expectedType +
                    " is required");
        }
        if (arrayLength != length) {
            throw new IllegalArgumentException(
                    "Incorrect array length, must be " + length + ", by is " +
                    arrayLength);
        }

        data = array;
    }
}
