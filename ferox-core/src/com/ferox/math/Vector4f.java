package com.ferox.math;

/**
 * Vector4f is the default, complete implementation of a 4 dimensional vector.
 * It can be both mutable and read-only depending on how it is exposed. Its
 * values are stored in four fields.
 * 
 * @author Michael Ludwig
 */
public final class Vector4f extends MutableVector4f {
    private float x;
    private float y;
    private float z;
    private float w;

    /**
     * Create a new Vector4f with values <0, 0, 0, 0>.
     */
    public Vector4f() {
        this(0f, 0f, 0f, 0f);
    }

    /**
     * Create a new Vector4f that copies its x, y, z, w values from v.
     * 
     * @param v Vector to be copied
     * @throws NullPointerException if v is null
     */
    public Vector4f(ReadOnlyVector4f v) {
        this(v.getX(), v.getY(), v.getZ(), v.getW());
    }

    /**
     * Create a Vector4f with the initial values for x, y, z, and w.
     * 
     * @param x Initial x value
     * @param y Initial y value
     * @param z Initial z value
     * @param w Initial w value
     */
    public Vector4f(float x, float y, float z, float w) {
        set(x, y, z, w);
    }

    @Override
    public float getW() {
        return w;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public float getZ() {
        return z;
    }

    @Override
    public MutableVector4f set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        
        return this;
    }

    @Override
    public MutableVector4f setX(float x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableVector4f setY(float y) {
        this.y = y;
        return this;
    }

    @Override
    public MutableVector4f setZ(float z) {
        this.z = z;
        return this;
    }

    @Override
    public MutableVector4f setW(float w) {
        this.w = w;
        return this;
    }
}
