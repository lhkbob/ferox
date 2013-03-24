package com.ferox.resource.data;

/**
 * ShortData is a buffer data implementation that stores signed short values in a short[]
 * array. Because it is signed data, its only usable as vertex data. {@link
 * UnsignedShortData} should be used for element or texel data.
 * <p/>
 * ShortData can be interpreted by OpenGL as normalized floating point values. If a
 * ShortData instance is normalized, an short value of {@link Short#MIN_VALUE} is -1.0 and
 * an short value of {@link Short#MAX_VALUE} becomes 1.0.
 *
 * @author Michael Ludwig
 */
public class ShortData extends AbstractData<short[]> implements VertexData<short[]> {
    private static final double OFFSET = (double) Short.MIN_VALUE;
    private static final double SCALE = 65535.0;

    private final boolean normalized;
    private final short[] data;

    /**
     * Create a new ShortData instance with the given <var>length</var>. It will create a
     * new short array and will not be normalized.
     *
     * @param length The length of the short data
     *
     * @throws NegativeArraySizeException if length is negative
     */
    public ShortData(int length) {
        this(length, false);
    }

    /**
     * Create a new ShortData instance that will create a new short array of the given
     * <var>length</var>. The data will be treated as normalized or not depending on the
     * <var>normalized</var> parameter.
     *
     * @param length     The length of the short data
     * @param normalized True if the short data is normalized to floating point
     *
     * @throws NegativeArraySizeException if length is negative
     */
    public ShortData(int length, boolean normalized) {
        this(new short[length], normalized);
    }

    /**
     * Create a new ShortData instance that wraps the given short array and treats the
     * data as not normalized.
     *
     * @param data The array to wrap
     *
     * @throws NullPointerException if data is null
     */
    public ShortData(short[] data) {
        this(data, false);
    }

    /**
     * Create a new ShortData instance that wraps the given short array and will treat the
     * data as normalized or not depending on the <var>normalized</var> parameter.
     *
     * @param data       The data to wrap
     * @param normalized True if the short data is normalized
     *
     * @throws NullPointerException if data is null
     */
    public ShortData(short[] data, boolean normalized) {
        super(DataType.SHORT, data.length);
        this.normalized = normalized;
        this.data = data;
    }

    /**
     * Get whether or not the short data is normalized to floating point values by OpenGL.
     * If this returns true, a short value of -128 becomes -1.0 and a short value of 127
     * becomes 1.0. Intermediate short values are similarly converted to the range (-1.0,
     * 1.0).
     *
     * @return True if it is normalized.
     */
    public boolean isNormalized() {
        return normalized;
    }

    /**
     * Get the short array backing this ShortData instance.
     *
     * @return Non-null array with a length equal to {@link #getLength()}
     */
    @Override
    public short[] get() {
        return data;
    }

    @Override
    public double getCoordinate(int i) {
        if (normalized) {
            return DataUtil.unnormalizeShort(data[i]);
        } else {
            return data[i];
        }
    }

    @Override
    public void setCoordinate(int i, double value) {
        if (normalized) {
            data[i] = DataUtil.unnormalizeShort(value);
        } else {
            data[i] = (short) value;
        }
    }
}
