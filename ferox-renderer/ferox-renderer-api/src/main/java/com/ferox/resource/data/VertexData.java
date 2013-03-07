package com.ferox.resource.data;

/**
 * VertexData is an interface {@link BufferData} implementations can implement if they are
 * capable of containing vector data for vertex attributes.
 *
 * @author Michael Ludwig
 */
public interface VertexData {
    /**
     * Get the coordinate value at buffer index <tt>i</tt>. If the underlying data is
     * already floating point, the value is returned as is. If the data is an integer
     * type, it is coerced into a double (i.e. 100 becomes 100.0).
     *
     * @param i The buffer index
     *
     * @return The coordinate value as a double
     *
     * @throws ArrayIndexOutOfBoundsException if i is out of bounds
     */
    public double getCoordinate(int i);

    /**
     * Set the coordinate value at the buffer index <tt>i</tt>. If the underlying data
     * type of the buffer is floating point, the value is kept as is barring any loss of
     * precision. If the data is an integer type, the value is cast to the range of the
     * type losing any fractional part (i.e. 100.3 becomes 100).
     *
     * @param i     The buffer index
     * @param value The coordinate value
     *
     * @throws ArrayIndexOutOfBoundsException if i is out of bounds
     */
    public void setColorComponent(int i, double value);
}
