package com.ferox.math;

import org.openmali.vecmath.Vector3f;

import com.ferox.renderer.View;
import com.ferox.renderer.View.FrustumIntersection;

/**
 * A BoundSphere is a location and a radius enclosing everything within radius
 * of the center location.
 * 
 * @author Michael Ludwig
 * 
 */
public class BoundSphere extends AbstractBoundVolume {
	private static final float radiusEpsilon = 1.00001f;

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
	 * Set the sphere's radius.
	 * 
	 * @param r New radius of the sphere, clamped to be above .00001
	 */
	public void setRadius(float r) {
		radius = Math.max(radiusEpsilon - 1f, r);
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
		if (result instanceof BoundSphere) {
			BoundSphere s = (BoundSphere) result;
			s.radius = radius;
			s.center.set(center);

			return s;
		} else if (result instanceof AxisAlignedBox) {
			AxisAlignedBox b = (AxisAlignedBox) result;
			b.setMin(center.x - radius, center.y - radius, center.z - radius);
			b.setMax(center.x + radius, center.y + radius, center.z + radius);

			return b;
		} else
			return this.clone(new BoundSphere());
	}

	@Override
	public void applyTransform(Transform trans) {
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
		extents.sub(aabb.getMax(), aabb.getMin());
		merge(center, extents.length() / 2f);
	}

	private void mergeSphere(BoundSphere sphere) {
		merge(sphere.center, sphere.radius);
	}

	private void merge(Vector3f center, float radius) {
		Vector3f diff = BoundSphere.tempA.get();

		diff.sub(center, this.center);
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
				this.center.scaleAdd((this.radius - or) / dist, diff,
						this.center);
			} // else we already enclose it, so do nothing
		} else
			// don't need to move the center, just take the largest radius
			this.radius = Math.max(radius, this.radius);
	}

	/**
	 * Converts the sphere into camera space of the view and then uses the
	 * view's sphere classifier to quickly test for frustum intersection.
	 */
	@Override
	public FrustumIntersection testFrustum(View view) {
		if (view == null)
			throw new NullPointerException(
					"Cannot test a frustum with a null view");

		FrustumIntersection result = FrustumIntersection.INSIDE;
		int planeState = view.getPlaneState();
		float dist;
		int plane = 0;

		for (int i = View.NUM_PLANES; i >= 0; i--) {
			if (i == lastFailedPlane
					|| (i == View.NUM_PLANES && lastFailedPlane < 0))
				continue;

			if (i == View.NUM_PLANES)
				plane = lastFailedPlane;
			else
				plane = i;

			if ((planeState & (1 << plane)) == 0) {
				dist = view.getWorldPlane(plane).signedDistance(center);

				if (dist < -radius) {
					view.setPlaneState(planeState);
					lastFailedPlane = plane;
					return FrustumIntersection.OUTSIDE;
				} else if (dist < radius)
					result = FrustumIntersection.INTERSECT;
				else
					planeState |= (1 << plane);
			}
		}

		view.setPlaneState(planeState);
		return result;
	}

	@Override
	public Vector3f getExtent(Vector3f dir, boolean reverse, Vector3f result) {
		if (dir == null)
			throw new NullPointerException(
					"Can't compute extent for a null direction");
		if (result == null)
			result = new Vector3f();
		result.set(center);

		if (reverse) {
			result.x -= dir.x * radius;
			result.y -= dir.y * radius;
			result.z -= dir.z * radius;
		} else {
			result.x += dir.x * radius;
			result.y += dir.y * radius;
			result.z += dir.z * radius;
		}

		return result;
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
			cross.sub(center, s.center);
			return cross.lengthSquared() <= (radius + s.radius)
					* (radius + s.radius);
		} else
			throw new UnsupportedOperationException(
					"Unable to compute intersection between the given type: "
							+ other);
	}

	@Override
	public String toString() {
		return "(BoundSphere center: " + center + " radius: " + radius + ")";
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
