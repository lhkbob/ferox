package com.ferox.math;

import java.nio.FloatBuffer;

/**
 * <p>
 * ReadOnlyQuat4f provides the foundation class for the implementation of a
 * 4-component quaternion. It is read-only in the sense that all operations
 * expose a <tt>result</tt> parameter that stores the computation. The calling
 * vector is not modified unless it happens to be the result. Additionally, this
 * class only exposes accessors to its data and no mutators. The class
 * {@link Quat4f} provides a standard implementation of ReadOnlyQuat4f that
 * exposes mutators for the 4 values of the matrix.
 * </p>
 * <p>
 * More information on the math and theory behind quaternions can be found <a
 * href="http://en.wikipedia.org/wiki/Quaternion">here</a>.
 * </p>
 * <p>
 * In all mathematical functions that compute a new vector, there is a method
 * parameter, often named result, that will hold the computed value. This result
 * vector is also returned so that complex mathematical expressions can be
 * chained. It is always safe to use the calling matrix as the result matrix
 * (assuming the calling matrix is of the correct type).
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class ReadOnlyQuat4f {
    /**
     * @return The 1st coordinate of the quaternion
     */
    public abstract float getX();
    
    /**
     * @return The 2nd coordinate of the quaternion
     */
    public abstract float getY();
    
    /**
     * @return The 3rd coordinate of the quaternion
     */
    public abstract float getW();
    
    /**
     * @return The 4th coordinate of the quaternion
     */
    public abstract float getZ();

    /**
     * Add the quaternion, <tt>q</tt> to this quaternion component-wise and
     * store the computation in <tt>result</tt>. If result is null, a new Quat4f
     * is created and returned instead.
     * 
     * @param q The quaternion to add
     * @param result The Quat4f that holds the addition, or null
     * @return result, or a new Quat4f if null
     * @throws NullPointerException if q is null
     */
    public MutableQuat4f add(ReadOnlyQuat4f q, MutableQuat4f result) {
        return add(q.getX(), q.getY(), q.getZ(), q.getW(), result);
    }

    /**
     * As {@link #add(ReadOnlyQuat4f, Quat4f)} if (x, y, z, w) represented an
     * implicit ReadOnlyQuat4f.
     * 
     * @param x
     * @param y
     * @param z
     * @param w
     * @param result 
     * @return result, or a new Quat4f if null
     */
    public MutableQuat4f add(float x, float y, float z, float w, MutableQuat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(getX() + x, getY() + y, getZ() + z, getW() + w);
    }

    /**
     * Subtract the quaternion, <tt>q</tt> from this quaternion component-wise
     * and store the computation in <tt>result</tt>. If result is null, a new
     * Quat4f is created and returned instead.
     * 
     * @param q The quaternion to subtract
     * @param result The Quat4f that holds the subtraction, or null
     * @return result, or a new Quat4f if null
     * @throws NullPointerException if q is null
     */
    public MutableQuat4f sub(ReadOnlyQuat4f q, MutableQuat4f result) {
        return sub(q.getX(), q.getY(), q.getZ(), q.getW(), result);
    }

    /**
     * As {@link #sub(ReadOnlyQuat4f, Quat4f)} if (x, y, z, w) represented an
     * implicit ReadOnlyQuat4f.
     * 
     * @param x
     * @param y
     * @param z
     * @param w
     * @param result
     * @return result, or a new Quat4f if null
     */
    public MutableQuat4f sub(float x, float y, float z, float w, MutableQuat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(getX() - x, getY() - y, getZ() - z, getW() - w);
    }

    /**
     * Multiply this quaternion by the quaternion, <tt>q</tt>. This is similar
     * to matrix multiplication in that it is not commutative and can be thought
     * of as concatenation of transforms. The multiplication is stored in
     * <tt>result</tt>, or a new Quat4f if result is null.
     * 
     * @param q The quaternion on the left side of the multiplication
     * @param result The result of the multiplication or null
     * @return result, or a new Quat4f if null
     * @throws NullPointerException if q is null
     */
    public MutableQuat4f mul(ReadOnlyQuat4f q, MutableQuat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(getW() * q.getX() + getX() * q.getW() + getY() * q.getZ() - getZ() * q.getY(),
                          getW() * q.getY() + getY() * q.getW() + getZ() * q.getX() - getX() * q.getZ(),
                          getW() * q.getZ() + getZ() * q.getW() + getX() * q.getY() - getY() * q.getX(),
                          getW() * q.getW() - getX() * q.getX() - getY() * q.getY() - getZ() * q.getZ());
    }

    /**
     * Multiply this quaternion by the quaternion implicitly formed by
     * <tt>v</tt>, where v holds the first 3 coordinates of the quaternion and
     * its fourth is considered to be 0. The multiplication is stored in
     * <tt>result</tt>, or a new Quat4f if result is null.
     * 
     * @param v The first three coordinates of the implicit quaternion
     * @param result The result of the multiplication or null
     * @return result, or a new Quat4f if null
     * @throws NullPointerException if v is null
     */
    public MutableQuat4f mul(ReadOnlyVector3f v, MutableQuat4f result) {
        if (result == null)
            result = new Quat4f();
        
        return result.set(getW() * v.getX() + getY() * v.getZ() - getZ() * v.getY(),
                          getW() * v.getY() + getZ() * v.getX() - getX() * v.getZ(),
                          getW() * v.getZ() + getX() * v.getY() - getY() * v.getX(),
                        -(getX() * v.getX() + getY() * v.getY() + getZ() * v.getZ()));
    }

    /**
     * Transform the given vector, <tt>v</tt> by this quaternion and store the
     * computation in <tt>result</tt>, or a new Vector3f if result is null.
     * 
     * @param v The vector to rotate or transform
     * @param result The result holding the rotation, or null
     * @return result, or a new Vector3f if null
     * @throws NullPointerException if v is null
     */
    public MutableVector3f rotate(ReadOnlyVector3f v, MutableVector3f result) {
        // FIXME: generates garbage
        MutableQuat4f q = mul(v, null).mul(inverse(null));
        if (result == null)
            return new Vector3f(q.getX(), q.getY(), q.getZ());
        else
            return result.set(q.getX(), q.getY(), q.getZ());
    }

    /**
     * Scale the components of this quaternion by <tt>s</tt> and store them in
     * <tt>result</tt>. If result is null, a new Quat4f is created and returned
     * instead.
     * 
     * @param s The scale factor
     * @param result The Quat4f containing the scaled results, or null
     * @return result, or a new Quat4f if null
     */
    public MutableQuat4f scale(float s, MutableQuat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(s * getX(), s * getY(), s * getZ(), s * getW());
    }

    /**
     * Normalize this quaternion and place the normalized quaternion in
     * <tt>result</tt>. If result is null, then create a new Quat4f and return
     * that instead.
     * 
     * @param result The Quat4f to contain the normalized result
     * @return result, or a new Quat4f if null
     * @throws ArithmeticException if this quaternion cannot be normalized
     */
    public MutableQuat4f normalize(MutableQuat4f result) {
        float d = length();
        if (d == 0f)
            throw new ArithmeticException("Cannot normalize quaternion with 0 length");
        return scale(1f / d, result);
    }

    /**
     * @return The length of this quaternion (identical to a 4-vector with the
     *         same components)
     */
    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }
    
    /**
     * @return The squared length of this quaternion (identical to a 4-vector
     *         with the same components)
     */
    public float lengthSquared() {
        return getX() * getX() + getY() * getY() + getZ() * getZ() + getW() * getW();
    }

    /**
     * @param q The other quaternion involved in the dot product
     * @return The dot product between this and <tt>q</tt>, which is the same as
     *         if both were treated as 4-vectors.
     * @throws NullPointerException if q is null
     */
    public float dot(ReadOnlyQuat4f q) {
        return getX() * q.getX() + getY() * q.getY() + getZ() * q.getZ() + getW() * q.getW();
    }

    /**
     * Return the dot product between this quaternion and the quaternion
     * implicitly formed by (x, y, z, w), which is the same as if both are
     * treated as 4-vectors.
     * 
     * @param x X coordinate of the other quaternion
     * @param y Y coordinate of the other quaternion
     * @param z Z coordinate of the other quaternion
     * @param w W coordinate of the other quaternion
     * @return The dot product
     */
    public float dot(float x, float y, float z, float w) {
        return getX() * x + getY() * y + getZ() * z + getW() * w;
    }

    /**
     * @return The rotation, in radians, about the conceptual axis of rotation
     *         representing this quaternions transform
     */
    public float getAxisAngle() {
        return (float) (2 * Math.acos(getW()));
    }
    
    /**
     * @return A new Vector3f containing the axis of rotation for this
     *         quaternion
     */
    public MutableVector3f getAxis() {
        return getAxis(null);
    }

    /**
     * Compute and return the axis of rotation for this quaternion. If <tt>result</tt> is
     * not null, it is stored in result. Otherwise a new Vector3f is created and
     * returned.
     * 
     * @param result The vector to contain the axis
     * @return result, or a new Vector3f if null
     */
    public MutableVector3f getAxis(MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        
        float s2 = 1f - getW() * getW();
        if (s2 < .0001f)
            return result.set(1f, 0f, 0f); // arbitrary
        
        float s = (float) Math.sqrt(s2);
        return result.set(getX() / s, getY() / s, getZ() / s);
    }

    /**
     * @param q The other quaternion
     * @return The angle of rotation, in radians, between this quaternion and q
     * @throws NullPointerException if q is null
     * @throws ArithmeticException if there is a singularity when computing the
     *             angle between this and q (i.e. if either have 0-length)
     */
    public float angle(ReadOnlyQuat4f q) {
        float s = (float) Math.sqrt(lengthSquared() * q.lengthSquared());
        if (s == 0f)
            throw new ArithmeticException("Undefined angle between two quaternions");
        
        return (float) Math.acos(dot(q) / s);
    }

    /**
     * Compute the inverse of this quaternion and store it in <tt>result</tt>.
     * If result is null, create a new Quat4f and return it.
     * 
     * @param result The quaternion to hold the inverse, or null
     * @return result, or a new Quat4f if null
     */
    public MutableQuat4f inverse(MutableQuat4f result) {
        if (result == null)
            result = new Quat4f();
        return result.set(-getX(), -getY(), -getZ(), getW());
    }
    
    /**
     * Perform spherical linear interpolation between this quaternion and <tt>q</tt>.
     * The interpolated quaternion is stored in <tt>result</tt> or a new Quat4f if
     * result is null. The parameter <tt>t</tt> should be in the range <code>[0, 1]</code>
     * where a value of 0 represents this quaternion and a value of 1 represents <tt>q</tt>.
     * @param q The other quaternion
     * @param t Interpolation factor, from 0 to 1
     * @param result The quaternion to hold the result interpolation
     * @return result, or a new Quat4f if null
     * @throws IllegalArgumentException if t is not in the range [0, 1]
     */
    public MutableQuat4f slerp(ReadOnlyQuat4f q, float t, MutableQuat4f result) {
        if (t < 0 || t > 1)
            throw new IllegalArgumentException("t must be in [0, 1], not: " + t);
        
        if (result == null)
            result = new Quat4f();
        
        float theta = angle(q);
        if (theta != 0f) {
            float d = 1f / (float) Math.sin(theta);
            float s0 = (float) Math.sin((1 - t) * theta);
            float s1 = (float) Math.sin(t * theta);
            
            if (dot(q) < 0) // long angle case, see http://en.wikipedia.org/wiki/Slerp
                s1 *= -1;
            
            return result.set((getX() * s0 + q.getX() * s1) * d,
                              (getY() * s0 + q.getY() * s1) * d,
                              (getZ() * s0 + q.getZ() * s1) * d,
                              (getW() * s0 + q.getW() * s1) * d);
        } else
            return result.set(getX(), getY(), getZ(), getW());
    }
    
    /**
     * Get the given component from this quaternion; index must be 0 (x), 1 (y),
     * 2 (z), or 3 (w)
     * 
     * @param index The quaternion component to retrieve
     * @return The component at the given index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public float get(int index) {
        switch (index) {
        case 0:
            return getX();
        case 1:
            return getY();
        case 2:
            return getZ();
        case 3:
            return getW();
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }
    }

    /**
     * Store the four component values of this quaternion into vals, starting at
     * offset. The components should be placed consecutively, ordered x, y, z,
     * and w. It is assumed that the array has at least four positions
     * available, starting at offset.
     * 
     * @param vals Array to store this quaternion in
     * @param offset First array index to hold the x value
     * @throws NullPointerException if vals is null
     * @throws ArrayIndexOutOfBoundsException if there isn't enough room to
     *             store this quaternion at offset
     */
    public void get(float[] vals, int offset) {
        vals[offset] = getX();
        vals[offset + 1] = getY();
        vals[offset + 2] = getZ();
        vals[offset + 3] = getW();
    }
    
    /**
     * As {@link #get(float[], int)}, but with a FloatBuffer.
     * <tt>offset</tt> is measured from 0, not the buffer's position.
     * 
     * @param store The FloatBuffer to hold the row values
     * @param offset The first index to use in the store
     * @throws ArrayIndexOutOfBoundsException if store doesn't have enough space
     *             for the quaternion
     */
    public void get(FloatBuffer store, int offset) {
        store.put(offset, getX());
        store.put(offset + 1, getY());
        store.put(offset + 2, getZ());
        store.put(offset + 3, getW());
    }

    @Override
    public String toString() {
        return "[" + getX() + ", " + getY() + ", " + getZ() + ", " + getW() + "]";
    }

    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + Float.floatToIntBits(getX());
        result += 31 * result + Float.floatToIntBits(getY());
        result += 31 * result + Float.floatToIntBits(getZ());
        result += 31 * result + Float.floatToIntBits(getW());

        return result;
    }

    @Override
    public boolean equals(Object v) {
        // this conditional correctly handles null values
        if (!(v instanceof ReadOnlyQuat4f))
            return false;
        else
            return equals((ReadOnlyQuat4f) v);
    }

    /**
     * Return true if these two quaternions are numerically equal. Returns false if
     * v is null.
     * 
     * @param q Vector to test equality with
     * @return True if these quaternions are numerically equal
     */
    public boolean equals(ReadOnlyQuat4f q) {
        return q != null && getX() == q.getX() && getY() == q.getY() && getZ() == q.getZ() && getW() == q.getW();
    }

    /**
     * Determine if these two quaternions are equal, within an error range of eps.
     * Returns false if q is null.
     * 
     * @param q Quaternion to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of q
     */
    public boolean epsilonEquals(ReadOnlyQuat4f q, float eps) {
        if (q == null)
            return false;

        float tx = Math.abs(getX() - q.getX());
        float ty = Math.abs(getY() - q.getY());
        float tz = Math.abs(getZ() - q.getZ());
        float tw = Math.abs(getW() - q.getW());

        return tx <= eps && ty <= eps && tz <= eps && tw <= eps;
    }
}
