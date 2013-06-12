package com.ferox.resource.data;

import com.ferox.resource.DataType;
import com.ferox.resource.texture.TextureFormat;

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
public class UnsignedShortData extends AbstractData<short[]>
        implements TexelData<short[]>, ElementData<short[]> {
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
    @Override
    public short[] get() {
        return data;
    }

    @Override
    public long getElementIndex(int i) {
        return DataUtil.unsignedShortToLong(data[i]);
    }

    @Override
    public void setElementIndex(int i, long value) {
        data[i] = DataUtil.longToUnsignedShort(value);
    }

    @Override
    public double getFloatComponent(int texelIndex, int component, TextureFormat format) {
        if (component < 0 || component >= format.getComponentCount()) {
            throw new IllegalArgumentException(
                    "Invalid component for format " + format + ": " + component);
        }

        switch (format) {
        // the formats listed below support the integer data type
        // none of them are packed and treat the integers as normalized floats
        case R:
        case RG:
        case RGB:
        case RGBA:
        case BGR:
        case BGRA:
            int dataIndex = texelIndex * format.getComponentCount() + component;
            return DataUtil.normalizeUnsignedShort(data[dataIndex]);
        default:
            throw new IllegalArgumentException(
                    "Component " + component + " is not an float component for " +
                    format);
        }
    }

    @Override
    public void setFloatComponent(int texelIndex, int component, TextureFormat format,
                                  double value) {
        if (component < 0 || component >= format.getComponentCount()) {
            throw new IllegalArgumentException(
                    "Invalid component for format " + format + ": " + component);
        }

        switch (format) {
        // the formats listed below support the integer data type
        // none of them are packed and treat the integers as normalized floats
        case R:
        case RG:
        case RGB:
        case RGBA:
        case BGR:
        case BGRA:
            int dataIndex = texelIndex * format.getComponentCount() + component;
            data[dataIndex] = DataUtil.unnormalizeUnsignedShort(value);
            break;
        default:
            throw new IllegalArgumentException(
                    "Component " + component + " is not an float component for " +
                    format);
        }
    }

    @Override
    public long getIntegerComponent(int texelIndex, int component, TextureFormat format) {
        if (component < 0 || component >= format.getComponentCount()) {
            throw new IllegalArgumentException(
                    "Invalid component for format " + format + ": " + component);
        }

        switch (format) {
        // the formats listed below support the integer data type
        // none of them are packed so they follow the same access pattern
        case R_UINT:
        case RG_UINT:
        case RGB_UINT:
        case RGBA_UINT:
        case BGR_UINT:
        case BGRA_UINT:
            int dataIndex = texelIndex * format.getComponentCount() + component;
            return DataUtil.unsignedShortToLong(data[dataIndex]);
        default:
            throw new IllegalArgumentException(
                    "Component " + component + " is not an integer component for " +
                    format);
        }
    }

    @Override
    public void setIntegerComponent(int texelIndex, int component, TextureFormat format,
                                    long value) {
        if (component < 0 || component >= format.getComponentCount()) {
            throw new IllegalArgumentException(
                    "Invalid component for format " + format + ": " + component);
        }

        switch (format) {
        // the formats listed below support the integer data type
        // none of them are packed so they follow the same access pattern
        case R_UINT:
        case RG_UINT:
        case RGB_UINT:
        case RGBA_UINT:
        case BGR_UINT:
        case BGRA_UINT:
            int dataIndex = texelIndex * format.getComponentCount() + component;
            data[dataIndex] = DataUtil.longToUnsignedShort(value);
            break;
        default:
            throw new IllegalArgumentException(
                    "Component " + component + " is not an integer component for " +
                    format);
        }
    }
}
