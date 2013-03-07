package com.ferox.resource.data;

/**
 * ElementData is an interface {@link BufferData} implementations can implement if they
 * are capable of containing unsigned integer data for element buffers.
 *
 * @author Michael Ludwig
 */
public interface ElementData {
    /**
     * Get the unsigned integer element index at buffer index <tt>i</tt>. Depending on the
     * underlying data type of the buffer data, the actual range of values returned may be
     * less than a full 32-bit unsigned integer. The long return type is to guarantee that
     * all unsigned integer values can be returned.
     *
     * @param i The buffer index
     *
     * @return The element index converted to a long to preserve its unsigned value
     *
     * @throws ArrayIndexOutOfBoundsException if i is out of bounds
     */
    public long getElementIndex(int i);

    /**
     * Set the unsigned element index at the buffer index <tt>i</tt>. If the underlying
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
