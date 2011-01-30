package com.ferox.resource;

public class BufferData {
    public static enum DataType {
        FLOAT, INT, SHORT, BYTE
    }
    
    private Object data;
    private final int length;
    private final DataType type;
    
    public BufferData(float[] data) {
        if (data == null)
            throw new NullPointerException("Array cannot be null");
        this.data = data;
        length = data.length;
        type = DataType.FLOAT;
    }
    
    public BufferData(int[] data) {
        if (data == null)
            throw new NullPointerException("Array cannot be null");
        this.data = data;
        length = data.length;
        type = DataType.INT;
    }
    
    public BufferData(short[] data) {
        if (data == null)
            throw new NullPointerException("Array cannot be null");
        this.data = data;
        length = data.length;
        type = DataType.SHORT;
    }
    
    public BufferData(byte[] data) {
        if (data == null)
            throw new NullPointerException("Array cannot be null");
        this.data = data;
        length = data.length;
        type = DataType.BYTE;
    }
    
    public BufferData(DataType type, int length) {
        if (type == null)
            throw new NullPointerException("DataType cannot be null");
        if (length < 1)
            throw new IllegalArgumentException("Length must be at least 1, not: " + length);
        
        this.length = length;
        this.type = type;
        data = null;
    }
    
    public int getLength() {
        return length;
    }
    
    public DataType getDataType() {
        return type;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    public void setData(float[] data) {
        if (type != DataType.FLOAT)
            throw new IllegalStateException("Incorrect DataType, FLOAT is required but BufferData is " + type);
        if (data != null && data.length != length)
            throw new IllegalArgumentException("Incorrect array length, must be " + length + ", but is " + data.length);
        
        this.data = data;
    }
    
    public void setData(int[] data) {
        if (type != DataType.INT)
            throw new IllegalStateException("Incorrect DataType, INT is required but BufferData is " + type);
        if (data != null && data.length != length)
            throw new IllegalArgumentException("Incorrect array length, must be " + length + ", but is " + data.length);
        
        this.data = data;
    }
    
    public void setData(short[] data) {
        if (type != DataType.SHORT)
            throw new IllegalStateException("Incorrect DataType, SHORT is required but BufferData is " + type);
        if (data != null && data.length != length)
            throw new IllegalArgumentException("Incorrect array length, must be " + length + ", but is " + data.length);
        
        this.data = data;
    }
    
    public void setData(byte[] data) {
        if (type != DataType.BYTE)
            throw new IllegalStateException("Incorrect DataType, BYTE is required but BufferData is " + type);
        if (data != null && data.length != length)
            throw new IllegalArgumentException("Incorrect array length, must be " + length + ", but is " + data.length);
        
        this.data = data;
    }
}
