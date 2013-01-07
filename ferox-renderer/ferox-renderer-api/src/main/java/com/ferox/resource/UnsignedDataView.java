package com.ferox.resource;

public interface UnsignedDataView {
    public BufferData getBufferData();

    public long get(int index);

    public void set(int index, long value);
}
