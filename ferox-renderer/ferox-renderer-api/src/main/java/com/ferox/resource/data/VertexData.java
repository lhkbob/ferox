package com.ferox.resource.data;

/**
 * VertexData is an interface {@link BufferData} implementations can implement if they are
 * capable of containing vector data for vertex attributes.
 *
 * @author Michael Ludwig
 */
public interface VertexData<T> extends OpenGLData<T> {
    /**
     * Get the coordinate value at buffer index <var>i</var>. If the underlying data is
     * already floating point, the value is returned as is. If the data is an integer
     * type, it is converted to a double by either coercion (i.e. 100 becomes 100.0) or by
     * normalization (i.e 127 becomes 1.0 for bytes).
     * <p/>
     * The exact conversion for integral types depends on the concrete type of the buffer.
     * It must be consistent so that a given instance will never change from normalizing
     * to coerced.
     *
     * @param i The buffer index
     *
     * @return The coordinate value as a double
     *
     * @throws ArrayIndexOutOfBoundsException if i is out of bounds
     */
    public double getCoordinate(int i);

    /**
     * Set the coordinate value at the buffer index <var>i</var>. If the underlying data
     * type of the buffer is floating point, the value is kept as, is barring any loss of
     * precision. If the data is an integer type, the value is either cast to the range of
     * the type losing any fractional part (i.e. 100.3 becomes 100), or unnormalized from
     * [-1.0, 1.0] to the full precision of the integral type.
     * <p/>
     * The exact conversion for integral types depends on the concrete type of the buffer.
     * It must be consistent so that a given instance will never change from normalizing
     * to coerced.
     *
     * @param i     The buffer index
     * @param value The coordinate value
     *
     * @throws ArrayIndexOutOfBoundsException if i is out of bounds
     */
    public void setCoordinate(int i, double value);
}
