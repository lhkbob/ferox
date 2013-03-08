package com.ferox.resource.data;


/**
 * IntData is a buffer data implementation that stores signed int values in a int[] array.
 * Because it is signed data, its only usable as vertex data. {@link UnsignedIntData}
 * should be used for element or texel data.
 * <p/>
 * IntData can be interpreted by OpenGL as normalized floating point values. If a IntData
 * instance is normalized, an int value of {@link Integer#MIN_VALUE} is -1.0 and an int
 * value of {@link Integer#MAX_VALUE} becomes 1.0.
 *
 * @author Michael Ludwig
 */
public class IntData extends AbstractData<int[]> implements VertexData<int[]> {
    private static final double OFFSET = (double) Integer.MIN_VALUE;
    private static final double SCALE = 4294967295.0;

    private final boolean normalized;
    private final int[] data;

    /**
     * Create a new IntData instance with the given <var>length</var>. It will create a
     * new int array and will not be normalized.
     *
     * @param length The length of the int data
     *
     * @throws NegativeArraySizeException if length is negative
     */
    public IntData(int length) {
        this(length, false);
    }

    /**
     * Create a new IntData instance that will create a new int array of the given
     * <var>length</var>. The data will be treated as normalized or not depending on the
     * <var>normalized</var> parameter.
     *
     * @param length     The length of the int data
     * @param normalized True if the int data is normalized to floating point
     *
     * @throws NegativeArraySizeException if length is negative
     */
    public IntData(int length, boolean normalized) {
        this(new int[length], normalized);
    }

    /**
     * Create a new IntData instance that wraps the given int array and treats the data as
     * not normalized.
     *
     * @param data The array to wrap
     *
     * @throws NullPointerException if data is null
     */
    public IntData(int[] data) {
        this(data, false);
    }

    /**
     * Create a new IntData instance that wraps the given int array and will treat the
     * data as normalized or not depending on the <var>normalized</var> parameter.
     *
     * @param data       The data to wrap
     * @param normalized True if the int data is normalized
     *
     * @throws NullPointerException if data is null
     */
    public IntData(int[] data, boolean normalized) {
        super(DataType.INT, data.length);
        this.normalized = normalized;
        this.data = data;
    }

    /**
     * Get whether or not the int data is normalized to floating point values by OpenGL.
     * If this returns true, a int value of -128 becomes -1.0 and a int value of 127
     * becomes 1.0. Intermediate int values are similarly converted to the range (-1.0,
     * 1.0).
     *
     * @return True if it is normalized.
     */
    public boolean isNormalized() {
        return normalized;
    }

    /**
     * Get the int array backing this IntData instance.
     *
     * @return Non-null array with a length equal to {@link #getLength()}
     */
    @Override
    public int[] get() {
        return data;
    }

    @Override
    public double getCoordinate(int i) {
        int value = data[i];
        if (normalized) {
            return 2.0 * (value - OFFSET) / SCALE - 1.0;
        } else {
            return value;
        }
    }

    @Override
    public void setCoordinate(int i, double value) {
        if (normalized) {
            data[i] = (int) ((value + 1.0) * SCALE / 2.0 + OFFSET);
        } else {
            data[i] = (int) value;
        }
    }
}
