package com.ferox.math;

/**
 * <p>
 * Vector4f provides a final implementation of a 4-dimensional vector. The four
 * components of the vector are available as public fields. There is no need for
 * further abstraction because a vector is just 4 values.
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
public final class Vector4f implements Cloneable {
	public float x;
	public float y;
	public float z;
	public float w;

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
	 */
	public Vector4f(Vector4f v) {
		this(v.x, v.y, v.z, v.w);
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
		return x * x + y * y + z * z + w * w;
	}

	/**
	 * Compute and return the shortest angle between this vector and v. The
	 * returned angle must be in radians.
	 * 
	 * @param v The other vector involved
	 * @return The smallest angle, in radians, between this and v
	 * @throws NullPointerException if v is null
	 */
	public float angle(Vector4f v) {
		return (float) Math.acos(dot(v) / (length() * v.length()));
	}

	/**
	 * Compute and return the shortest angle between this vector and <x, y, z,
	 * w>. The returned angle must be in radians.
	 * 
	 * @param x X coordinate of other vector
	 * @param y Y coordinate of other vector
	 * @param z Z coordinate of other vector
	 * @param w W coordinate of other vector
	 * @return The smallest angle, in radians, between this and <x, y, z, w>
	 */
	public float angle(float x, float y, float z, float w) {
		float len = x * x + y * y + z * z + w * w;
		return (float) Math.acos(dot(x, y, z, w) / (length() * len));
	}

	/**
	 * Compute and return the dot product between this vector and the given
	 * vector, v.
	 * 
	 * @param v Vector to compute dot product with
	 * @return The dot product between v and this
	 * @throws NullPointerException if v is null
	 */
	public float dot(Vector4f v) {
		return dot(v.x, v.y, v.z, v.w);
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
		return this.x * x + this.y * y + this.z * z + this.w * w;
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
	public Vector4f ortho(Vector4f proj, Vector4f result) {
		return ortho(proj.x, proj.y, proj.z, proj.w, result);
	}

	/**
	 * As ortho(proj, result) where result is the calling vector.
	 * 
	 * @param proj
	 * @return This vector
	 */
	public Vector4f ortho(Vector4f proj) {
		return ortho(proj, this);
	}

	/**
	 * Orthogonalize this vector using the Gram-Schmidt process so that the
	 * resultant vector is orthogonal to <x, y, z, w>. The orthogonal vector is
	 * stored in result. If result is null, a new Vector4f should be created and
	 * returned.
	 * 
	 * @param x X coordinate of other vector
	 * @param y Y coordinate of other vector
	 * @param z Z coordinate of other vector
	 * @param w W coordinate of other vector
	 * @param result Vector to store the result, or null
	 * @return result, or a new Vector4f if null, holding an orthogonal vector
	 *         to <x, y, z, w>, based off of this vector
	 */
	public Vector4f ortho(float x, float y, float z, float w, Vector4f result) {
		// remember this vector, in case it's the same as result
		float tx = this.x;
		float ty = this.y;
		float tz = this.z;
		float tw = this.w;

		result = project(x, y, z, w, result);
		return result.set(tx - result.x, ty - result.y, tz - result.z, tw - result.w);
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
	public Vector4f project(Vector4f proj, Vector4f result) {
		return proj.scale(dot(proj) / proj.lengthSquared(), result);
	}

	/**
	 * As project(proj, result) where result is the calling vector.
	 * 
	 * @param proj
	 * @return This vector
	 */
	public Vector4f project(Vector4f proj) {
		return project(proj, this);
	}

	/**
	 * Project this vector onto <x, y, z, w> and store it in result. The
	 * resulting projection will be parallel to <x, y, z, w>, but possibly
	 * pointing in the opposite direction. If result is null, a new Vector4f
	 * should be created and returned.
	 * 
	 * @param x X coordinate of other vector
	 * @param y Y coordinate of other vector
	 * @param z Z coordinate of other vector
	 * @param w W coordinate of other vector
	 * @param result Vector to store the result, or null
	 * @return result, or a new Vector4f if null, holding the projection of this
	 *         vector onto <x, y, z, w>
	 */
	public Vector4f project(float x, float y, float z, float w, Vector4f result) {
		if (result == null)
			result = new Vector4f();
		float scale = dot(x, y, z, w) / (x * x + y * y + z * z + w * w);
		return result.set(scale * x, scale * y, scale * z, scale * w);
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
	public Vector4f add(Vector4f v, Vector4f result) {
		return add(v.x, v.y, v.z, v.w, result);
	}

	/**
	 * As add(v, result) where result is the calling vector.
	 * 
	 * @param v
	 * @return This vector
	 */
	public Vector4f add(Vector4f v) {
		return add(v, this);
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
		return result.set(this.x + x, this.y + y, this.z + z, this.w + w);
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
	public Vector4f sub(Vector4f v, Vector4f result) {
		return sub(v.x, v.y, v.z, v.w, result);
	}

	/**
	 * As sub(v, result) where result is the calling vector.
	 * 
	 * @param v
	 * @return This vector
	 */
	public Vector4f sub(Vector4f v) {
		return sub(v, this);
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
		return result.set(this.x - x, this.y - y, this.z - z, this.w - w);
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
	public Vector4f scaleAdd(float scalar, Vector4f add, Vector4f result) {
		if (result == null)
			result = new Vector4f();
		return result.set(scalar * x + add.x, scalar * y + add.y, scalar * z + add.z, scalar * w + add.w);
	}

	/**
	 * As scaleAdd(scalar, add, result) where result is this vector.
	 * 
	 * @param scalar
	 * @param add
	 * @return This vector
	 */
	public Vector4f scaleAdd(float scalar, Vector4f add) {
		return scaleAdd(scalar, add, this);
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
		return result.set(scalar * x, scalar * y, scalar * z, scalar * w);
	}

	/**
	 * As scale(scalar, result) where result is the calling vector.
	 * 
	 * @param scalar
	 * @return This vector
	 */
	public Vector4f scale(float scalar) {
		return scale(scalar, this);
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
		return scale(1 / length(), result);
	}

	/**
	 * Normalize this vector in place.
	 * 
	 * @return This vector
	 */
	public Vector4f normalize() {
		return normalize(this);
	}

	@Override
	public Vector4f clone() {
		return new Vector4f(this);
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
			return x;
		case 1:
			return y;
		case 2:
			return z;
		case 3:
			return w;
		default:
			throw new IndexOutOfBoundsException("Index must be in [0, 3]");
		}
	}

	/**
	 * Store the three component values of this vector into vals, starting at
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
		vals[offset] = x;
		vals[offset + 1] = y;
		vals[offset + 2] = z;
		vals[offset + 3] = w;
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
	public Vector4f set(Vector4f v) {
		return set(v.x, v.y, v.z, v.w);
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
	 * @throws ArrayIndexOutOfBoundsException if vals doesn't have four values
	 *             starting at offset
	 */
	public Vector4f set(float[] vals, int offset) {
		return set(vals[offset], vals[offset + 1], vals[offset + 2], vals[offset + 3]);
	}

	@Override
	public String toString() {
		return "[" + x + ", " + y + ", " + z + ", " + w + "]";
	}

	@Override
	public int hashCode() {
		int result = 17;

		result += 31 * result + Float.floatToIntBits(x);
		result += 31 * result + Float.floatToIntBits(y);
		result += 31 * result + Float.floatToIntBits(z);
		result += 31 * result + Float.floatToIntBits(w);

		return result;
	}

	@Override
	public boolean equals(Object v) {
		// this conditional correctly handles null values
		if (!(v instanceof Vector4f))
			return false;
		else
			return equals((Vector4f) v);
	}

	/**
	 * Return true if these two vectors are numerically equal. Returns false if
	 * v is null.
	 * 
	 * @param v Vector to test equality with
	 * @return True if these vectors are numerically equal
	 */
	public boolean equals(Vector4f v) {
		return v != null && x == v.x && y == v.y && z == v.z && w == v.w;
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
	public boolean epsilonEquals(Vector4f v, float eps) {
		if (v == null)
			return false;

		float tx = Math.abs(x - v.x);
		float ty = Math.abs(y - v.y);
		float tz = Math.abs(z - v.z);
		float tw = Math.abs(w - v.w);

		return tx <= eps && ty <= eps && tz <= eps && tw <= eps;
	}
}
