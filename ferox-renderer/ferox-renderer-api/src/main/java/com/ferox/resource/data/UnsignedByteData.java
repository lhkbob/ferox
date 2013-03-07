package com.ferox.resource.data;

/**
 * UnsignedByteData is a buffer data implementation that stores data in byte[] arrays.
 * Although Java treats bytes as 2's complement signed values, the byte patterns in these
 * arrays will be treated by OpenGL as 1's complement unsigned values.
 * <p/>
 * When used as a element data, the range of interpreted values will be in [0, 255]. When
 * used as texel data, the data will be normalized from [0, 255] to [0.0, 1.0] similarly
 * to how the signed byte data is normalized to [-1.0, 1.0].
 *
 * @author Michael Ludwig
 */
public class UnsignedByteData extends BufferData implements TexelData, ElementData {
    private byte[] data;

    /**
     * Create a new UnsignedByteData instance with the given <var>length</var>. It will
     * create a new byte array.
     *
     * @param length The length of the byte data
     *
     * @throws NegativeArraySizeException if length is negative
     */
    public UnsignedByteData(int length) {
        this(new byte[length]);
    }

    /**
     * Create a new UnsignedByteData instance that wraps the given byte array and treats
     * the data.
     *
     * @param data The array to wrap
     *
     * @throws NullPointerException if data is null
     */
    public UnsignedByteData(byte[] data) {
        super(DataType.BYTE, data.length);
        this.data = data;
    }

    /**
     * Get the byte array backing this UnsignedByteData instance.
     *
     * @return Non-null array with a length equal to {@link #getLength()}
     */
    public byte[] getArray() {
        return data;
    }

    @Override
    public long getElementIndex(int i) {
        return (0xff & data[i]);
    }

    @Override
    public void setElementIndex(int i, long value) {
        data[i] = (byte) value;
    }

    @Override
    public double getColorComponent(int i) {
        return (0xff & data[i]) / 255.0;
    }

    @Override
    public void setColorComponent(int i, double value) {
        data[i] = (byte) (value * 255);
    }
}
