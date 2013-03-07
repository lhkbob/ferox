package com.ferox.resource.data;

/**
 * Created with IntelliJ IDEA. User: michaelludwig Date: 3/7/13 Time: 1:13 AM To change
 * this template use File | Settings | File Templates.
 */
public class UnsignedShortData extends BufferData implements TexelData, ElementData {
    private short[] data;

    public UnsignedShortData(int length) {
        super(DataType.SHORT, length);
        data = null;
    }

    public UnsignedShortData(short[] data) {
        super(DataType.SHORT, data.length);
        this.data = data;
    }

    @Override
    public long getElementIndex(int i) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setElementIndex(int i, long value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double getColorComponent(int i) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setColorComponent(int i, double value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
