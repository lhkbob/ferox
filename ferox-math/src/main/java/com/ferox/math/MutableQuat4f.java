package com.ferox.math;

import java.nio.FloatBuffer;

/**
 * <p>
 * MutableQuat4f is a mutable extension to ReadOnlyQuat4f. When returned as a
 * ReadOnlyQuat4f is will function as if it is read-only. However, it exposes
 * a number of ways to modify its four components. Any changes to its component
 * values will then be reflected in the accessors defined in ReadOnlyQuat4f.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class MutableQuat4f extends ReadOnlyQuat4f {
    /**
     * As {@link #add(ReadOnlyQuat4f, Quat4f)} where result is this quaternion.
     * 
     * @param q
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public MutableQuat4f add(ReadOnlyQuat4f q) {
        return add(q, this);
    }

    /**
     * As {@link #sub(ReadOnlyQuat4f, Quat4f)} where result is this quaternion.
     * 
     * @param q
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public MutableQuat4f sub(ReadOnlyQuat4f q) {
        return sub(q, this);
    }

    /**
     * As {@link #mul(ReadOnlyQuat4f, Quat4f)} where result is this quaternion.
     * 
     * @param q
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public MutableQuat4f mul(ReadOnlyQuat4f q) {
        return mul(q, this);
    }

    /**
     * As {@link #mul(ReadOnlyVector3f, Quat4f)} where result is this
     * quaternion.
     * 
     * @param v
     * @return This quaternion
     * @throws NullPointerException if q is null 
     */
    public MutableQuat4f mul(ReadOnlyVector3f v) {
        return mul(v, this);
    }

    /**
     * As {@link #scale(float, Quat4f)} where result is this quaternion
     * 
     * @param s
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public MutableQuat4f scale(float s) {
        return scale(s, this);
    }

    /**
     * Normalize this quaternion in place, equivalent to
     * {@link #normalize(Quat4f)} where result is this quaternion.
     * 
     * @return This quaternion
     * @throws ArithmeticException if this quaternion cannot be normalized
     */
    public MutableQuat4f normalize() {
        return normalize(this);
    }

    /**
     * Invert this quaternion in place, equivalent to {@link #inverse(Quat4f)}
     * where result is this quaternion.
     * 
     * @return This quaternion
     */
    public MutableQuat4f inverse() {
        return inverse(this);
    }

    /**
     * As {@link #slerp(ReadOnlyQuat4f, float, Quat4f)} where result is this
     * quaternion.
     * 
     * @param q
     * @param t
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public MutableQuat4f slerp(ReadOnlyQuat4f q, float t) {
        return slerp(q, t, this);
    }

    /**
     * Set this quaternion to the identity quaternion, which is (0, 0, 0, 1).
     * 
     * @return This quaternion
     */
    public MutableQuat4f setIdentity() {
        return set(0f, 0f, 0f, 1f);
    }

    /**
     * Set this quaternion to be the rotation of <tt>angle</tt> radians about
     * the specified <tt>axis</tt>.
     * 
     * @param axis The axis of rotation, should not be the 0 vector
     * @param angle The angle of rotation, in radians
     * @return This quaternion
     * @throws NullPointerException if axis is null
     */
    public MutableQuat4f setAxisAngle(ReadOnlyVector3f axis, float angle) {
        float d = axis.length();
        float s = (float) Math.sin(.5f * angle) / d;
        return set(axis.getX() * s, axis.getY() * s, axis.getZ() * s, (float) Math.cos(.5f * angle));
    }
    
    /**
     * Set this quaternion to be the rotation described by the Euler angles:
     * <tt>yaw</tt>, <tt>pitch</tt> and <tt>roll</tt>. Yaw is the rotation
     * around the y-axis, pitch is the rotation around the x-axis, and roll is
     * the rotation around the z-axis. The final rotation is formed by rotating
     * first Y, then X and then Z.
     * 
     * @param yaw Rotation around the y-axis in radians
     * @param pitch Rotation around the x-axis in radians
     * @param roll Rotation around the z-axis in radians
     * @return This quaternion
     */
    public MutableQuat4f setEuler(float yaw, float pitch, float roll) {
        float cosYaw = (float) Math.cos(.5f * yaw);
        float sinYaw = (float) Math.sin(.5f * yaw);
        
        float cosPitch = (float) Math.cos(.5f * pitch);
        float sinPitch = (float) Math.sin(.5f * pitch);
        
        float cosRoll = (float) Math.cos(.5f * roll);
        float sinRoll = (float) Math.sin(.5f * roll);
        
        return set(cosRoll * sinPitch * cosYaw + sinRoll * cosPitch * sinYaw,
                   cosRoll * cosPitch * sinYaw - sinRoll * sinPitch * cosYaw,
                   sinRoll * cosPitch * cosYaw - cosRoll * sinPitch * sinYaw,
                   cosRoll * cosPitch * cosYaw + sinRoll * sinPitch * sinYaw);
    }
    
    /**
     * Set the value of this quaternion to represent the rotation of the given
     * matrix, <tt>e</tt>. It is assumed that <tt>e</tt> contains a rotation
     * matrix and does not include scale factors, or other form of 3x3 matrix.
     * 
     * @param e The rotation matrix to convert to quaternion form
     * @return This quaternion
     * @throws NullPointerException if e is null
     */
    public MutableQuat4f set(ReadOnlyMatrix3f e) {
        float trace = e.get(0, 0) + e.get(1, 1) + e.get(2, 2);
        if (trace > 0f) {
            float s = (float) Math.sqrt(trace + 1f);
            setW(.5f * s);
            s = .5f / s;
            
            setX((e.get(2, 1) - e.get(1, 2)) * s);
            setY((e.get(0, 2) - e.get(2, 0)) * s);
            setZ((e.get(1, 0) - e.get(0, 1)) * s);
        } else {
            // get the column that has the highest diagonal element
            int i = (e.get(0, 0) < e.get(1, 1) ? (e.get(1, 1) < e.get(2, 2) ? 2 : 1)
                                               : (e.get(0, 0) < e.get(2, 2) ? 2 : 0));
            int j = (i + 1) % 3;
            int k = (i + 2) % 3;
            
            float s = (float) Math.sqrt(e.get(i, i) - e.get(j, j) - e.get(k, k) + 1f);
            set(i, .5f * s);
            s = .5f / s;
            
            set(3, (e.get(k, j) - e.get(j, k)) * s);
            set(j, (e.get(j, i) + e.get(i, j)) * s);
            set(k, (e.get(k, i) + e.get(i, k)) * s);
        }
        
        return this;
    }

    /**
     * Set the new value of this quaternion's x coordinate. The value in
     * <tt>x</tt> will be returned by subsequent calls to {@link #getX()}
     * 
     * @param x The new x coordinate value
     */
    public abstract MutableQuat4f setX(float x);
    
    /**
     * Set the new value of this quaternion's y coordinate. The value in
     * <tt>y</tt> will be returned by subsequent calls to {@link #getY()}
     * 
     * @param y The new y coordinate value
     */
    public abstract MutableQuat4f setY(float y);
    
    /**
     * Set the new value of this quaternion's z coordinate. The value in
     * <tt>z</tt> will be returned by subsequent calls to {@link #getZ()}
     * 
     * @param z The new z coordinate value
     */
    public abstract MutableQuat4f setZ(float z);
    
    /**
     * Set the new value of this quaternion's w coordinate. The value in
     * <tt>w</tt> will be returned by subsequent calls to {@link #getW()}
     * 
     * @param w The new w coordinate value
     */
    public abstract MutableQuat4f setW(float w);
    
    /**
     * Set the quaternion coordinate at index to the given value. index must be
     * one of 0 (x), 1 (y), 2 (z), or 3 (w).
     * 
     * @param index Coordinate to modify
     * @param val New value for coordinate
     * @return This quaternion
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public MutableQuat4f set(int index, float val) {
        switch (index) {
        case 0:
            return setX(val);
        case 1:
            return setY(val);
        case 2:
            return setZ(val);
        case 3:
            return setW(val);
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }
    }

    /**
     * Set the x, y, z, and w values of this Quat4f to the values held in q.
     * 
     * @param q Quaternion to be copied into this
     * @return This quaternion
     * @throws NullPointerException if q is null
     */
    public MutableQuat4f set(ReadOnlyQuat4f q) {
        return set(q.getX(), q.getY(), q.getZ(), q.getW());
    }

    /**
     * Set the x, y, z, and w values of this Quat4f to the given four
     * coordinates.
     * 
     * @param x New x coordinate
     * @param y New y coordinate
     * @param z New z coordinate
     * @param w New w coordinate
     * @return This quaternion
     */
    public abstract MutableQuat4f set(float x, float y, float z, float w);

    /**
     * Set the x, y, z and w values of this Quat4f to the four values held
     * within the vals array, starting at offset.
     * 
     * @param vals Array to take 4 component values from
     * @param offset Index of the x coordinate
     * @return This quaternion
     * @throws NullPointerException if vals is null
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values
     *             starting at offset
     */
    public MutableQuat4f set(float[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2], vals[offset + 3]);
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
    public MutableQuat4f set(FloatBuffer vals, int offset) {
        return set(vals.get(offset), vals.get(offset + 1), vals.get(offset + 2), vals.get(offset + 3));
    }
}
