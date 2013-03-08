package com.ferox.resource.data;

/**
 * TexelData is a sub-interface of {@link OpenGLData} that declares the underlying
 * primitive data is compatible with the data types OpenGL expects for texture color data.
 * OpenGL supports many different data types for textures but the common element is there
 * is some mechanism for converting values to the floating-point range [0, 1].
 * <p/>
 * Floating-point primitives are taken as-is or clamped, depending on if the hardware
 * supports arbitrary float values. Integer types are interpreted as unsigned values and
 * normalized by dividing them by the maximum representable unsigned number based on the
 * primitive's byte size.
 *
 * @param <T> The primitive data type
 *
 * @author Michael Ludwig
 */
public interface TexelData<T> extends OpenGLData<T> {
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
