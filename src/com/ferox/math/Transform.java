package com.ferox.math;

import org.openmali.vecmath.AxisAngle4f;
import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Quat4f;
import org.openmali.vecmath.Vector3f;

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
	 * Copies the quaternion as the rotation of this Transform.
	 * 
	 * @param rot Quaternion rotation to use, null implies identity
	 */
	public void setRotation(Quat4f rot) {
		if (rot == null)
			this.rot.setIdentity();
		else
			this.rot.set(rot);
	}

	/**
	 * Copies the axis angle as the rotation of this Transform.
	 * 
	 * @param rot Axis angle rotation to use, null implies identity
	 */
	public void setRotation(AxisAngle4f rot) {
		if (rot == null)
			this.rot.setIdentity();
		else
			this.rot.set(rot);
	}

	/**
	 * Copy the rotation into rot and return rot. If rot is null, it creates a
	 * new instance
	 * 
	 * @param rot Storage for this rotation
	 * @return Rotation as a quaternion, rot or a new Quat4f if rot was null
	 */
	public Quat4f getRotation(Quat4f rot) {
		if (rot == null)
			rot = new Quat4f();
		rot.set(this.rot);
		return rot;
	}

	/**
	 * Copy the rotation into rot and return rot. If rot is null, it creates a
	 * new instance
	 * 
	 * @param rot Storage for this rotation
	 * @return Rotation as an axis angle, rot or a new AxisAngle4f if rot was
	 *         null
	 */
	public AxisAngle4f getRotation(AxisAngle4f rot) {
		if (rot == null)
			rot = new AxisAngle4f();
		rot.set(this.rot);
		return rot;
	}

	/**
	 * Stores t1 X t2 into this transform (order is as in conventional matrix
	 * math). It is safe to call with t1 or t2 as this transform.
	 * 
	 * @param t1 Left-hand transform in multiplication
	 * @param t2 Right-hand transform in multiplication
	 * @return This transform
	 * @throws NullPointerException if t1 or t2 are null
	 */
	public Transform mul(Transform t1, Transform t2) {
		if (t1 == null || t2 == null)
			throw new NullPointerException("Can't multiply null transforms");
		Vector3f t = temp.get();
		t1.transform(t2.trans, t);
		trans.set(t);
		scale = t1.scale * t2.scale;
		rot.mul(t1.rot, t2.rot);
		return this;
	}

	/** Calls this.inverse(this) */
	public void inverse() {
		this.inverse(this);
	}

	/**
	 * Stores the inverse of t into this Transform. Safe to call with t = this.
	 * 
	 * @param t Transform to invert
	 * @return This transform
	 * @throws NullPointerException if t is null
	 */
	public Transform inverse(Transform t) {
		if (t == null)
			throw new NullPointerException("Can't inverse a null transform");
		return inverseMul(t, IDENTITY.get());
	}

	/**
	 * Stores ti^-1 X tn into this transform. Safe to call with ti or tn = this.
	 * 
	 * @param ti Inverse is used in left-hand of multiplication
	 * @param tn Right-hand transform in multiplication
	 * @return This transform
	 * @throws NullPointerException if ti or tn are null
	 */
	public Transform inverseMul(Transform ti, Transform tn) {
		if (ti == null || tn == null)
			throw new NullPointerException(
				"Can't inverse multiply null transforms");
		Vector3f t = temp.get();
		ti.inverseTransform(tn.trans, t);
		trans.set(t);
		scale = tn.scale / ti.scale;
		rot.mulTransposeLeft(ti.rot, tn.rot);
		return this;
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

		result.scale(scale, t);
		rot.transform(result);
		result.add(trans);

		return result;
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

		result.sub(t, trans);
		result.set(
			rot.m00 * result.x + rot.m10 * result.y + rot.m20 * result.z,
			rot.m01 * result.x + rot.m11 * result.y + rot.m21 * result.z,
			rot.m02 * result.x + rot.m12 * result.y + rot.m22 * result.z);
		result.scale(1f / scale);

		return result;
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