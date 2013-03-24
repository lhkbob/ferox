package com.ferox.resource.data;

import com.ferox.resource.texture.TextureFormat;

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
 * <p/>
 * This is a description of the most common texel conversion behavior. Each {@link
 * TextureFormat} can treat the primitive data differently.
 *
 * @param <T> The primitive data type
 *
 * @author Michael Ludwig
 */
public interface TexelData<T> extends OpenGLData<T> {
    /**
     * Get the floating-point value of the given <var>component</var>, for the texel at
     * <var>texelIndex</var>. The texel index is not a direct index into the underlying
     * data but must be converted based on the provided <var>format</var>.
     * <p/>
     * This assumes that the requested texel component is floating-point given the texture
     * format. If the underlying data are unsigned integers, they are normalized to [0,1].
     * An exception is thrown if the component is not a floating-point component.
     *
     * @param texelIndex The index of the texel to retrieve
     * @param component  The component of the texel that is returned, starting at 0
     * @param format     The assumed texture format for this data
     *
     * @return The texel component value as a double
     *
     * @throws ArrayIndexOutOfBoundsException if texelIndex would access an invalid texel
     * @throws IllegalArgumentException       if the component is invalid or not a float
     *                                        component
     * @throws UnsupportedOperationException  if the data does not support float
     *                                        components, or if the format is compressed
     */
    public double getFloatComponent(int texelIndex, int component, TextureFormat format);

    /**
     * Set the floating-point value of the given <var>component</var>, for the texel at
     * <var>texelIndex</var>. The texel index is not a direct index into the underlying
     * data but must be converted based on the provided <var>format</var>.
     * <p/>
     * This assumes that the requested texel component is floating-point given the texture
     * format. If the underlying data are unsigned integers, they are normalized to [0,1].
     * An exception is thrown if the component is not a floating-point component.
     *
     * @param texelIndex The index of the texel to modified
     * @param component  The component of the texel that is modified, starting at 0
     * @param format     The assumed texture format for this data
     * @param value      The new value of the component
     *
     * @throws ArrayIndexOutOfBoundsException if texelIndex would access an invalid texel
     * @throws IllegalArgumentException       if the component is invalid or not a float
     *                                        component
     * @throws UnsupportedOperationException  if the data does not support float
     *                                        components, or if the format is compressed
     */
    public void setFloatComponent(int texelIndex, int component, TextureFormat format,
                                  double value);

    /**
     * Get the unsigned integer value of the given <var>component</var>, for the texel at
     * <var>texelIndex</var>. The texel index is not a direct index into the underlying
     * data but must be converted based on the provided <var>format</var>.
     * <p/>
     * This assumes that the requested component is integer type given the texture format.
     * An exception is thrown if the component is meant to be floating-point, or if the
     * underlying data is floating-point
     *
     * @param texelIndex The index of the texel to retrieve
     * @param component  The component of the texel that is modified, starting at 0
     * @param format     The assumed texture format for this data
     *
     * @return The unsigned integer texel value
     *
     * @throws ArrayIndexOutOfBoundsException if texelIndex would access an invalid texel
     * @throws IllegalArgumentException       if the component is invalid or not a integer
     *                                        component
     * @throws UnsupportedOperationException  if the data does not support integer
     *                                        components, or if the format is compressed
     */
    public long getIntegerComponent(int texelIndex, int component, TextureFormat format);

    /**
     * Set the unsigned integer value of the given <var>component</var>, for the texel at
     * <var>texelIndex</var>. The texel index is not a direct index into the underlying
     * data but must be converted based on the provided <var>format</var>.
     * <p/>
     * This assumes that the requested component is integer type given the texture format.
     * An exception is thrown if the component is meant to be floating-point, or if the
     * underlying data is floating-point
     *
     * @param texelIndex The index of the texel to modified
     * @param component  The component of the texel that is modified, starting at 0
     * @param format     The assumed texture format for this data
     * @param value      The new value of the component
     *
     * @throws ArrayIndexOutOfBoundsException if texelIndex would access an invalid texel
     * @throws IllegalArgumentException       if the component is invalid or not a integer
     *                                        component
     * @throws UnsupportedOperationException  if the data does not support integer
     *                                        components, or if the format is compressed
     */
    public void setIntegerComponent(int texelIndex, int component, TextureFormat format,
                                    long value);
}
