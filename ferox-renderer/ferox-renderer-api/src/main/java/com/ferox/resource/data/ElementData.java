package com.ferox.resource.data;

/**
 * ElementData is a sub-interface of {@link OpenGLData} that declares the underlying
 * primitive data is compatible with the data types OpenGL expects for vertex attribute
 * data.
 * <p/>
 * OpenGL requires element data to be unsigned integers of various precisions (e.g. bytes,
 * shorts, ints, etc).
 *
 * @param <T> The primitive data type
 *
 * @author Michael Ludwig
 */
public interface ElementData<T> extends OpenGLData<T> {
    /**
     * Get the unsigned integer element index at buffer index <var>i</var>. Depending on
     * the underlying data type of the buffer data, the actual range of values returned
     * may be less than a full 32-bit unsigned integer. The long return type is to
     * guarantee that all unsigned integer values can be returned.
     *
     * @param i The buffer index
     *
     * @return The element index converted to a long to preserve its unsigned value
     *
     * @throws ArrayIndexOutOfBoundsException if i is out of bounds
     */
    public long getElementIndex(int i);

    /**
     * Set the unsigned element index at the buffer index <var>i</var>. If the underlying
     * data type of the buffer has less precision than 32 bits, the value will be
     * truncated to fit.
     *
     * @param i     The buffer index
     * @param value The new element index value
     *
     * @throws ArrayIndexOutOfBoundsException if i is out of bounds
     */
    public void setElementIndex(int i, long value);
}
