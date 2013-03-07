package com.ferox.resource.data;

/**
 * UnsignedShortData is a buffer data implementation that stores data in short[] arrays.
 * Although Java treats shorts as 2's complement signed values, the short patterns in
 * these arrays will be treated by OpenGL as 1's complement unsigned values.
 * <p/>
 * When used as a element data, the range of interpreted values will be in [0, 65536].
 * When used as texel data, the data will be normalized from [0, 65536] to [0.0, 1.0]
 * similarly to how the signed short data is normalized to [-1.0, 1.0].
 *
 * @author Michael Ludwig
 */
public class UnsignedShortData extends BufferData implements TexelData, ElementData {
    private static final double SCALE = 65535.0;

    private short[] data;

    /**
     * Create a new UnsignedShortData instance with the given <var>length</var>. It will
     * create a new short array.
     *
     * @param length The length of the short data
     *
     * @throws NegativeArraySizeException if length is negative
     */
    public UnsignedShortData(int length) {
        this(new short[length]);
    }

    /**
     * Create a new UnsignedShortData instance that wraps the given short array and treats
     * the data.
     *
     * @param data The array to wrap
     *
     * @throws NullPointerException if data is null
     */
    public UnsignedShortData(short[] data) {
        super(DataType.SHORT, data.length);
        this.data = data;
    }

    /**
     * Get the short array backing this UnsignedShortData instance.
     *
     * @return Non-null array with a length equal to {@link #getLength()}
     */
    public short[] getArray() {
        return data;
    }

    @Override
    public long getElementIndex(int i) {
        return (0xffff & data[i]);
    }

    @Override
    public void setElementIndex(int i, long value) {
        data[i] = (short) value;
    }

    @Override
    public double getColorComponent(int i) {
        return (0xffff & data[i]) / SCALE;
    }

    @Override
    public void setColorComponent(int i, double value) {
        data[i] = (short) (value * SCALE);
    }
}
