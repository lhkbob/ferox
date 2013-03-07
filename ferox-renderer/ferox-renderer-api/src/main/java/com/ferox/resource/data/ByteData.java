package com.ferox.resource.data;

/**
 * Created with IntelliJ IDEA. User: michaelludwig Date: 3/7/13 Time: 1:12 AM To change
 * this template use File | Settings | File Templates.
 */
public class ByteData extends BufferData implements VertexData {
    private final boolean normalized;
    private byte[] data;

    public ByteData(int length) {
        this(length, false);
    }

    public ByteData(int length, boolean normalized) {
        super(DataType.BYTE, length);
        this.normalized = normalized;
    }

    public ByteData(byte[] data) {
        this(data, false);
    }

    public ByteData(byte[] data, boolean normalized) {
        super(DataType.BYTE, data.length);
        this.normalized = normalized;
        this.data = data;
    }

    @Override
    public double getCoordinate(int i) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setColorComponent(int i, double value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
