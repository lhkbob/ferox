package com.ferox.math;

/**
 * <p>
 * Describes a translation, rotation, and scale that effectively create a 4x4
 * matrix transform from one basis to another (e.g. a given Transform is the
 * transform from its identity space to standard space (or parent space if in a
 * hierarchy)).
 * </p>
 * <p>
 * Whenever mentioned in this class, the rotation matrix must be an orthogonal
 * matrix representing a rotation, there should be no skewing or scaling
 * incorporated within it (scaling is handled elsewhere).
 * </p>
 * <p>
 * Only uniform scaling is supported (things are scaled along all 3 axis
 * equally).
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Transform {
	private final Matrix3f rot;
	private final Vector3f trans;
	private float scale;

	/** Create an identity transform. */
	public Transform() {
		rot = new Matrix3f();
		rot.setIdentity();
		trans = new Vector3f();
		scale = 1f;
	}

	/**
	 * Create a transform with no rotation, uniform scale, and the given
	 * translation.
	 * 
	 * @param trans The translation vector to use
	 */
	public Transform(Vector3f trans) {
		this();
		this.setTranslation(trans);
	}

	/**
	 * Create a transform with no rotation and the given translation and scale.
	 * 
	 * @param trans The translation vector to use
	 * @param scale Uniform scale to use
	 * @throws IllegalArgumentException if scale < .0001
	 */
	public Transform(Vector3f trans, float scale) {
		this();
		setTranslation(trans);
		setScale(scale);
	}

	/**
	 * Create a transform with uniform scale and the given translation and
	 * rotation.
	 * 
	 * @param trans The translation vector to use
	 * @param rot The 3x3 matrix representing the orientation
	 */
	public Transform(Vector3f trans, Matrix3f rot) {
		this();
		setTranslation(trans);
		setRotation(rot);
	}

	/**
	 * Create a transform with the given translation, rotation and scale.
	 * 
	 * @param trans The translation vector to use
	 * @param rot The 3x3 matrix representing the orientation
	 * @param scale Uniform scale to use
	 * @throws IllegalArgumentException if scale < .0001
	 */
	public Transform(Vector3f trans, Matrix3f rot, float scale) {
		this();
		setTranslation(trans);
		setScale(scale);
		setRotation(rot);
	}

	/**
	 * Get the vector instance storing the translation. Any changes to this will
	 * be visible in the Transform.
	 * 
	 * @return Translation vector
	 */
	public Vector3f getTranslation() {
		return trans;
	}

	/**
	 * Get the scale factor for this transform. Defaults to 1.
	 * 
	 * @return Uniform scaling
	 */
	public float getScale() {
		return scale;
	}

	/**
	 * Get the matrix instnance storing the rotation of this transform. Any
	 * changes will be visible in the transform. It should only ever be set to
	 * an orthogonal rotation matrix.
	 * 
	 * @return Rotation matrix
	 */
	public Matrix3f getRotation() {
		return rot;
	}

	/**
	 * Copies the vector as the translation component of this Transform.
	 * 
	 * @param t New translation vector, if null uses <0, 0, 0>
	 */
	public void setTranslation(Vector3f t) {
		if (t == null)
			trans.set(0f, 0f, 0f);
		else
			trans.set(t);
	}

	/**
	 * Sets this transform's translation vector
	 * 
	 * @param x New x translation
	 * @param y New y translation
	 * @param z New z translation
	 */
	public void setTranslation(float x, float y, float z) {
		trans.set(x, y, z);
	}

	/**
	 * Sets this transform's scale factor.
	 * 
	 * @param scale The uniform scaling
	 * @throws IllegalArgumentException if scale < .0001
	 */
	public void setScale(float scale) {
		if (scale < .0001f)
			throw new IllegalArgumentException(
				"Can't set a scale smaller than .0001: " + scale);
		this.scale = scale;
	}

	/**
	 * Copies the matrix as the rotation of this Transform.
	 * 
	 * @param rot The rotation matrix to copy, null implies the identity matrix
	 */
	public void setRotation(Matrix3f rot) {
		if (rot == null)
			this.rot.setIdentity();
		else
			this.rot.set(rot);
	}

	/**
	 * Compute the distance between this Transform's translation and the other's
	 * translation.
	 * 
	 * @param other The Transform to compute distance between
	 * @return The distance between other and this's translation vectors
	 * @throws NullPointerException if other is null
	 */
	public float distance(Transform other) {
		return (float) Math.sqrt(distanceSquared(other));
	}

	/**
	 * Compute the square of the distance between this Transform's translation
	 * and the other's translation.
	 * 
	 * @param other The Transform to compute distance between
	 * @return The distance squared between other and this's translation vectors
	 * @throws NullPointerException if other is null
	 */
	public float distanceSquared(Transform other) {
		float dx = trans.x - other.trans.x;
		float dy = trans.y - other.trans.y;
		float dz = trans.z - other.trans.z;

		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Stores this X t1 into result (order is as in conventional matrix
	 * math). It is safe to call with result as this transform.
	 * 
	 * @param t1 Right-hand transform in multiplication
	 * @param t2 Result of the multiplication
	 * @return result, or a new Transform if null
	 * @throws NullPointerException if t1 is null
	 */
	public Transform mul(Transform t1, Transform result) {
		if (result == null)
			result = new Transform();
		
		Vector3f t = temp.get();
		transform(t1.trans, t);
		result.trans.set(t);
		result.scale = scale * t1.scale;
		rot.mul(t1.rot, result.rot);
		return result;
	}

	/** Calls this.inverse(this) */
	public void inverse() {
		this.inverse(this);
	}

	/**
	 * Stores the inverse of this transform into result. Safe to call with result = this.
	 * 
	 * @param result Transform to store the inverse
	 * @return result, or a new Transform if null
	 */
	public Transform inverse(Transform result) {
		return inverseMul(IDENTITY.get(), result);
	}

	/**
	 * Stores this^-1 X tn into result. Safe to call with result = tn or this.
	 * If result is null, a new Transform is created.
	 * 
	 * @param tn Right-hand transform in multiplication
	 * @param result Result of the multiplication
	 * @return result, or a new Transform if null
	 * @throws NullPointerException if tn is null
	 */
	public Transform inverseMul(Transform tn, Transform result) {
		if (result == null)
			result = new Transform();
		
		Vector3f t = temp.get();
		inverseTransform(tn.trans, t);
		result.trans.set(t);
		result.scale = tn.scale / this.scale;
		rot.mulTransposeLeft(tn.rot, result.rot);
		return result;
	}

	/**
	 * Calls transform(t, t), but doesn't return anything.
	 * 
	 * @param t Vector to be transformed by this Transform
	 * @throws NullPointerException if t is null
	 * @throws IllegalArgumentException if t is this Transform's translation
	 *             vector
	 */
	public void transform(Vector3f t) {
		this.transform(t, t);
	}

	/**
	 * Transforms the vector t by this transform (e.g. [this] X t, t as a column
	 * matrix, 4th component is a 1). Stores result into the result vector,
	 * leaving t unchanged unless result = t. <br>
	 * If result = null, create a new instance to store the result.
	 * 
	 * @param t Vector to be transformed
	 * @param result Vector to store transformed t
	 * @return Return transformed vector, result or new Vector3f if result was
	 *         null
	 * @throws NullPointerException if t was null
	 * @throws IllegalArgumentException if t is this Transform's translation
	 *             vector
	 */
	public Vector3f transform(Vector3f t, Vector3f result) {
		if (t == null)
			throw new NullPointerException("Can't transform a null vector");
		if (result == trans)
			throw new IllegalArgumentException(
				"Can't use this transform's vectors as a result");
		if (result == null)
			result = new Vector3f();

		return rot.mul(t.scale(scale, result), result).add(trans, result);
	}

	/**
	 * Calls this.inverseTransform(t, t), but doesn't return the result.
	 * 
	 * @param t Vector to be transformed by the inverse of this
	 * @throws NullPointerException if t is null
	 * @throws IllegalArgumentException if t is this Transform's translation
	 *             vector
	 */
	public void inverseTransform(Vector3f t) {
		this.inverseTransform(t, t);
	}

	/**
	 * Transforms the vector t by the inverse of this transform (e.g. [this]^-1
	 * X t, t as a column matrix, 4th row = 1). Stores result into the result
	 * vector, t is unchanged unless result = t. <br>
	 * If result = null, creates a new instance to store the result.
	 * 
	 * @param t Vector to be transformed by the inverse of this Transform
	 * @param result Vector to store transformed t
	 * @return Return transformed vector, result or new Vector3f if result was
	 *         null
	 * @throws NullPointerException if t was null
	 * @throws IllegalArgumentException if t is this Transform's translation
	 *             vector
	 */
	public Vector3f inverseTransform(Vector3f t, Vector3f result) {
		if (t == null)
			throw new NullPointerException("Can't transform a null vector");
		if (result == trans)
			throw new IllegalArgumentException(
				"Can't use this transform's vectors as a result");
		if (result == null)
			result = new Vector3f();

		return rot.mulPre(t.sub(trans, result), result).scale(1f / scale, result);
	}

	/**
	 * Copies the values of t into this transform.
	 * 
	 * @param t Transform to copy into this transform, null = identity
	 */
	public void set(Transform t) {
		if (t == null)
			setIdentity();
		else {
			rot.set(t.rot);
			scale = t.scale;
			trans.set(t.trans);
		}
	}

	/** Sets this transform to the identity. */
	public void setIdentity() {
		rot.setIdentity();
		scale = 1f;
		trans.set(0f, 0f, 0f);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Transform))
			return false;
		Transform that = (Transform) other;
		return trans.equals(that.trans) && scale == that.scale
			&& rot.equals(that.rot);
	}

	private static final ThreadLocal<Transform> IDENTITY =
		new ThreadLocal<Transform>() {
			@Override
			protected Transform initialValue() {
				return new Transform();
			}
		};
	private static final ThreadLocal<Vector3f> temp =
		new ThreadLocal<Vector3f>() {
			@Override
			protected Vector3f initialValue() {
				return new Vector3f();
			}
		};
}