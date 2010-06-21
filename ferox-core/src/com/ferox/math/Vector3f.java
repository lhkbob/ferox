package com.ferox.math;

/**
 * <p>
 * Vector3f provides a final implementation of a 3-dimensional vector. The three
 * components of the vector are available as public fields. There is no need for
 * further abstraction because a vector is just 3 values.
 * </p>
 * <p>
 * In all mathematical functions that compute a new vector, there is a method
 * parameter, often named result, that will hold the computed value. This result
 * vector is also returned so that complex mathematical expressions can be
 * chained. It is always safe to use the calling vector as the result vector.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Vector3f implements Cloneable {
    public float x;
    public float y;
    public float z;

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
    public Vector3f(Vector3f v) {
        this(v.x, v.y, v.z);
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
        return x * x + y * y + z * z;
    }

    /**
     * Compute the distance between this vector and <tt>v</tt>, treating both
     * vectors as 3D points.
     * 
     * @param v The other vector
     * @return The distance between this and v
     * @throws NullPointerException if v is null
     */
    public float distance(Vector3f v) {
        return (float) Math.sqrt(distanceSquared(v));
    }

    /**
     * Compute the square of the distance between this and <tt>v</tt>, treating
     * both vectors as 3D points.
     * 
     * @param v The other vector
     * @return The distance between this and v
     * @throws NullPointerException if v is null
     */
    public float distanceSquared(Vector3f v) {
        return (x - v.x) * (x - v.x) + (y - v.y) * (y - v.y) + (z - v.z) * (z - v.z);
    }

    /**
     * Compute and return the shortest angle between this vector and v. The
     * returned angle must be in radians.
     * 
     * @param v The other vector involved
     * @return The smallest angle, in radians, between this and v
     * @throws NullPointerException if v is null
     */
    public float angle(Vector3f v) {
        return (float) Math.acos(dot(v) / (length() * v.length()));
    }

    /**
     * Compute and return the shortest angle between this vector and <x, y, z>.
     * The returned angle must be in radians.
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @return The smallest angle, in radians, between this and <x, y, z>
     */
    public float angle(float x, float y, float z) {
        float len = x * x + y * y + z * z;
        return (float) Math.acos(dot(x, y, z) / (length() * len));
    }

    /**
     * Compute and return the dot product between this vector and the given
     * vector, v.
     * 
     * @param v Vector to compute dot product with
     * @return The dot product between v and this
     * @throws NullPointerException if v is null
     */
    public float dot(Vector3f v) {
        return x * v.x + y * v.y + z * v.z;
    }

    /**
     * Compute and return the dot product between this vector and a vector
     * represented by <x, y, z>
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @return The dot product between this and <x, y, z>
     */
    public float dot(float x, float y, float z) {
        return this.x * x + this.y * y + this.z * z;
    }

    /**
     * <p>
     * Orthogonalize this vector using the Gram-Schmidt process so that the
     * resultant vector is orthogonal to proj. The orthogonal vector is stored
     * in result. If result is null, a new Vector3f should be created and
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
     * @return result, or a new Vector3f if null, holding an orthogonal vector
     *         to proj, based off of this vector
     * @throws NullPointerException if proj is null
     */
    public Vector3f ortho(Vector3f proj, Vector3f result) {
        return ortho(proj.x, proj.y, proj.z, result);
    }

    /**
     * As ortho(proj, result) where result is this vector
     * 
     * @param proj
     * @return This vector
     */
    public Vector3f ortho(Vector3f proj) {
        return ortho(proj, this);
    }

    /**
     * Orthogonalize this vector using the Gram-Schmidt process so that the
     * resultant vector is orthogonal to <x, y, z>. The orthogonal vector is
     * stored in result. If result is null, a new Vector3f should be created and
     * returned.
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding an orthogonal vector
     *         to <x, y, z>, based off of this vector
     */
    public Vector3f ortho(float x, float y, float z, Vector3f result) {
        // remember this vector, in case it's the same as result
        float tx = this.x;
        float ty = this.y;
        float tz = this.z;

        result = project(x, y, z, result);
        return result.set(tx - result.x, ty - result.y, tz - result.z);
    }

    /**
     * Project this vector onto proj and store it in result. The resulting
     * projection will be parallel to proj, but possibly pointing in the
     * opposite direction. If result is null, a new Vector3f should be created
     * and returned.
     * 
     * @param proj The vector that will be projected onto
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the projection of this
     *         vector onto proj
     * @throws NullPointerException if proj is null
     */
    public Vector3f project(Vector3f proj, Vector3f result) {
        return proj.scale(dot(proj) / proj.lengthSquared(), result);
    }

    /**
     * As project(proj, result) where result is this vector
     * 
     * @param proj
     * @return This vector
     */
    public Vector3f project(Vector3f proj) {
        return project(proj, this);
    }

    /**
     * Project this vector onto <x, y, z> and store it in result. The resulting
     * projection will be parallel to <x, y, z>, but possibly pointing in the
     * opposite direction. If result is null, a new Vector3f should be created
     * and returned.
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the projection of this
     *         vector onto <x, y, z>
     */
    public Vector3f project(float x, float y, float z, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        float scale = dot(x, y, z) / (x * x + y * y + z * z);
        return result.set(scale * x, scale * y, scale * z);
    }

    /**
     * Compute the cross product (this X v) and store it in result. If result is
     * null, a new Vector3f should be created and returned.
     * 
     * @param v Right-hand side of the cross product
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the cross product
     * @throws NullPointerException if v is null
     */
    public Vector3f cross(Vector3f v, Vector3f result) {
        return cross(v.x, v.y, v.z, result);
    }

    /**
     * As cross(v, result) where result is this vector
     * 
     * @param v
     * @return This vector
     */
    public Vector3f cross(Vector3f v) {
        return cross(v, this);
    }

    /**
     * Compute the cross product (this X <x, y, z>) and store it in result. If
     * result is null, a new Vector3f should be created and returned.
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the cross product
     */
    public Vector3f cross(float x, float y, float z, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        return result.set(this.y * z - y * this.z, this.z * x - z * this.x, this.x * y - x * this.y);
    }

    /**
     * Add this and v together (this + v) and store the added vector in result.
     * If result is null, a new Vector3f should be created and returned.
     * 
     * @param v The vector to be added to this
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the addition result
     * @throws NullPointerException if v is null
     */
    public Vector3f add(Vector3f v, Vector3f result) {
        return add(v.x, v.y, v.z, result);
    }

    /**
     * As add(v, result) where result is this vector
     * 
     * @param v
     * @return This vector
     */
    public Vector3f add(Vector3f v) {
        return add(v, this);
    }

    /**
     * Add this and <x, y, z> together (this + <x, y, z>) and store the added
     * vector in result. If result is null, a new Vector3f should be created and
     * returned.
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the addition result
     */
    public Vector3f add(float x, float y, float z, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        return result.set(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Subtract v from this vector (this - v) and store the subtraction in
     * result. If result is null, a new Vector3f should be created and returned.
     * 
     * @param v The vector to be subtracted from this
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the subtraction result
     * @throws NullPointerException if v is null
     */
    public Vector3f sub(Vector3f v, Vector3f result) {
        return sub(v.x, v.y, v.z, result);
    }

    /**
     * As sub(v, result) where result is this vector
     * 
     * @param v
     * @return This vector
     */
    public Vector3f sub(Vector3f v) {
        return sub(v, this);
    }

    /**
     * Subtract <x, y, z> from this vector (this - <x, y, z>) and store the
     * subtraction in result. If result is null, a new Vector3f should be
     * created and returned.
     * 
     * @param x X coordinate of other vector
     * @param y Y coordinate of other vector
     * @param z Z coordinate of other vector
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the subtraction result
     */
    public Vector3f sub(float x, float y, float z, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        return result.set(this.x - x, this.y - y, this.z - z);
    }

    /**
     * Scale this vector by scalar and then add it to v (scalar*this + v),
     * storing the final computation in result. If result is null, a new
     * Vector3f should be created and returned.
     * 
     * @param scalar The scaling factor applied to this vector
     * @param add The vector added to this vector after scaling
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the subtraction result
     * @throws NullPointerException if v is null
     */
    public Vector3f scaleAdd(float scalar, Vector3f add, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        return result.set(scalar * x + add.x, scalar * y + add.y, scalar * z + add.z);
    }

    /**
     * As scaleAdd(scalar, add, result) where result is this vector.
     * 
     * @param scalar
     * @param add
     * @return This vector
     */
    public Vector3f scaleAdd(float scalar, Vector3f add) {
        return scaleAdd(scalar, add, this);
    }

    /**
     * Scale this vector by the given scalar (scalar*this) and store it in
     * result. If result is null, a new Vector3f should be created and returned.
     * 
     * @param scalar Scaling factor to use
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the scaled vector
     */
    public Vector3f scale(float scalar, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        return result.set(scalar * x, scalar * y, scalar * z);
    }

    /**
     * As scale(scalar, result) where result is this vector
     * 
     * @param scalar
     * @return This vector
     */
    public Vector3f scale(float scalar) {
        return scale(scalar, this);
    }

    /**
     * Normalize this vector to be of length 1 and store it in result. If result
     * is null, a new Vector3f should be created and returned. This vector can't
     * be normalized if it's length is 0. If it's length is very close to 0, the
     * results may suffer from loss of precision.
     * 
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the subtraction result
     * @throws ArithmeticException if this vector can't be normalized
     */
    public Vector3f normalize(Vector3f result) {
        return scale(1 / length(), result);
    }

    /**
     * Normalize this vector in place
     * 
     * @return This vector
     */
    public Vector3f normalize() {
        return normalize(this);
    }

    @Override
    public Vector3f clone() {
        return new Vector3f(this);
    }

    /**
     * Get the given component from this vector; index must be 0 (x), 1 (y), or
     * 2 (z).
     * 
     * @param index The vector component to retrieve
     * @return The component at the given index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public float get(int index) {
        switch (index) {
        case 0:
            return x;
        case 1:
            return y;
        case 2:
            return z;
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 2]");
        }
    }

    /**
     * Store the three component values of this vector into vals, starting at
     * offset. The components should be placed consecutively, ordered x, y, and
     * z. It is assumed that the array has at least three positions available,
     * starting at offset.
     * 
     * @param vals Array to store this vector in
     * @param offset First array index to hold the x value
     * @throws NullPointerException if vals is null
     * @throws ArrayIndexOutOfBoundsException if there isn't enough room to
     *             store this vector at offset
     */
    public void get(float[] vals, int offset) {
        vals[offset] = x;
        vals[offset + 1] = y;
        vals[offset + 2] = z;
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
    public Vector3f set(int index, float val) {
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
        default:
            throw new IndexOutOfBoundsException("Index must be in [0, 3]");
        }

        return this;
    }

    /**
     * Set the x, y, and z values of this Vector3f to the values held in v.
     * 
     * @param v Vector to be copied into this
     * @return This vector
     * @throws NullPointerException if v is null
     */
    public Vector3f set(Vector3f v) {
        return set(v.x, v.y, v.z);
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
    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;

        return this;
    }

    /**
     * Set the x, y, and z values of this Vector3f to the three values held
     * within the vals array, starting at offset.
     * 
     * @param vals Array to take 3 component values from
     * @param offset Index of the x coordinate
     * @return This vector
     * @throws ArrayIndexOutOfBoundsException if vals doesn't have three values
     *             starting at offset
     */
    public Vector3f set(float[] vals, int offset) {
        return set(vals[offset], vals[offset + 1], vals[offset + 2]);
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + "]";
    }

    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + Float.floatToIntBits(x);
        result += 31 * result + Float.floatToIntBits(y);
        result += 31 * result + Float.floatToIntBits(z);

        return result;
    }

    @Override
    public boolean equals(Object v) {
        // this conditional correctly handles null values
        if (!(v instanceof Vector3f))
            return false;
        else
            return equals((Vector3f) v);
    }

    /**
     * Return true if these two vectors are numerically equal. Returns false if
     * e is null.
     * 
     * @param e Vector to test equality with
     * @return True if these vectors are numerically equal
     */
    public boolean equals(Vector3f e) {
        return e != null && x == e.x && y == e.y && z == e.z;
    }

    /**
     * Determine if these two vectors are equal, within an error range of eps.
     * Returns false if v is null.
     * 
     * @param v Vector to check approximate equality to
     * @param eps Error tolerance of each component
     * @return True if all component values are within eps of the corresponding
     *         component of v
     */
    public boolean epsilonEquals(Vector3f v, float eps) {
        if (v == null)
            return false;

        float tx = Math.abs(x - v.x);
        float ty = Math.abs(y - v.y);
        float tz = Math.abs(z - v.z);

        return tx <= eps && ty <= eps && tz <= eps;
    }
}
