package com.ferox.resource.data;

/**
 * UnsignedIntData is a buffer data implementation that stores data in int[] arrays.
 * Although Java treats ints as 2's complement signed values, the int patterns in these
 * arrays will be treated by OpenGL as 1's complement unsigned values.
 * <p/>
 * When used as a element data, the range of interpreted values will be in [0,
 * 4294967296]. When used as texel data, the data will be normalized from [0, 4294967296]
 * to [0.0, 1.0] similarly to how the signed int data is normalized to [-1.0, 1.0].
 *
 * @author Michael Ludwig
 */
public class UnsignedIntData extends BufferData implements TexelData, ElementData {
    private static final double SCALE = 4294967295.0;

    private int[] data;

    /**
     * Create a new UnsignedIntData instance with the given <var>length</var>. It will
     * create a new int array.
     *
     * @param length The length of the int data
     *
     * @throws NegativeArraySizeException if length is negative
     */
    public UnsignedIntData(int length) {
        this(new int[length]);
    }

    /**
     * Create a new UnsignedIntData instance that wraps the given int array and treats the
     * data.
     *
     * @param data The array to wrap
     *
     * @throws NullPointerException if data is null
     */
    public UnsignedIntData(int[] data) {
        super(DataType.INT, data.length);
        this.data = data;
    }

    /**
     * Get the int array backing this UnsignedIntData instance.
     *
     * @return Non-null array with a length equal to {@link #getLength()}
     */
    public int[] getArray() {
        return data;
    }

    @Override
    public long getElementIndex(int i) {
        return (0xffffffffL & (long) data[i]);
    }

    @Override
    public void setElementIndex(int i, long value) {
        data[i] = (int) value;
    }

    @Override
    public double getColorComponent(int i) {
        return (0xffffffffL & (long) data[i]) / SCALE;
    }

    @Override
    public void setColorComponent(int i, double value) {
        long discrete = (long) (value * SCALE);
        data[i] = (int) discrete;
    }
}
