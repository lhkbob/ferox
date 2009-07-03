package com.ferox.math;

/**
 * Represents a 3D plane <a,b,c,d> that satisfies (a*x + b*y + c*z + d = 0),
 * where <a, b, c> represents the normal of the plane.
 * 
 * @author Michael Ludwig
 */
public class Plane {
	private static final ThreadLocal<Vector3f> temp = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> norm = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};

	private float a, b, c, d;
	private float len;

	/**
	 * Creates a plane with the given values.
	 * 
	 * @param a a value in the plane equation
	 * @param b b value in the plane equation
	 * @param c c value in the plane equation
	 * @param d d value in the plane equation
	 */
	public Plane(float a, float b, float c, float d) {
		setPlane(a, b, c, d);
	}

	/**
	 * Store the normal vector in result. If result is null, a new Vector3f is
	 * created.
	 * 
	 * @param result Storage for the normal vector <a,b,c>
	 * @return Normal of this Plane, result or a new vector if result was null
	 */
	public Vector3f getNormal(Vector3f result) {
		if (result == null)
			result = new Vector3f();
		result.set(a, b, c);
		return result;
	}

	/**
	 * Sets <a, b, c> based on the given vector.
	 * 
	 * @param normal The new normal vector
	 * @throws NullPointerException if normal is null
	 * @throws ArithmeticException if setPlane() fails
	 */
	public void setNormal(Vector3f normal) {
		if (normal == null)
			throw new NullPointerException("Normal vector can't be null");
		setPlane(normal.x, normal.y, normal.z, d);
	}

	/**
	 * Set all four values for this plane.
	 * 
	 * @param a a value in the plane equation
	 * @param b b value in the plane equation
	 * @param c c value in the plane equation
	 * @param d d value in the plane equation
	 * @throws ArithmeticException if |<a, b, c>| == 0.
	 */
	public void setPlane(float a, float b, float c, float d) {
		float dist = (float) Math.sqrt(a * a + b * b + c * c);
		if (dist == 0)
			throw new ArithmeticException("Invalid plane input: " + a + " " + b + " " + c  + " " + 
										  d + ", causes normal vector to be of length 0");

		len = dist;
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	/**
	 * Get the a value for this plane.
	 * 
	 * @return The a value for the plane
	 */
	public float getA() {
		return a;
	}

	/**
	 * Get the b value for this plane.
	 * 
	 * @return The b value for the plane
	 */
	public float getB() {
		return b;
	}

	/**
	 * Get the c value for this plane.
	 * 
	 * @return The c value for the plane
	 */
	public float getC() {
		return c;
	}

	/**
	 * Get the d value for this plane.
	 * 
	 * @return The d value for the plane
	 */
	public float getD() {
		return d;
	}

	/**
	 * Compute the signed distance between the given position vector and the
	 * plane. 0 implies on the plane, greater than 0 means in front of (in
	 * direction of the normal), less than 0 means in opposite direction of the
	 * normal.
	 * 
	 * @param v Position to compute the distance to
	 * @return Signed distance from the plane to v
	 * @throws NullPointerException if v is null
	 */
	public float signedDistance(Vector3f v) {
		float num = v.dot(a, b, c) + d;
		if (len == 1f)
			return num;
		else
			return num / len;
	}

	/**
	 * Transforms this plane in place by trans.
	 * 
	 * @param trans The transform to adjust this plane with
	 * @throws NullPointerException if trans is null
	 */
	public void transform(Transform trans) {
		this.transform(this, trans);
	}

	/**
	 * Transforms the plane by trans and stores it in this plane.
	 * 
	 * @param p The transformed plane, can be this
	 * @param trans The Transform to that adjusts p
	 * @return this
	 * @throws NullPointerException if p or trans are null
	 */
	public Plane transform(Plane p, Transform trans) {
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

		trans.transform(temp);
		trans.getRotation().mul(norm.set(p.a, p.b, p.c), norm);

		setPlane(norm.x, norm.y, norm.z, -temp.dot(norm));

		return this;
	}

	/** Normalize this plane. */
	public void normalize() {
		float inverseD = 1f / len;
		a = inverseD * a;
		b = inverseD * b;
		c = inverseD * c;
		d = inverseD * d;
		len = 1f;
	}

	@Override
	public String toString() {
		return "(Plane " + a + ", " + b + ", " + c + ", " + d + ")";
	}
}