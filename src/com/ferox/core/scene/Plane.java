package com.ferox.core.scene;

import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector3f;

import com.ferox.core.util.FeroxException;



/**
 * Represents a 3D plane that satisifies Normal.dot(v) = const.
 * @author Michael Ludwig
 *
 */
public class Plane {
	private static final Vector3f temp = new Vector3f();
	
	private Vector3f normal;
	private float constant;
	
	/**
	 * Same as Plane(new Vector3f(0f, 0f, 1f)).
	 */
	public Plane() {
		this(new Vector3f(0f, 0f, 1f));
	}
	
	/**
	 * Same as Plane(normal, 0);
	 */
	public Plane(Vector3f normal) {
		this(normal, 0f);
	}
	
	/**
	 * Creates a plane that mathematically represents a plane satisfying
	 * normal.dot(v) = constant.
	 */
	public Plane(Vector3f normal, float constant) {
		this.setNormal(normal);
		this.setConstant(constant);
	}

	/**
	 * Get the normal vector for this plane.
	 */
	public Vector3f getNormal() {
		return this.normal;
	}

	/**
	 * Set the normal vector reference for this plane.
	 */
	public void setNormal(Vector3f normal) {
		if (normal == null)
			normal = new Vector3f(0f, 0f, 1f);
		this.normal = normal;
	}
	
	public void setPlane(float p1, float p2, float p3, float p4) {
		if (this.normal == null)
			this.normal = new Vector3f();
		this.normal.set(p1, p2, p3);
		this.constant = p4;
	}

	/**
	 * Get the constant term for this plane.
	 */
	public float getConstant() {
		return this.constant;
	}

	/**
	 * Set the constant term for this plane.
	 */
	public void setConstant(float constant) {
		this.constant = constant;
	}
	
	/**
	 * Compute the signed distance between the given vector and 
	 * the plane.  0 implies on the plane, > 0 means in front of (in direction
	 * of the normal), < 0 means in opposite direction of the normal.
	 */
	public float signedDistance(Vector3f v) {
		return this.normal.dot(v) + this.constant;
	}
	
	/**
	 * Math.abs(signedDistance(v))
	 */
	public float distance(Vector3f v) {
		return Math.abs(this.signedDistance(v));
	}
	
	/**
	 * Whether or not the given vector is on the plane or in the direction of the 
	 * plane's normal (ie signedDistance(v) >= 0).
	 */
	public boolean inFrontOfPlane(Vector3f v) {
		return this.signedDistance(v) >= 0;
	}
	
	/**
	 * Transforms this plane by the given Transform and stores the result in place.
	 */
	public void transformLocal(Transform trans) {
		this.transform(trans, this);
	}
	
	/**
	 * Transforms this plane by Transform and stores the result in the given plane.
	 */
	public Plane transform(Transform trans, Plane result) throws NullPointerException, FeroxException {
		if (trans == null)
			throw new NullPointerException("Can't transform a plane by a null transform");
		if (result == null)
			result = new Plane();
		
		if (this.normal.z != 0)
			temp.set(0f, 0f, -this.constant / this.normal.z);
		else if (this.normal.y != 0)
			temp.set(0f, -this.constant / this.normal.y, 0f);
		else if (this.normal.x != 0)
			temp.set(-this.constant / this.normal.x, 0f, 0f);
		else
			throw new FeroxException("Illegal normal vector for a plane");
		trans.transform(temp);
		trans.getRotation().transform(this.normal, result.normal);
		result.constant = -temp.dot(result.normal);
		
		return result;
	}
	
	public void normalize() {
		float d = this.normal.length();
		this.normal.scale(1f / d);
		this.constant /= d;
	}
	
	/**
	 * Transforms this plane by the given matrix and stores the result in place.
	 */
	public void transformLocal(Matrix4f trans) {
		this.transform(trans, this);
	}
	
	/**
	 * Transforms this plane by Matrix and stores the result in the given plane.
	 */
	public Plane transform(Matrix4f trans, Plane result) throws NullPointerException, FeroxException {
		if (trans == null)
			throw new NullPointerException("Can't transform a plane by a null matrix");
		if (result == null)
			result = new Plane();
		
		if (this.normal.z != 0)
			temp.set(0f, 0f, -this.constant / this.normal.z);
		else if (this.normal.y != 0)
			temp.set(0f, -this.constant / this.normal.y, 0f);
		else if (this.normal.x != 0)
			temp.set(-this.constant / this.normal.x, 0f, 0f);
		else
			throw new FeroxException("Illegal normal vector for a plane");
		
		trans.transform(temp);
		temp.set(temp.x + trans.m03, temp.y + trans.m13, temp.z + trans.m23);
		trans.transform(this.normal, result.normal);
		result.constant = -temp.dot(result.normal);
		
		return result;
	}
	
	@Override
	public String toString() {
		return "[ " + this.constant + " ], " + this.normal;
	}
}
