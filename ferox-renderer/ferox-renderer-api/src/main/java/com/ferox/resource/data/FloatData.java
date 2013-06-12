package com.ferox.resource.data;

import com.ferox.resource.DataType;
import com.ferox.resource.texture.TextureFormat;

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
    public double getCoordinate(int i) {
        return data[i];
    }

    @Override
    public void setCoordinate(int i, double value) {
        data[i] = (float) value;
    }

    @Override
    public double getFloatComponent(int texelIndex, int component, TextureFormat format) {
        // All TextureFormats that support FLOATS have every single component as a float component
        if (!format.getSupportedTypes().contains(DataType.FLOAT)) {
            throw new IllegalArgumentException("Component is not a float component");
        }
        if (component < 0 || component >= format.getComponentCount()) {
            throw new IllegalArgumentException(
                    "Invalid component for format " + format + ": " + component);
        }
        int dataIndex = texelIndex * format.getComponentCount() + component;
        return data[dataIndex];
    }

    @Override
    public void setFloatComponent(int texelIndex, int component, TextureFormat format,
                                  double value) {
        // All TextureFormats that support FLOATS have every single component as a float component
        if (!format.getSupportedTypes().contains(DataType.FLOAT)) {
            throw new IllegalArgumentException("Component is not a float component");
        }
        if (component < 0 || component >= format.getComponentCount()) {
            throw new IllegalArgumentException(
                    "Invalid component for format " + format + ": " + component);
        }
        int dataIndex = texelIndex * format.getComponentCount() + component;
        data[dataIndex] = (float) value;
    }

    @Override
    public long getIntegerComponent(int texelIndex, int component, TextureFormat format) {
        throw new UnsupportedOperationException(
                "FloatData cannot store integer components");
    }

    @Override
    public void setIntegerComponent(int texelIndex, int component, TextureFormat format,
                                    long value) {
        throw new UnsupportedOperationException(
                "FloatData cannot store integer components");
    }
}
