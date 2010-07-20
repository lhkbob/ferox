package com.ferox.math;

/**
 * <p>
 * ReadOnlyVector4f provides the foundation class for the implementation of a
 * 4-component vector. It is read-only in the sense that all operations expose a
 * <tt>result</tt> parameter that stores the computation. The calling vector is
 * not modified unless it happens to be the result. Additionally, this class
 * only exposes accessors to its data and no mutators. The class
 * {@link Vector4f} provides a standard implementation of ReadOnlyVector4f that
 * exposes mutators for the 4 values of the vector.
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
public abstract class ReadOnlyVector4f {
    /**
     * Compute the length of this vector.
     * 
     * @return Length of this vector
     */
    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    /**
     * Compute the length of this vector, squared. Often, when only length
     * comparisons are necessary, the sauared length is useful because its
     * computation doesn't involve a slow square root operation.
     * 
     * @return The squared length of this vector
     */
    public float lengthSquared() {
        return getX() * getX() + getY() * getY() + getZ() * getZ() + getW() * getW();
    }
    
    /**
     * Compute the distance between this vector and <tt>v</tt>, treating both
     * vectors as 4D points.
     * 
     * @param v The other vector
     * @return The distance between this and v
     * @throws NullPointerException if v is null
     */
    public float distance(ReadOnlyVector4f v) {
        return (float) Math.sqrt(distanceSquared(v));
    }

    /**
     * Compute the square of the distance between this and <tt>v</tt>, treating
     * both vectors as 4D points.
     * 
     * @param v The other vector
     * @return The distance between this and v
     * @throws NullPointerException if v is null
     */
    public float distanceSquared(ReadOnlyVector4f v) {
        float dx = getX() - v.getX();
        float dy = getY() - v.getY();
        float dz = getZ() - v.getZ();
        float dw = getW() - v.getW();
        return dx * dx + dy * dy + dz * dz + dw * dw;
    }

    /**
     * Compute and return the shortest angle between this vector and v. The
     * returned angle must be in radians.
     * 
     * @param v The other vector involved
     * @return The smallest angle, in radians, between this and v
     * @throws NullPointerException if v is null
     */
    public float angle(ReadOnlyVector4f v) {
        return (float) Math.acos(dot(v) / (length() * v.length()));
    }

    /**
     * Compute and return the dot product between this vector and the given
     * vector, v.
     * 
     * @param v Vector to compute dot product with
     * @return The dot product between v and this
     * @throws NullPointerException if v is null
     */
    public float dot(ReadOnlyVector4f v) {
        return dot(v.getX(), v.getY(), v.getZ(), v.getW());
    }

    /**
     * Compute and return the dot product between this vector and a vector
     * represented by <x, y, z, w>
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @param w W coordinate of other vector
     * @return The dot product between this and <x, y, z, w>
     */
    public float dot(float x, float y, float z, float w) {
        return getX() * x + getY() * y + getZ() * z + getW() * w;
    }

    /**
     * <p>
     * Orthogonalize this vector using the Gram-Schmidt process so that the
     * resultant vector is orthogonal to proj. The orthogonal vector is stored
     * in result. If result is null, a new Vector4f should be created and
     * returned.
     * </p>
     * <p>
     * The computed orthogonal vector will be in the same plane as this and
     * proj. If this vector angles to the left of proj, the orthogonal vector is
     * to the left; if this is to the right, the orthogonal vector is to the
     * right.
     * </p>
     * 
     * @param proj The vector that the resultant vector is orthogonal to
     * @param result Vector to store the result, or null
     * @return result, or a new Vector4f if null, holding an orthogonal vector
     *         to proj, based off of this vector
     * @throws NullPointerException if proj is null
     */
    public Vector4f ortho(ReadOnlyVector4f proj, Vector4f result) {
        // remember this vector, in case it's the same as result
        float tx = getX();
        float ty = getY();
        float tz = getZ();
        float tw = getW();

        result = project(proj, result);
        return result.set(tx - result.getX(), ty - result.getY(), tz - result.getZ(), tw - result.getW());
    }

    /**
     * Project this vector onto proj and store it in result. The resulting
     * projection will be parallel to proj, but possibly pointing in the
     * opposite direction. If result is null, a new Vector4f should be created
     * and returned.
     * 
     * @param proj The vector that will be projected onto
     * @param result Vector to store the result, or null
     * @return result, or a new Vector4f if null, holding the projection of this
     *         vector onto proj
     * @throws NullPointerException if proj is null
     */
    public Vector4f project(ReadOnlyVector4f proj, Vector4f result) {
        return proj.scale(dot(proj) / proj.lengthSquared(), result);
    }

    /**
     * Add this and v together (this + v) and store the added vector in result.
     * If result is null, a new Vector4f should be created and returned.
     * 
     * @param v The vector to be added to this
     * @param result Vector to store the result, or null
     * @return result, or a new Vector4f if null, holding the addition result
     * @throws NullPointerException if v is null
     */
    public Vector4f add(ReadOnlyVector4f v, Vector4f result) {
        return add(v.getX(), v.getY(), v.getZ(), v.getW(), result);
    }

    /**
     * Add this and <x, y, z, w> together (this + <x, y, z, w>) and store the
     * added vector in result. If result is null, a new Vector4f should be
     * created and returned.
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @param w W coordinate of other vector
     * @param result Vector to store the result, or null
     * @return result, or a new Vector4f if null, holding the addition result
     */
    public Vector4f add(float x, float y, float z, float w, Vector4f result) {
        if (result == null)
            result = new Vector4f();
        return result.set(getX() + x, getY() + y, getZ() + z, getW() + w);
    }

    /**
     * Subtract v from this vector (this - v) and store the subtraction in
     * result. If result is null, a new Vector4f should be created and returned.
     * 
     * @param v The vector to be subtracted from this
     * @param result Vector to store the result, or null
     * @return result, or a new Vector4f if null, holding the subtraction result
     * @throws NullPointerException if v is null
     */
    public Vector4f sub(ReadOnlyVector4f v, Vector4f result) {
        return sub(v.getX(), v.getY(), v.getZ(), v.getW(), result);
    }

    /**
     * Subtract <x, y, z, w> from this vector (this - <x, y, z, w>) and store
     * the subtraction in result. If result is null, a new Vector4f should be
     * created and returned.
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @param w W coordinate of other vector
     * @param result Vector to store the result, or null
     * @return result, or a new Vector4f if null, holding the subtraction result
     */
    public Vector4f sub(float x, float y, float z, float w, Vector4f result) {
        if (result == null)
            result = new Vector4f();
        return result.set(getX() - x, getY() - y, getZ() - z, getW() - w);
    }

    /**
     * Scale this vector by scalar and then add it to v (scalar*this + v),
     * storing the final computation in result. If result is null, a new
     * Vector4f should be created and returned.
     * 
     * @param scalar The scaling factor applied to this vector
     * @param add The vector added to this vector after scaling
     * @param result Vector to store the result, or null
     * @return result, or a new Vector4f if null, holding the subtraction result
     * @throws NullPointerException if v is null
     */
    public Vector4f scaleAdd(float scalar, ReadOnlyVector4f add, Vector4f result) {
        if (result == null)
            result = new Vector4f();
        return result.set(scalar * getX() + add.getX(), 
                          scalar * getY() + add.getY(), 
                          scalar * getZ() + add.getZ(),
                          scalar * getW() + add.getW());
    }

    /**
     * Scale this vector by the given scalar (scalar*this) and store it in
     * result. If result is null, a new Vector4f should be created and returned.
     * 
     * @param scalar Scaling factor to use
     * @param result Vector to store the result, or null
     * @return result, or a new Vector4f if null, holding the scaled vector
     */
    public Vector4f scale(float scalar, Vector4f result) {
        if (result == null)
            result = new Vector4f();
        return result.set(scalar * getX(), scalar * getY(), scalar * getZ(), scalar * getW());
    }

    /**
     * Normalize this vector to be of length 1 and store it in result. If result
     * is null, a new Vector4f should be created and returned. This vector can't
     * be normalized if it's length is 0. If it's length is very close to 0, the
     * results may suffer from loss of precision.
     * 
     * @param result Vector to store the result, or null
     * @return result, or a new Vector4f if null, holding the subtraction result
     * @throws ArithmeticException if this vector can't be normalized
     */
    public Vector4f normalize(Vector4f result) {
        float d = length();
        if (d == 0f)
            throw new ArithmeticException("Cannot normalize 0 vector");
        return scale(1 / d, result);
    }
    
    /**
     * Get the given component from this vector; index must be 0 (x), 1 (y), 2
     * (z), or 3 (w)
     * 
     * @param index The vector component to retrieve
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
     * Store the four component values of this vector into vals, starting at
     * offset. The components should be placed consecutively, ordered x, y, z,
     * and w. It is assumed that the array has at least four positions
     * available, starting at offset.
     * 
     * @param vals Array to store this vector in
     * @param offset First array index to hold the x value
     * @throws NullPointerException if vals is null
     * @throws ArrayIndexOutOfBoundsException if there isn't enough room to
     *             store this vector at offset
     */
    public void get(float[] vals, int offset) {
        vals[offset] = getX();
        vals[offset + 1] = getY();
        vals[offset + 2] = getZ();
        vals[offset + 3] = getW();
    }

    /**
     * Return a ReadOnlyVector3f that wraps this 4-component vector. Any changes
     * to the values within this vector will be reflected by the returned
     * 3-component vector.
     * 
     * @return A 3-component vector wrapping this 4-component vector
     */
    public ReadOnlyVector3f getAsVector3f() {
        return new ReducedVector();
    }

    /**
     * @return The 1st coordinate of the vector, commonly referred to as X
     */
    public abstract float getX();
    
    /**
     * @return The 2nd coordinate of the vector, commonly referred to as Y
     */
    public abstract float getY();
    
    /**
     * @return The 3rd coordinate of the vector, commonly referred to as Z
     */
    public abstract float getZ();
    
    /**
     * @return The 4th coordinate of the vector, commonly referred to as W
     */
    public abstract float getW();
    

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
        if (!(v instanceof ReadOnlyVector4f))
            return false;
        else
            return equals((ReadOnlyVector4f) v);
    }

    /**
     * Return true if these two vectors are numerically equal. Returns false if
     * v is null.
     * 
     * @param v Vector to test equality with
     * @return True if these vectors are numerically equal
     */
    public boolean equals(ReadOnlyVector4f v) {
        return v != null && getX() == v.getX() && getY() == v.getY() && getZ() == v.getZ() && getW() == v.getW();
    }

    /**
     * Determine if these two vectors are equal, within an error range of eps.
     * Returns false if v is null
     * 
     * @param v Vector to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of v
     */
    public boolean epsilonEquals(ReadOnlyVector4f v, float eps) {
        if (v == null)
            return false;

        float tx = Math.abs(getX() - v.getX());
        float ty = Math.abs(getY() - v.getY());
        float tz = Math.abs(getZ() - v.getZ());
        float tw = Math.abs(getW() - v.getW());

        return tx <= eps && ty <= eps && tz <= eps && tw <= eps;
    }
    
    // FIXME: should this instead treat it as a Homogenous vector
    // and automatically divide by w?
    private class ReducedVector extends ReadOnlyVector3f {
        @Override
        public float getX() {
            return ReadOnlyVector4f.this.getX();
        }

        @Override
        public float getY() {
            return ReadOnlyVector4f.this.getY();
        }

        @Override
        public float getZ() {
            return ReadOnlyVector4f.this.getZ();
        }
    }
}
