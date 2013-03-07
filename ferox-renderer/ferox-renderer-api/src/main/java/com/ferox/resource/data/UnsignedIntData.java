package com.ferox.resource.data;

/**
 * Created with IntelliJ IDEA. User: michaelludwig Date: 3/7/13 Time: 1:13 AM To change
 * this template use File | Settings | File Templates.
 */
public class UnsignedIntData extends BufferData implements TexelData, ElementData {
    private int[] data;

    public UnsignedIntData(int length) {
        super(DataType.INT, length);
        data = null;
    }

    public UnsignedIntData(int[] data) {
        super(DataType.INT, data.length);
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
