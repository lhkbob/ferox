package com.ferox.math;

import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector3f;



/**
 * Represents a 3D plane <a,b,c,d> that satisfies (a*x + b*y + c*z + d = 0)
 * <a, b, c> represents the normal of the plane.
 * 
 * @author Michael Ludwig
 *
 */
public class Plane {
	private static final ThreadLocal<Vector3f> temp = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> norm = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	
	private float a, b, c, d;
	private float len;
	
	/** Creates a plane with the given values */
	public Plane(float a, float b, float c, float d) {
		this.setPlane(a, b, c, d);
	}

	/** Store the normal vector in result.  If result is null, a new Vector3f is created. */
	public Vector3f getNormal(Vector3f result) {
		if (result == null)
			result = new Vector3f();
		result.set(this.a, this.b, this.c);
		return result;
	}

	/** Sets <a, b, c> based on the given vector.  normal can't be null. */
	public void setNormal(Vector3f normal) throws NullPointerException {
		if (normal == null)
			throw new NullPointerException("Normal vector can't be null");
		this.setPlane(normal.x, normal.y, normal.z, this.d);
	}
	
	/** Set all four values for this plane. |<a, b, c>| != 0. */
	public void setPlane(float a, float b, float c, float d) throws ArithmeticException {
		float dist = (float)Math.sqrt(a * a + b * b + c * c);
		if (dist == 0)
			throw new ArithmeticException("Invalid plane input: " + a + " " + b + " " + c + " " + d + ", causes normal vector to be of length 0");
		
		this.len = dist;
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}
	
	/** Store the 4 components of this plane into the given array. 
	 * Fails if the array is null or doesn't have length 4. */
	public void get(float[] store) throws IllegalArgumentException {
		if (store == null || store.length != 4)
			throw new IllegalArgumentException("Array must be of length 4 and not null");
		
		store[0] = this.a;
		store[1] = this.b;
		store[2] = this.c;
		store[3] = this.d;
	}
	
	/** Return true if the 4 values of the array match the ones in this plane. */
	public boolean equals(float[] plane) {
		if (plane == null || plane.length != 4)
			return false;
		return this.a == plane[0] && this.b == plane[1] && this.c == plane[2] && this.d == plane[3];
	}
	
	/** Get the a value for this plane. */
	public float getA() {
		return this.a;
	}

	/** Get the b value for this plane. */
	public float getB() {
		return this.b;
	}

	/** Get the c value for this plane. */
	public float getC() {
		return this.c;
	}

	/** Get the d value for this plane. */
	public float getD() {
		return this.d;
	}

	/** Compute the signed distance between the given position vector and 
	 * the plane.  0 implies on the plane, greater than 0 means in front of (in direction
	 * of the normal), less than 0 means in opposite direction of the normal. */
	public float signedDistance(Vector3f v) {
		float num = this.a * v.x + this.b * v.y + this.c * v.z + this.d;
		if (this.len == 1f)
			return num;
		else
			return num / this.len;
	}
	
	/** Math.abs(signedDistance(v)). */
	public float distance(Vector3f v) {
		return Math.abs(this.signedDistance(v));
	}
	
	/** Whether or not the given vector is on the plane or in the direction of the 
	 * plane's normal (ie signedDistance(v) >= 0). */
	public boolean inFrontOfPlane(Vector3f v) {
		return this.signedDistance(v) >= 0;
	}
	
	/** Transforms this plane in place by trans. */
	public void transform(Transform trans) {
		this.transform(this, trans);
	}
	
	/** Transforms the plane by trans and stores it in this plane. Fails if p or trans are null.
	 * Returns this plane. */
	public Plane transform(Plane p, Transform trans) throws NullPointerException {
		if (trans == null || p == null)
			throw new NullPointerException("Can't have null input: " + trans + " " + p);
			
		Vector3f temp = Plane.temp.get();
		Vector3f norm = Plane.norm.get();
		
		if (p.c != 0)
			temp.set(0f, 0f, -p.d / p.c);
		else if (p.b != 0)
			temp.set(0f, -p.d / p.b, 0f);
		else
			temp.set(-p.d / p.a, 0f, 0f);
		
		norm.set(p.a, p.b, p.c);
		trans.transform(temp);
		trans.getRotation().transform(norm);
		
		this.setPlane(norm.x, norm.y, norm.z, -temp.dot(norm));

		return this;
	}
	
	/** Normalize this plane. */
	public void normalize() {
		float inverseD = 1f / this.len;
		this.a = inverseD * this.a;
		this.b = inverseD * this.b;
		this.c = inverseD * this.c;
		this.d = inverseD * this.d;
		this.len = 1f;
	}
	
	/** Transforms this plane in place by trans. */
	public void transformLocal(Matrix4f trans) {
		this.transform(this, trans);
	}
	
	/** Transforms the plane by trans and stores it in this plane. Fails if p or trans are null.
	 * Returns this plane. */
	public Plane transform(Plane p, Matrix4f trans) throws NullPointerException {
		if (trans == null || p == null)
			throw new NullPointerException("Can't have null input: " + trans + " " + p);
				
		Vector3f temp = Plane.temp.get();
		Vector3f norm = Plane.norm.get();
		
		if (p.c != 0)
			temp.set(0f, 0f, -p.d / p.c);
		else if (p.b != 0)
			temp.set(0f, -p.d / p.b, 0f);
		else
			temp.set(-p.d / p.a, 0f, 0f);
		
		norm.set(p.a, p.b, p.c);
		trans.transform(temp);
		temp.set(temp.x + trans.m03, temp.y + trans.m13, temp.z + trans.m23);
		trans.transform(norm);
		
		this.setPlane(norm.x, norm.y, norm.z, -temp.dot(norm));
		
		return this;
	}
	
	@Override
	public String toString() {
		return this.a + " " + this.b + " " + this.c + " " + this.d;
	}
}
