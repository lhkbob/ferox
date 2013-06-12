package com.ferox.resource.data;

import com.ferox.resource.DataType;
import com.ferox.resource.texture.TextureFormat;

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
public class UnsignedIntData extends AbstractData<int[]>
        implements TexelData<int[]>, ElementData<int[]> {
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
    @Override
    public int[] get() {
        return data;
    }

    @Override
    public long getElementIndex(int i) {
        return DataUtil.unsignedIntToLong(data[i]);
    }

    @Override
    public void setElementIndex(int i, long value) {
        data[i] = DataUtil.longToUnsignedInt(value);
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
        case DEPTH:
            int dataIndex = texelIndex * format.getComponentCount() + component;
            return DataUtil.normalizeUnsignedInt(data[dataIndex]);
        // for the packed floats, we know the component argument is [0, 1, 2]
        case RGB_PACKED_FLOAT:
            if (component == 0) {
                return DataUtil.getPackedFloatR(data[texelIndex]);
            } else if (component == 1) {
                return DataUtil.getPackedFloatG(data[texelIndex]);
            } else {
                return DataUtil.getPackedFloatB(data[texelIndex]);
            }
            // the component index is the same as the byte word to access
        case ARGB_PACKED_INT:
            byte word = DataUtil.getWord(data[texelIndex], component);
            return DataUtil.normalizeUnsignedByte(word);
        // only the depth component is a float value
        case DEPTH_STENCIL:
            if (component != 0) {
                throw new IllegalArgumentException(
                        "Component 0 (DEPTH) is the only float component in DEPTH_STENCIL");
            }
            return DataUtil.getDepth(data[texelIndex]);
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
        case DEPTH:
            int dataIndex = texelIndex * format.getComponentCount() + component;
            data[dataIndex] = DataUtil.unnormalizeUnsignedInt(value);
            break;
        // for the packed floats, we know the component argument is [0, 1, 2]
        case RGB_PACKED_FLOAT:
            if (component == 0) {
                data[texelIndex] = DataUtil.setPackedFloatR(data[texelIndex], value);
            } else if (component == 1) {
                data[texelIndex] = DataUtil.setPackedFloatG(data[texelIndex], value);
            } else {
                data[texelIndex] = DataUtil.setPackedFloatB(data[texelIndex], value);
            }
            break;
        // the component index is the same as the byte word to access
        case ARGB_PACKED_INT:
            byte word = DataUtil.unnormalizeUnsignedByte(value);
            data[texelIndex] = DataUtil.setWord(data[texelIndex], component, word);
            break;
        // only the depth component is a float value
        case DEPTH_STENCIL:
            if (component != 0) {
                throw new IllegalArgumentException(
                        "Component 0 (DEPTH) is the only float component in DEPTH_STENCIL");
            }
            data[texelIndex] = DataUtil.setDepth(data[texelIndex], value);
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
            return DataUtil.unsignedIntToLong(data[dataIndex]);
        case DEPTH_STENCIL:
            if (component != 1) {
                throw new IllegalArgumentException(
                        "Component 1 (STENCIL) is the only integer component in DEPTH_STENCIL");
            }
            return DataUtil.getStencil(data[texelIndex]);
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
            data[dataIndex] = DataUtil.longToUnsignedInt(value);
            break;
        case DEPTH_STENCIL:
            if (component != 1) {
                throw new IllegalArgumentException(
                        "Component 1 (STENCIL) is the only integer component in DEPTH_STENCIL");
            }
            data[texelIndex] = DataUtil.setStencil(data[texelIndex], (byte) value);
            break;
        default:
            throw new IllegalArgumentException(
                    "Component " + component + " is not an integer component for " +
                    format);
        }
    }
}
