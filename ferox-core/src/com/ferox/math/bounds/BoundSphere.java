package com.ferox.math.bounds;

import com.ferox.math.Frustum;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.Frustum.FrustumIntersection;

/**
 * A BoundSphere is a location and a radius enclosing everything within radius
 * of the center location.
 * 
 * @author Michael Ludwig
 */
public class BoundSphere extends AbstractBoundVolume {
	private static final float minRadius = .00001f;

	private float radius;
	private final Vector3f center;
	private int lastFailedPlane;

	/** Create a sphere with 1 radius at the origin. */
	public BoundSphere() {
		super();
		radius = 1;
		center = new Vector3f();
		lastFailedPlane = -1;
	}

	/**
	 * Create a sphere with the given radius at the origin.
	 * 
	 * @param radius Radius of the sphere
	 */
	public BoundSphere(float radius) {
		this();
		setRadius(radius);
	}

	/**
	 * Create a sphere with the given center and radius.
	 * 
	 * @param center Center location of the sphere
	 * @param radius Radius of the sphere
	 */
	public BoundSphere(Vector3f center, float radius) {
		this();
		this.setCenter(center);
		setRadius(radius);
	}

	/**
	 * Create a sphere with the given center and radius.
	 * 
	 * @param x X coordinate of the center
	 * @param y Y coordinate of the center
	 * @param z Z coordinate of the center
	 * @param radius Radius of the sphere
	 */
	public BoundSphere(float x, float y, float z, float radius) {
		this();
		this.setCenter(x, y, z);
		setRadius(radius);
	}
	
	/**
	 * Create a sphere that encloses the given set of vertices. The vertices array
	 * is assumed to have each vertex as three consecutive values within the
	 * array, starting at index 0 being the x coordinate of the first vertex.
	 * 
	 * @param vertices Set of vertices for the BoundSphere to enclose
	 * @throws IllegalArgumentException if vertices.length isn't a multiple of
	 *             3, or if its length < 3
	 * @throws NullPointerException if vertices is null
	 */
	public BoundSphere(float[] vertices) {
		this();
		BoundSphereUtil.getBounds(vertices, this);
	}

	/**
	 * Set the sphere's radius.
	 * 
	 * @param r New radius of the sphere, clamped to be above .00001
	 */
	public void setRadius(float r) {
		radius = Math.max(minRadius, r);
	}

	/**
	 * Copy the vector into the center location.
	 * 
	 * @param center New center location, null = origin
	 */
	public void setCenter(Vector3f center) {
		if (center == null)
			this.center.set(0f, 0f, 0f);
		else
			this.center.set(center);
	}

	/**
	 * Set the center location of the sphere.
	 * 
	 * @param x X coordinate of the center
	 * @param y Y coordinate of the center
	 * @param z Z coordinate of the center
	 */
	public void setCenter(float x, float y, float z) {
		center.set(x, y, z);
	}

	/**
	 * Get the center location of the sphere. Modifications will affect the
	 * BoundSphere's actual location.
	 * 
	 * @return Center vector
	 */
	public Vector3f getCenter() {
		return center;
	}

	/**
	 * Get the radius of the sphere.
	 * 
	 * @return Radius of the sphere
	 */
	public float getRadius() {
		return radius;
	}

	@Override
	public BoundVolume clone(BoundVolume result) {
		if (result == null || !(result instanceof BoundSphere))
			result = new BoundSphere();
		
		BoundSphere s = (BoundSphere) result;
		s.radius = radius;
		s.center.set(center);

		return s;
	}

	@Override
	public void transform(Transform trans) {
		if (trans == null)
			return;
		trans.transform(center);
		float s = trans.getScale();
		radius *= s;
	}

	@Override
	public void enclose(BoundVolume child) {
		if (child == null)
			return;
		if (child instanceof BoundSphere)
			mergeSphere((BoundSphere) child);
		else if (child instanceof AxisAlignedBox)
			mergeAABB((AxisAlignedBox) child);
	}

	private void mergeAABB(AxisAlignedBox aabb) {
		// tempA is used in merge()
		Vector3f center = BoundSphere.tempB.get();
		Vector3f extents = BoundSphere.tempC.get();

		aabb.getCenter(center);
		aabb.getMax().sub(aabb.getMin(), extents);
		merge(center, extents.length() / 2f);
	}

	private void mergeSphere(BoundSphere sphere) {
		merge(sphere.center, sphere.radius);
	}

	private void merge(Vector3f center, float radius) {
		Vector3f diff = center.sub(this.center, BoundSphere.tempA.get());
		float dist = diff.length();

		if (dist != 0f) {
			if (radius > this.radius + dist) {
				// this sphere is inside other sphere
				this.radius = radius;
				this.center.set(center);
			} else if (dist + radius > this.radius) {
				// other sphere is at least partially outside of us
				float or = this.radius;
				this.radius = (dist + radius + this.radius) / 2f;
				diff.scaleAdd((this.radius - or) / dist, this.center, this.center);
			} // else we already enclose it, so do nothing
		} else
			// don't need to move the center, just take the largest radius
			this.radius = Math.max(radius, this.radius);
	}

	@Override
	public FrustumIntersection testFrustum(Frustum frustum, PlaneState planeState) {
		if (frustum == null)
			throw new NullPointerException("Cannot test a null frustum");

		FrustumIntersection result = FrustumIntersection.INSIDE;
		float dist;
		int plane = 0;

		for (int i = Frustum.NUM_PLANES; i >= 0; i--) {
			if (i == lastFailedPlane || (i == Frustum.NUM_PLANES && lastFailedPlane < 0))
				continue;

			if (i == Frustum.NUM_PLANES)
				plane = lastFailedPlane;
			else
				plane = i;

			if (planeState == null || planeState.isTestRequired(plane)) {
				dist = frustum.getFrustumPlane(plane).signedDistance(center);

				if (dist < -radius) {
					lastFailedPlane = plane;
					return FrustumIntersection.OUTSIDE;
				} else if (dist < radius) {
					result = FrustumIntersection.INTERSECT;
				} else {
					if (planeState != null)
						planeState.setTestRequired(plane, false);
				}
			}
		}

		return result;
	}

	@Override
	public Vector3f getExtent(Vector3f dir, boolean reverse, Vector3f result) {
		if (dir == null)
			throw new NullPointerException("Can't compute extent for a null direction");

		if (reverse)
			return dir.scaleAdd(-radius, center, result);
		else
			return dir.scaleAdd(radius, center, result);
	}

	/** When intersecting an AxisAlignedBox, it calls other.intersects(this). */
	@Override
	public boolean intersects(BoundVolume other) {
		if (other == null)
			return false;

		if (other instanceof AxisAlignedBox)
			return other.intersects(this);
		else if (other instanceof BoundSphere) {
			Vector3f cross = BoundSphere.tempA.get();

			BoundSphere s = (BoundSphere) other;
			center.sub(s.center, cross);
			return cross.lengthSquared() <= (radius + s.radius) * (radius + s.radius);
		} else
			throw new UnsupportedOperationException("Unable to compute intersection between the given type: " + other);
	}

	@Override
	public String toString() {
		return "(BoundSphere center: " + center + " radius: " + radius + ")";
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BoundSphere))
			return false;
		BoundSphere that = (BoundSphere) o;
		return that.center.equals(center) && that.radius == radius;
	}
	
	@Override
	public int hashCode() {
		return center.hashCode() ^ (Float.floatToIntBits(radius) * 37);
	}

	// used in enclose
	private static final ThreadLocal<Vector3f> tempA = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tempB = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tempC = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
}
