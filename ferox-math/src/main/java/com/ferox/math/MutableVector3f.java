package com.ferox.math;

import java.nio.FloatBuffer;

/**
 * <p>
 * MutableVector3f is a mutable extension to ReadOnlyVector3f. When returned as
 * a ReadOnlyVector3f is will function as if it is read-only. However, it
 * exposes a number of ways to modify its three components. Any changes to its
 * component values will then be reflected in the accessors defined in
 * ReadOnlyVector3f.
 * </p>
 * <p>
 * In the majority of cases, encountered ReadOnlyVector3f's are likely to be
 * MutableVector3f's but it should not be considered safe to downcast because
 * certain functions within ReadOnlyMatrix3f or Vector4f can return
 * ReadOnlyVector3fs that use a different data source.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class MutableVector3f extends ReadOnlyVector3f {
    /**
     * As {@link #ortho(ReadOnlyVector3f, Vector3f)} where result is this vector
     * 
     * @param proj
     * @return This vector
     * @throws NullPointerException if proj is null
     */
    public MutableVector3f ortho(ReadOnlyVector3f proj) {
        return ortho(proj, this);
    }

    /**
     * As {@link #project(ReadOnlyVector3f, Vector3f)} where result is this
     * vector
     * 
     * @param proj
     * @return This vector
     * @throws NullPointerException if proj is null
     */
    public MutableVector3f project(ReadOnlyVector3f proj) {
        return project(proj, this);
    }

    /**
     * As {@link #cross(ReadOnlyVector3f, Vector3f)} where result is this vector
     * 
     * @param v
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public MutableVector3f cross(ReadOnlyVector3f v) {
        return cross(v, this);
    }

    /**
     * As {@link #add(ReadOnlyVector3f, Vector3f)} where result is this vector
     * 
     * @param v
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public MutableVector3f add(ReadOnlyVector3f v) {
        return add(v, this);
    }

    /**
     * As {@link #sub(Vector3f)} where result is this vector
     * 
     * @param v
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public MutableVector3f sub(ReadOnlyVector3f v) {
        return sub(v, this);
    }

    /**
     * As {@link #scaleAdd(float, ReadOnlyVector3f, Vector3f)} where result is
     * this vector.
     * 
     * @param scalar
     * @param add
     * @return This vector
     * @throws NullPointerException if add is null
     */
    public MutableVector3f scaleAdd(float scalar, ReadOnlyVector3f add) {
        return scaleAdd(scalar, add, this);
    }

    /**
     * As {@link #scale(float)} where result is this vector
     * 
     * @param scalar
     * @return This vector
     */
    public MutableVector3f scale(float scalar) {
        return scale(scalar, this);
    }

    /**
     * Normalize this vector in place, equivalent to
     * {@link #normalize(Vector3f)} where result is this vector.
     * 
     * @return This vector
     * @throws ArithmeticException if the vector cannot be normalized
     */
    public MutableVector3f normalize() {
        return normalize(this);
    }

    /**
     * Set the vector coordinate at index to the given value. index must be one
     * of 0 (x), 1 (y), or 2 (z).
     * 
     * @param index Coordinate to modify
     * @param val New value for coordinate
     * @return This vector
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public MutableVector3f set(int index, float val) {
        switch (index) {
        case 0:
            return setX(val);
        case 1:
            return setY(val);
        case 2:
            return setZ(val);
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }
    }

    /**
     * Set the x, y, and z values of this Vector3f to the values held in v.
     * 
     * @param v Vector to be copied into this
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public MutableVector3f set(ReadOnlyVector3f v) {
        return set(v.getX(), v.getY(), v.getZ());
    }

    /**
     * Set the x, y, and z values of this Vector3f to the given three
     * coordinates.
     * 
     * @param x New x coordinate
     * @param y New y coordinate
     * @param z New z coordinate
     * @return This vector
     */
    public abstract MutableVector3f set(float x, float y, float z);

    /**
     * Set the x, y, and z values of this Vector3f to the three values held
     * within the vals array, starting at offset.
     * 
     * @param vals Array to take 3 component values from
     * @param offset Index of the x coordinate
     * @return This vector
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have three values
     *             starting at offset
     * @throws NullPointerException if vals is null
     */
    public MutableVector3f set(float[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2]);
    }
    
    /**
     * As {@link #set(float[], int)} but the values are taken from the
     * FloatBuffer
     * 
     * @param vals The float value source
     * @param offset The index into vals for the x coordinate
     * @return This vector
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values
     *             starting at offset
     */
    public MutableVector3f set(FloatBuffer vals, int offset) {
        return set(vals.get(offset), vals.get(offset + 1), vals.get(offset + 2));
    }

    /**
     * Set the new value of this vector's x coordinate. The value in <tt>x</tt>
     * will be returned by subsequent calls to {@link #getX()}
     * 
     * @param x The new x coordinate value
     */
    public abstract MutableVector3f setX(float x);
    
    /**
     * Set the new value of this vector's y coordinate. The value in <tt>y</tt>
     * will be returned by subsequent calls to {@link #getY()}
     * 
     * @param y The new y coordinate value
     */
    public abstract MutableVector3f setY(float y);
    
    /**
     * Set the new value of this vector's z coordinate. The value in <tt>z</tt>
     * will be returned by subsequent calls to {@link #getZ()}
     * 
     * @param z The new z coordinate value
     */
    public abstract MutableVector3f setZ(float z);
}
