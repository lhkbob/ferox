package com.ferox.math;

/**
 * Quat4f is the default complete implementation of a quaternion. It can be used
 * as both a mutable and read-only quaternion. It stores its data in 4 member
 * fields.
 * 
 * @author Michael Ludwig
 */
public final class Quat4f extends MutableQuat4f {
    private float x;
    private float y;
    private float z;
    private float w;

    /**
     * Create a new Quat4f initialized to the identity quaternion.
     */
    public Quat4f() {
        setIdentity();
    }

    /**
     * Create a new Quat4f that copies its values from those in <tt>q</tt>.
     * 
     * @param q The quaternion to clone
     * @throws NullPointerException if q is null
     */
    public Quat4f(ReadOnlyQuat4f q) {
        set(q);
    }

    /**
     * Create a new Quat4f that takes its initial values as (x, y, z, w).
     * 
     * @param x
     * @param y
     * @param z
     * @param w
     */
    public Quat4f(float x, float y, float z, float w) {
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
    public MutableQuat4f setX(float x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableQuat4f setY(float y) {
        this.y = y;
        return this;
    }

    @Override
    public MutableQuat4f setZ(float z) {
        this.z = z;
        return this;
    }

    @Override
    public MutableQuat4f setW(float w) {
        this.w = w;
        return this;
    }

    @Override
    public MutableQuat4f set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        
        return this;
    }
}
