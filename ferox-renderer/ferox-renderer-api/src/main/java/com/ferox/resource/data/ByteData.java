package com.ferox.resource.data;

/**
 * ByteData is a buffer data implementation that stores signed byte values in a byte[]
 * array. Because it is signed data, its only usable as vertex data. {@link
 * UnsignedByteData} should be used for element or texel data.
 * <p/>
 * ByteData can be interpreted by OpenGL as normalized floating point values. If a
 * ByteData instance is normalized, a byte value of -128 is -1.0 and a byte value of 127
 * becomes 1.0.
 *
 * @author Michael Ludwig
 */
public class ByteData extends BufferData implements VertexData {
    private final boolean normalized;
    private final byte[] data;

    /**
     * Create a new ByteData instance with the given <var>length</var>. It will create a
     * new byte array and will not be normalized.
     *
     * @param length The length of the byte data
     *
     * @throws NegativeArraySizeException if length is negative
     */
    public ByteData(int length) {
        this(length, false);
    }

    /**
     * Create a new ByteData instance that will create a new byte array of the given
     * <var>length</var>. The data will be treated as normalized or not depending on the
     * <var>normalized</var> parameter.
     *
     * @param length     The length of the byte data
     * @param normalized True if the byte data is normalized to floating point
     *
     * @throws NegativeArraySizeException if length is negative
     */
    public ByteData(int length, boolean normalized) {
        this(new byte[length], normalized);
    }

    /**
     * Create a new ByteData instance that wraps the given byte array and treats the data
     * as not normalized.
     *
     * @param data The array to wrap
     *
     * @throws NullPointerException if data is null
     */
    public ByteData(byte[] data) {
        this(data, false);
    }

    /**
     * Create a new ByteData instance that wraps the given byte array and will treat the
     * data as normalized or not depending on the <var>normalized</var> parameter.
     *
     * @param data       The data to wrap
     * @param normalized True if the byte data is normalized
     *
     * @throws NullPointerException if data is null
     */
    public ByteData(byte[] data, boolean normalized) {
        super(DataType.BYTE, data.length);
        this.normalized = normalized;
        this.data = data;
    }

    /**
     * Get whether or not the byte data is normalized to floating point values by OpenGL.
     * If this returns true, a byte value of -128 becomes -1.0 and a byte value of 127
     * becomes 1.0. Intermediate byte values are similarly converted to the range (-1.0,
     * 1.0).
     *
     * @return True if it is normalized.
     */
    public boolean isNormalized() {
        return normalized;
    }

    /**
     * Get the byte array backing this ByteData instance.
     *
     * @return Non-null array with a length equal to {@link #getLength()}
     */
    public byte[] getArray() {
        return data;
    }

    @Override
    public double getCoordinate(int i) {
        int value = data[i];
        if (normalized) {
            return 2.0 * (value + 128.0) / 255.0 - 1.0;
        } else {
            return value;
        }
    }

    @Override
    public void setCoordinate(int i, double value) {
        if (normalized) {
            data[i] = (byte) ((value + 1.0) * 255.0 / 2.0 - 128.0);
        } else {
            data[i] = (byte) value;
        }
    }
}
