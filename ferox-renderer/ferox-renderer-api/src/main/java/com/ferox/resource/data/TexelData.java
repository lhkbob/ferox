package com.ferox.resource.data;

/**
 * TexelData is an interface {@link BufferData} implementations can implement if they are
 * capable of containing color data for textures.
 *
 * @author Michael Ludwig
 */
public interface TexelData {
    /**
     * Get the color component value at buffer index <var>i</var>. If the underlying data
     * is already floating point, the value is returned as is. If the data is an integer
     * type, it is normalized to the range [0, 1].
     *
     * @param i The buffer index
     *
     * @return The color component value as a double
     *
     * @throws ArrayIndexOutOfBoundsException if i is out of bounds
     */
    public double getColorComponent(int i);

    /**
     * Set the color component value at the buffer index <var>i</var>. If the underlying
     * data type of the buffer is floating point, the value is kept as is barring any loss
     * of precision. If the data is an integer type, the value is assumed to be in the
     * range [0, 1] and is unnormalized to the full range of the data type.
     *
     * @param i     The buffer index
     * @param value The color component value
     *
     * @throws ArrayIndexOutOfBoundsException if i is out of bounds
     */
    public void setColorComponent(int i, double value);
}
