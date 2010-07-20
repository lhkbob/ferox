package com.ferox.math;

/**
 * <p>
 * Vector4f is a mutable extension to ReadOnlyVector4f. When returned as a
 * ReadOnlyVector4f is will function as if it is read-only. However, it exposes
 * a number of ways to modify its four components. Any changes to its component
 * values will then be reflected in the accessors defined in ReadOnlyVector4f.
 * </p>
 * <p>
 * In the majority of cases, encountered ReadOnlyVector4f's are likely to be
 * Vector4f's but it should not be considered safe to downcast because certain
 * functions within ReadOnlyMatrix4f or Vector3f can return ReadOnlyVector4fs
 * that use a different data source.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Vector4f extends ReadOnlyVector4f implements Cloneable {
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

    /**
     * As {@link #ortho(ReadOnlyVector4f, Vector4f)} where result is the calling
     * vector.
     * 
     * @param proj
     * @return This vector
     * @throws NullPointerException if proj is null
     */
    public Vector4f ortho(ReadOnlyVector4f proj) {
        return ortho(proj, this);
    }

    /**
     * As {@link #project(ReadOnlyVector4f, Vector4f)} where result is the
     * calling vector.
     * 
     * @param proj
     * @return This vector
     * @throws NullPointerException if proj is null
     */
    public Vector4f project(ReadOnlyVector4f proj) {
        return project(proj, this);
    }

    /**
     * As {@link #add(ReadOnlyVector4f, Vector4f)} where result is the calling
     * vector.
     * 
     * @param v
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector4f add(ReadOnlyVector4f v) {
        return add(v, this);
    }

    /**
     * As {@link #sub(ReadOnlyVector4f, Vector4f)} where result is the calling
     * vector.
     * 
     * @param v
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector4f sub(ReadOnlyVector4f v) {
        return sub(v, this);
    }

    /**
     * As {@link #scaleAdd(float, ReadOnlyVector4f, Vector4f)} where result is
     * this vector.
     * 
     * @param scalar
     * @param add
     * @return This vector
     * @throws NullPointerException if add is null
     */
    public Vector4f scaleAdd(float scalar, ReadOnlyVector4f add) {
        return scaleAdd(scalar, add, this);
    }

    /**
     * As {@link #scale(float, Vector4f)} where result is the calling vector.
     * 
     * @param scalar
     * @return This vector
     */
    public Vector4f scale(float scalar) {
        return scale(scalar, this);
    }

    /**
     * Normalize this vector in place, equivalent to
     * {@link #normalize(Vector4f)} where the result is this vector.
     * 
     * @return This vector
     * @throws ArithmeticException if this vector cannot be normalized
     */
    public Vector4f normalize() {
        return normalize(this);
    }

    @Override
    public Vector4f clone() {
        try {
            return (Vector4f) super.clone();
        } catch (CloneNotSupportedException e) {
            // shouldn't happen since Vector4f implements Cloneable
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Set the vector coordinate at index to the given value. index must be one
     * of 0 (x), 1 (y), 2 (z), or 3 (w).
     * 
     * @param index Coordinate to modify
     * @param val New value for coordinate
     * @return This vector
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Vector4f set(int index, float val) {
        switch (index) {
        case 0:
            x = val;
            break;
        case 1:
            y = val;
            break;
        case 2:
            z = val;
            break;
        case 3:
            w = val;
            break;
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }

        return this;
    }

    /**
     * Set the x, y, z, and w values of this Vector4f to the values held in v.
     * 
     * @param v Vector to be copied into this
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector4f set(ReadOnlyVector4f v) {
        return set(v.getX(), v.getY(), v.getZ(), v.getW());
    }

    /**
     * Set the x, y, z, and w values of this Vector4f to the given four
     * coordinates.
     * 
     * @param x New x coordinate
     * @param y New y coordinate
     * @param z New z coordinate
     * @param w New w coordinate
     * @return This vector
     */
    public Vector4f set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;

        return this;
    }

    /**
     * Set the x, y, z and w values of this Vector4f to the four values held
     * within the vals array, starting at offset.
     * 
     * @param vals Array to take 4 component values from
     * @param offset Index of the x coordinate
     * @return This vector
     * @throws NullPointerException if vals is null
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values
     *             starting at offset
     */
    public Vector4f set(float[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2], vals[offset + 3]);
    }
    
    /**
     * Set the new value of this vector's x coordinate. The value in <tt>x</tt>
     * will be returned by subsequent calls to {@link #getX()}
     * 
     * @param x The new x coordinate value
     */
    public void setX(float x) {
        this.x = x;
    }
    
    /**
     * Set the new value of this vector's y coordinate. The value in <tt>y</tt>
     * will be returned by subsequent calls to {@link #getY()}
     * 
     * @param y The new y coordinate value
     */
    public void setY(float y) {
        this.y = y;
    }
    
    /**
     * Set the new value of this vector's z coordinate. The value in <tt>z</tt>
     * will be returned by subsequent calls to {@link #getZ()}
     * 
     * @param z The new z coordinate value
     */
    public void setZ(float z) {
        this.z = z;
    }
    
    /**
     * Set the new value of this vector's w coordinate. The value in <tt>w</tt>
     * will be returned by subsequent calls to {@link #getW()}
     * 
     * @param w The new w coordinate value
     */
    public void setW(float w) {
        this.w = w;
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
}
