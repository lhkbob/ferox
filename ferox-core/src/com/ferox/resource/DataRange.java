package com.ferox.resource;

public class DataRange {
    private final int offset;
    private final int length;
    
    public DataRange(int offset, int length) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset must be at least 0, not: " + offset);
        if (length < 1)
            throw new IllegalArgumentException("Length must be at least 1, not: " + length);
        
        this.offset = offset;
        this.length = length;
    }
    
    public DataRange merge(DataRange d) {
        if (d == null)
            return this;
        int offset = Math.min(this.offset, d.offset);
        int maxElement = Math.max(this.offset + this.length, d.offset + d.length);
        return new DataRange(offset, maxElement - offset);
    }
    
    public int getOffset() {
        return offset;
    }
    
    public int getLength() {
        return length;
    }
}
