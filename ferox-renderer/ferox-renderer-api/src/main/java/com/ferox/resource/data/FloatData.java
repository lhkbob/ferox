package com.ferox.resource.data;

/**
 * Created with IntelliJ IDEA. User: michaelludwig Date: 3/7/13 Time: 1:06 AM To change
 * this template use File | Settings | File Templates.
 */
public class FloatData extends BufferData implements TexelData, VertexData {
    private float[] data;

    /**
     * <p/>
     * Create a FloatData that will store <tt>length</tt> floats.
     *
     * @param length The length of the buffer data
     *
     * @throws IllegalArgumentException if length is less than 0
     */
    public FloatData(int length) {
        super(DataType.FLOAT, length);
        data = null;
    }

    /**
     * <p/>
     * Create a FloatData that wraps the given float[] array. The buffer data's length
     * will be equal to the length of the array. It will initially point to the given
     * array instance.
     *
     * @param data The initial array to wrap
     *
     * @throws NullPointerException if data is null
     */
    public FloatData(float[] data) {
        super(DataType.FLOAT, data.length);
        this.data = data;
    }

    @Override
    public double getColorComponent(int i) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
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
