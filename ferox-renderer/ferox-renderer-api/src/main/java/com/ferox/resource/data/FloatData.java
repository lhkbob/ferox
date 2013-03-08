package com.ferox.resource.data;

/**
 * FloatData is a buffer data implementation that stores 32-bit floating point values.
 * Depending on where the buffer is used, values outside the range [0, 1] may be clamped
 * by OpenGL or invalid.
 *
 * @author Michael Ludwig
 */
public class FloatData extends AbstractData<float[]>
        implements TexelData<float[]>, VertexData<float[]> {
    private final float[] data;

    /**
     * <p/>
     * Create a FloatData that will store <var>length</var> floats. It will create a new
     * array of the given length to wrap.
     *
     * @param length The length of the buffer data
     *
     * @throws NegativeArraySizeException if length is less than 0
     */
    public FloatData(int length) {
        this(new float[length]);
    }

    /**
     * <p/>
     * Create a FloatData that wraps the given float[] array. The buffer data's length
     * will be equal to the length of the array.
     *
     * @param data The array to wrap
     *
     * @throws NullPointerException if data is null
     */
    public FloatData(float[] data) {
        super(DataType.FLOAT, data.length);
        this.data = data;
    }

    /**
     * Get the float[] array that backs this FloatData instance. The returned array will
     * be non-null and have a length equal to {@link #getLength()}.
     *
     * @return The array backing this FloatData instance
     */
    @Override
    public float[] get() {
        return data;
    }

    @Override
    public double getColorComponent(int i) {
        return data[i];
    }

    @Override
    public double getCoordinate(int i) {
        return data[i];
    }

    @Override
    public void setColorComponent(int i, double value) {
        data[i] = (float) value;
    }

    @Override
    public void setCoordinate(int i, double value) {
        data[i] = (float) value;
    }
}
