package com.ferox.math;

import com.ferox.math.bounds.Plane;

/**
 * <p>
 * ReadOnlyVector3f provides the foundation class for the implementation of a
 * 3-component vector. It is read-only in the sense that all operations expose a
 * <tt>result</tt> parameter that stores the computation. The calling vector is
 * not modified unless it happens to be the result. Additionally, this class
 * only exposes accessors to its data and no mutators. The class
 * {@link Vector3f} provides a standard implementation of ReadOnlyVector3f that
 * exposes mutators for the 3 values of the vector.
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
public abstract class ReadOnlyVector3f {
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
        return getX() * getX() + getY() * getY() + getZ() * getZ();
    }

    /**
     * Compute the distance between this vector and <tt>v</tt>, treating both
     * vectors as 3D points.
     * 
     * @param v The other vector
     * @return The distance between this and v
     * @throws NullPointerException if v is null
     */
    public float distance(ReadOnlyVector3f v) {
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
    public float distanceSquared(ReadOnlyVector3f v) {
        float dx = getX() - v.getX();
        float dy = getY() - v.getY();
        float dz = getZ() - v.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Compute and return the shortest angle between this vector and v. The
     * returned angle must be in radians.
     * 
     * @param v The other vector involved
     * @return The smallest angle, in radians, between this and v
     * @throws NullPointerException if v is null
     */
    public float angle(ReadOnlyVector3f v) {
        return (float) Math.acos(dot(v) / (length() * v.length()));
    }

    /**
     * Return a new Quat4f containing the shortest rotation arc from this vector
     * to <tt>v</tt>.
     * 
     * @param v The other vector
     * @return A new Quat4f containing the rotation from this to v
     */
    public Quat4f arc(ReadOnlyVector3f v) {
        return arc(v, null);
    }

    /**
     * Compute the shortest rotation arc from this vector to <tt>v</tt>. The
     * rotation is stored in <tt>result</tt>, or if result is null, a new Quat4f
     * is created and returned.
     * 
     * @param v The other vector
     * @param result The Quat4f containing the rotation or null
     * @return result, or a new Quat4f if it was null
     * @throws NullPointerException if v was null
     */
    public Quat4f arc(ReadOnlyVector3f v, Quat4f result) {
        if (result == null)
            result = new Quat4f();
        
        // make sure that both this and v are normalized
        // if they aren't unit length, get the normalized version as new vectors
        // FIXME: can we do this better?
        // FIXME: is it worth it, or should we just always call normalize(null)
        // FIXME: if it is worth it, should probably do an eps compare and not strict ==
        float d0 = lengthSquared();
        float d1 = lengthSquared();
        ReadOnlyVector3f v0 = (d0 == 1f ? this : scale(1f / (float) Math.sqrt(d0), null));
        ReadOnlyVector3f v1 = (d1 == 1f ? v : v.scale(1f / (float) Math.sqrt(d1), null));
        
        float d = v0.dot(v1);
        if (d < 1f) {
            Vector3f n = new Vector3f();
            Plane.getTangentSpace(v0, n, new Vector3f());
            
            return result.set(n.getX(), n.getY(), n.getZ(), 0f);
        } else {
            Vector3f c = v0.cross(v1, null);
            float s = (float) Math.sqrt(2f * (1f + d));
            float rs = 1f / s;
            
            return result.set(c.getX() * rs, c.getY() * rs, c.getZ() * rs, .5f * s);
        }
    }

    /**
     * Compute and return the dot product between this vector and the given
     * vector, v.
     * 
     * @param v Vector to compute dot product with
     * @return The dot product between v and this
     * @throws NullPointerException if v is null
     */
    public float dot(ReadOnlyVector3f v) {
        return getX() * v.getX() + getY() * v.getY() + getZ() * v.getZ();
    }

    /**
     * Compute and return the dot product between this vector and the implicit
     * vector represented by <x, y, z>.
     * 
     * @param x The x coordinate of the other vector
     * @param y The y coordinate of the other vector
     * @param z The z coordinate of the other vector
     * @return The dot product
     */
    public float dot(float x, float y, float z) {
        return getX() * x + getY() * y + getZ() * z;
    }

    /**
     * <p>
     * Orthogonalize this vector using the Gram-Schmidt process so that the
     * resultant vector is orthogonal to proj. The orthogonal vector is stored
     * in result. If result is null, a new Vector3f is created and returned.
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
    public Vector3f ortho(ReadOnlyVector3f proj, Vector3f result) {
        // remember this vector, in case it's the same as result
        float tx = getX();
        float ty = getY();
        float tz = getZ();

        result = project(proj, result);
        return result.set(tx - result.getX(), ty - result.getY(), tz - result.getZ());
    }

    /**
     * Project this vector onto proj and store it in result. The resulting
     * projection will be parallel to proj, but possibly pointing in the
     * opposite direction. If result is null, a new Vector3f is created and
     * returned.
     * 
     * @param proj The vector that will be projected onto
     * @param result Vector to store the result, or null
     * @return result, or a new Vector3f if null, holding the projection of this
     *         vector onto proj
     * @throws NullPointerException if proj is null
     */
    public Vector3f project(ReadOnlyVector3f proj, Vector3f result) {
        return proj.scale(dot(proj) / proj.lengthSquared(), result);
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
    public Vector3f cross(ReadOnlyVector3f v, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        return result.set(getY() * v.getZ() - v.getY() * getZ(), 
                          getZ() * v.getX() - v.getZ() * getX(), 
                          getX() * v.getY() - v.getX() * getY());
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
    public Vector3f add(ReadOnlyVector3f v, Vector3f result) {
        return add(v.getX(), v.getY(), v.getZ(), result);
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
        return result.set(getX() + x, getY() + y, getZ() + z);
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
    public Vector3f sub(ReadOnlyVector3f v, Vector3f result) {
        return sub(v.getX(), v.getY(), v.getZ(), result);
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
        return result.set(getX() - x, getY() - y, getZ() - z);
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
    public Vector3f scaleAdd(float scalar, ReadOnlyVector3f add, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        return result.set(scalar * getX() + add.getX(), 
                          scalar * getY() + add.getY(), 
                          scalar * getZ() + add.getZ());
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
        return result.set(scalar * getX(), scalar * getY(), scalar * getZ());
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
        float d = length();
        if (d == 0f)
            throw new ArithmeticException("Can't normalize 0 vector");
        return scale(1f / d, result);
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
            return getX();
        case 1:
            return getY();
        case 2:
            return getZ();
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
        vals[offset] = getX();
        vals[offset + 1] = getY();
        vals[offset + 2] = getZ();
    }

    /**
     * Return a 4-component vector that wraps the x, y, and z components of this
     * 3-component vector. Any changes to this vector will be reflected by the
     * returned ReadOnlyVector4f. The w-coordinate of the 4-component vector
     * will return the value of <tt>w</tt>.
     * 
     * @param w The w coordinate value to use
     * @return A 4-component vector wrapping this vector
     */
    public ReadOnlyVector4f getAsVector4f(float w) {
        return new ExpandedVector(w);
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
    
    @Override
    public String toString() {
        return "[" + getX() + ", " + getY() + ", " + getZ() + "]";
    }

    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + Float.floatToIntBits(getX());
        result += 31 * result + Float.floatToIntBits(getY());
        result += 31 * result + Float.floatToIntBits(getZ());

        return result;
    }

    @Override
    public boolean equals(Object v) {
        // this conditional correctly handles null values
        if (!(v instanceof ReadOnlyVector3f))
            return false;
        else
            return equals((ReadOnlyVector3f) v);
    }

    /**
     * Return true if these two vectors are numerically equal. Returns false if
     * e is null.
     * 
     * @param e Vector to test equality with
     * @return True if these vectors are numerically equal
     */
    public boolean equals(ReadOnlyVector3f e) {
        return e != null && getX() == e.getX() && getY() == e.getY() && getZ() == e.getZ();
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
    public boolean epsilonEquals(ReadOnlyVector3f v, float eps) {
        if (v == null)
            return false;

        float tx = Math.abs(getX() - v.getX());
        float ty = Math.abs(getY() - v.getY());
        float tz = Math.abs(getZ() - v.getZ());

        return tx <= eps && ty <= eps && tz <= eps;
    }
    
    private class ExpandedVector extends ReadOnlyVector4f {
        private final float w;
        
        public ExpandedVector(float w) {
            this.w = w;
        }
        
        @Override
        public float getW() {
            return w;
        }

        @Override
        public float getX() {
            return ReadOnlyVector3f.this.getX();
        }

        @Override
        public float getY() {
            return ReadOnlyVector3f.this.getY();
        }

        @Override
        public float getZ() {
            return ReadOnlyVector3f.this.getZ();
        }
    }
}
