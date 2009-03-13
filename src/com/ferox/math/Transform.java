package com.ferox.math;

import org.openmali.vecmath.AxisAngle4f;
import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Quat4f;
import org.openmali.vecmath.Vector3f;

/**
 * Describes a translation, rotation, and scale that effectively create a 4x4 matrix transform
 * from one basis to another (e.g. a given Transform is the transform from its identity space to
 * standard space (or parent space if in a hierarchy)).
 * 
 * Whenever mentioned in this class, the rotation matrix must be an orthogonal matrix representing a 
 * rotation, there should be no skewing or scaling incorporated within it (scaling is handled elsewhere).
 * 
 * Only uniform scaling is supported (things are scaled along all 3 axis equally).
 * 
 * @author Michael Ludwig
 *
 */
public class Transform {
	private static final ThreadLocal<Transform> IDENTITY = new ThreadLocal<Transform>() {
		protected Transform initialValue() {
			return new Transform();
		}
	};
	private static final ThreadLocal<Vector3f> temp = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	
	private final Matrix3f rot;
	private final Vector3f trans;
	private float scale;
	
	/** Create an identity transform */
	public Transform() {
		this.rot = new Matrix3f();
		this.rot.setIdentity();
		this.trans = new Vector3f();
		this.scale = 1f;
	}
	
	/** Create a transform with no rotation, uniform scale, and the given translation */
	public Transform(Vector3f trans) {
		this();
		this.setTranslation(trans);
	}
	
	/** Create a transform with no rotation and the given translation and scale */
	public Transform(Vector3f trans, float scale) {
		this();
		this.setTranslation(trans);
		this.setScale(scale);
	}
	
	/** Create a transform with uniform scale and the given translation and rotation */
	public Transform(Vector3f trans, Matrix3f rot) {
		this();
		this.setTranslation(trans);
		this.setRotation(rot);
	}
	
	/** Create a transform with the given translation, rotation and scale */
	public Transform(Vector3f trans, Matrix3f rot, float scale) {
		this();
		this.setTranslation(trans);
		this.setScale(scale);
		this.setRotation(rot);
	}
	
	/** Get the vector instance storing the translation.  Any changes to this will
	 * be visible in the Transform. */
	public Vector3f getTranslation() {
		return this.trans;
	}
	
	/** Get the scale factor for this transform.  Defaults to 1. */
	public float getScale() {
		return this.scale;
	}
	
	/** Get the matrix instnance storing the rotation of this transform.  Any
	 * changes will be visible in the transform.  It should only ever be
	 * set to an orthogonal rotation matrix. */
	public Matrix3f getRotation() {
		return this.rot;
	}
	
	/** Copies the vector as the translation component of this Transform.
	 * If null, the translation is set to the origin. */
	public void setTranslation(Vector3f t) {
		if (t == null)
			this.trans.set(0f, 0f, 0f);
		else
			this.trans.set(t);
	}
	
	/** Sets this translation vector */
	public void setTranslation(float x, float y, float z) {
		this.trans.set(x, y, z);
	}
	
	/** Sets this scale factor.  Fails if scale is less than .0001. */
	public void setScale(float scale) throws IllegalArgumentException {
		if (scale < .0001f)
			throw new IllegalArgumentException("Can't set a scale smaller than .0001: " + scale);
		this.scale = scale;
	}
	
	/** Copies the matrix as the rotation of this Transform.  If null,
	 * the rotation is set to the identity matrix */
	public void setRotation(Matrix3f rot) {
		if (rot == null)
			this.rot.setIdentity();
		else
			this.rot.set(rot);
	}
	
	/** Copies the quaternion as the rotation of this Transform.  If null,
	 * the rotation is set to the identity matrix */
	public void setRotation(Quat4f rot) {
		if (rot == null)
			this.rot.setIdentity();
		else
			this.rot.set(rot);
	}
	
	/** Copies the axis angle as the rotation of this Transform.  If null,
	 * the rotation is set to the identity matrix */
	public void setRotation(AxisAngle4f rot) {
		if (rot == null)
			this.rot.setIdentity();
		else
			this.rot.set(rot);
	}
	
	/** Copy the rotation into rot and return rot.  If rot is null, it creates a new instance */
	public Quat4f getRotation(Quat4f rot) {
		if (rot == null)
			rot = new Quat4f();
		rot.set(this.rot);
		return rot;
	}
	
	/** Copy the rotation into rot and return rot.  If rot is null, it creates a new instance */
	public AxisAngle4f getRotation(AxisAngle4f rot) {
		if (rot == null)
			rot = new AxisAngle4f();
		rot.set(this.rot);
		return rot;
	}
	
	/** Stores t1 X t2 into this transform (order is as in conventional matrix math).  It is
	 * safe to call with t1 or t2 as this transform. Returns this transform.  Fails
	 * if either t1 or t2 are null. */
	public Transform mul(Transform t1, Transform t2) throws NullPointerException {
		if (t1 == null || t2 == null)
			throw new NullPointerException("Can't multiply null transforms");
		Vector3f t = temp.get();
		t1.transform(t2.trans, t);
		this.trans.set(t);
		this.scale = t1.scale * t2.scale;
		this.rot.mul(t1.rot, t2.rot);
		return this;
	}
	
	/** Calls this.inverse(this) */
	public void inverse() {
		this.inverse(this);
	}
	
	/** Stores the inverse of t into this Transform.  Safe to call with t = this. 
	 * Returns this transform, fails if t is null. */
	public Transform inverse(Transform t) throws NullPointerException {
		if (t == null)
			throw new NullPointerException("Can't inverse a null transform");
		return this.inverseMul(t, IDENTITY.get());
	}
	
	/** Stores ti^-1 X tn into this transform.  Safe to call with ti or tn = this. 
	 * Fails if ti or tn are null.  Returns this transform. */
	public Transform inverseMul(Transform ti, Transform tn) throws NullPointerException {
		if (ti == null || tn == null)
			throw new NullPointerException("Can't inverse multiply null transforms");
		Vector3f t = temp.get();
		ti.inverseTransform(tn.trans, t);
		this.trans.set(t);
		this.scale = tn.scale / ti.scale;
		this.rot.mulTransposeLeft(ti.rot, tn.rot);
		return this;
	}
	
	/** Calls transform(t, t), but doesn't return anything */
	public void transform(Vector3f t) {
		this.transform(t, t);
	}
	
	/** Transforms the vector t by this transform (e.g. [this] X t, t as a column matrix, 4th component is a 1).
	 * Stores result into the result vector, leaving t unchanged unless result = t.  If result = null, create a new
	 * instance to store the result. 
	 * 
	 * Fails if t is null or if result is the translation vector of this transform. */
	public Vector3f transform(Vector3f t, Vector3f result) {
		if (t == null)
			throw new NullPointerException("Can't transform a null vector");
		if (result == this.trans)
			throw new IllegalArgumentException("Can't use this transform's vectors as a result");
		if (result == null)
			result = new Vector3f();
		
		result.scale(this.scale, t);
		this.rot.transform(result);
		result.add(this.trans);
		
		return result;
	}
	
	/** Calls this.inverseTransform(t, t), but doesn't return the result */
	public void inverseTransform(Vector3f t) {
		this.inverseTransform(t, t);
	}
	
	/** Transforms the vector t by the inverse of this transform (e.g. [this]^-1 X t, t as a column matrix, 4th row = 1).
	 * Stores result into the result vector, t is unchanged unless result = t.  If result = null, creates a new instance
	 * to store the result. 
	 * 
	 * Fails if t is null or if result is the translation vector of this transform. */
	public Vector3f inverseTransform(Vector3f t, Vector3f result) throws NullPointerException, IllegalArgumentException {
		if (t == null)
			throw new NullPointerException("Can't transform a null vector");
		if (result == this.trans)
			throw new IllegalArgumentException("Can't use this transform's vectors as a result");
		if (result == null)
			result = new Vector3f();
		
		result.sub(t, this.trans);
		result.set(this.rot.m00 * result.x + this.rot.m10 * result.y + this.rot.m20 * result.z, 
				   this.rot.m01 * result.x + this.rot.m11 * result.y + this.rot.m21 * result.z,
				   this.rot.m02 * result.x + this.rot.m12 * result.y + this.rot.m22 * result.z);
		result.scale(1f / this.scale);
			
		return result;
	}
	
	/** Copies the values of t into this transform */
	public void set(Transform t) {
		if (t == null)
			this.setIdentity();
		else {
			this.rot.set(t.rot);
			this.scale = t.scale;
			this.trans.set(t.trans);
		}
	}
	
	/** Sets this transform to the identity */
	public void setIdentity() {
		this.rot.setIdentity();
		this.scale = 1f;
		this.trans.set(0f, 0f, 0f);
	}
	
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Transform))
			return false;
		Transform that = (Transform)other;
		return this.trans.equals(that.trans) && this.scale == that.scale && this.rot.equals(that.rot);
	}
}