package com.ferox.resource;

/**
 * DataRange is a small object that tracks changes with an array or buffer that
 * is one-dimensional. It has both an offset into the data and a length; these
 * are measured in number of indices and not any other concept such as pixel or
 * vector.
 * 
 * @author Michael Ludwig
 */
public class DataRange {
    private final int offset;
    private final int length;

    /**
     * Create a DataRange with the given offset and length of data. The offset
     * cannot be less than 0 and the length must be at least 1.
     * 
     * @param offset The offset into the array or buffer of data
     * @param length The length of modified data starting at offset
     * @throws IllegalArgumentException if offset < 0 or length < 1
     */
    public DataRange(int offset, int length) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset must be at least 0, not: " + offset);
        if (length < 1)
            throw new IllegalArgumentException("Length must be at least 1, not: " + length);
        
        this.offset = offset;
        this.length = length;
    }
    
    /**
     * @return The offset into the modified array or buffer, will be at least 0
     */
    public int getOffset() {
        return offset;
    }
    
    /**
     * @return The length of modified data, will be at least 1
     */
    public int getLength() {
        return length;
    }
}
