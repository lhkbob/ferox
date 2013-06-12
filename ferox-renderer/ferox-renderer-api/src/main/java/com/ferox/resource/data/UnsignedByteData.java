package com.ferox.resource.data;

import com.ferox.resource.DataType;
import com.ferox.resource.texture.TextureFormat;

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
public class UnsignedByteData extends AbstractData<byte[]>
        implements TexelData<byte[]>, ElementData<byte[]> {
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
    @Override
    public byte[] get() {
        return data;
    }

    @Override
    public long getElementIndex(int i) {
        return DataUtil.unsignedByteToLong(data[i]);
    }

    @Override
    public void setElementIndex(int i, long value) {
        data[i] = DataUtil.longToUnsignedByte(value);
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
        case ARGB_BYTE:
            int dataIndex = texelIndex * format.getComponentCount() + component;
            return DataUtil.normalizeUnsignedByte(data[dataIndex]);
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
        case ARGB_BYTE:
            int dataIndex = texelIndex * format.getComponentCount() + component;
            data[dataIndex] = DataUtil.unnormalizeUnsignedByte(value);
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
            return DataUtil.unsignedByteToLong(data[dataIndex]);
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
            data[dataIndex] = DataUtil.longToUnsignedByte(value);
            break;
        default:
            throw new IllegalArgumentException(
                    "Component " + component + " is not an integer component for " +
                    format);
        }
    }
}
