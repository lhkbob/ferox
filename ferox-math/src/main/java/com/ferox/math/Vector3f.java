package com.ferox.math;

/**
 * Vector3f is the default, complete implementation of a 3 dimensional vector.
 * It can be both mutable and read-only depending on how it is exposed. Its
 * component values are stored in three fields.
 * 
 * @author Michael Ludwig
 */
public final class Vector3f extends MutableVector3f implements Cloneable {
    private float x;
    private float y;
    private float z;

    /**
     * Create a new Vector3f with values <0, 0, 0>.
     */
    public Vector3f() {
        this(0f, 0f, 0f);
    }

    /**
     * Create a new Vector3f that copies its x, y, z values from v.
     * 
     * @param v Vector to be copied
     * @throws NullPointerException if v is null
     */
    public Vector3f(ReadOnlyVector3f v) {
        this(v.getX(), v.getY(), v.getZ());
    }

    /**
     * Create a Vector3f with the initial values for x, y, and z.
     * 
     * @param x Initial x value
     * @param y Initial y value
     * @param z Initial z value
     */
    public Vector3f(float x, float y, float z) {
        set(x, y, z);
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
    public MutableVector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        
        return this;
    }

    @Override
    public MutableVector3f setX(float x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableVector3f setY(float y) {
        this.y = y;
        return this;
    }

    @Override
    public MutableVector3f setZ(float z) {
        this.z = z;
        return this;
    }
}
