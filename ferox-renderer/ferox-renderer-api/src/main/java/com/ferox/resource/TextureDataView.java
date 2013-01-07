package com.ferox.resource;

public interface TextureDataView {
    public BufferData getBufferData();

    public double get(int index);

    public void set(int index, double value);
}
