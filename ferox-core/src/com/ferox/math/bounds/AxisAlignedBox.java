package com.ferox.math.bounds;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix3f;
import com.ferox.math.Plane;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.Frustum.FrustumIntersection;

/**
 * An AxisAlignedBox is a BoundVolume that represents a rectangular box that
 * stays aligned to the xyz axis of world space.
 * 
 * @author Michael Ludwig
 */
public class AxisAlignedBox extends AbstractBoundVolume {
	private final Vector3f worldMax;
	private final Vector3f worldMin;
	private int lastFailedPlane;

	/** Create a cube with sides of 1. */
	public AxisAlignedBox() {
		super();
		worldMax = new Vector3f(.5f, .5f, .5f);
		worldMin = new Vector3f(-.5f, -.5f, -.5f);
		lastFailedPlane = -1;
	}

	/**
	 * Create a box with copies of the given min and max vectors.
	 * 
	 * @param min Location of "minimum" coordinate of the box
	 * @param max Location of "maximum" coordinate of the box
	 */
	public AxisAlignedBox(Vector3f min, Vector3f max) {
		this(min.x, min.y, min.z, max.x, max.y, max.z);
	}

	/**
	 * Create a box with the given dimensions, which represents a vector of the
	 * dimensions.
	 * 
	 * @param dimensions Dimensions of the box, (width, height, depth)
	 */
	public AxisAlignedBox(Vector3f dimensions) {
		this(dimensions.x, dimensions.y, dimensions.z);
	}

	/**
	 * Create a box with the given min and max values.
	 * 
	 * @param minX Minimum x coordinate of the box
	 * @param minY Minimum y coordinate of the box
	 * @param minZ Minimum z coordinate of the box
	 * @param maxX Maximum x coordinate of the box
	 * @param maxY Maximum y coordinate of the box
	 * @param maxZ Maximum z coordinate of the box
	 */
	public AxisAlignedBox(float minX, float minY, float minZ, 
						  float maxX, float maxY, float maxZ) {
		this();
		this.setMin(minX, minY, minZ);
		this.setMax(maxX, maxY, maxZ);
	}

	/**
	 * Create a box with the given dimensions, which is centered at the origin.
	 * 
	 * @param width Width of the box, size along the x axis
	 * @param height Height of the box, size along the y axis
	 * @param depth Depth of the box, size along the z axis
	 */
	public AxisAlignedBox(float width, float height, float depth) {
		this();
		this.setMin(-width / 2f, -height / 2f, -depth / 2f);
		this.setMax(width / 2f, height / 2f, depth / 2f);
	}

	/**
	 * Create a box that encloses the given set of vertices. The vertices array
	 * is assumed to have each vertex as three consecutive values within the
	 * array, starting at index 0 being the x coordinate of the first vertex.
	 * 
	 * @param vertices Set of vertices for the AxisAlignedBox to enclose
	 * @throws IllegalArgumentException if vertices.length isn't a multiple of
	 *             3, or if its length < 3
	 * @throws NullPointerException if vertices is null
	 */
	public AxisAlignedBox(float[] vertices) {
		this();
		if (vertices == null)
			throw new NullPointerException("Vertices cannot be null");
		if (vertices.length % 3 != 0 || vertices.length < 3)
			throw new IllegalArgumentException("Vertices length must be a multiple of 3, and at least 3: " + vertices.length);
		
		int vertexCount = vertices.length / 3;
		for (int i = 0; i < vertexCount; i++)
			enclosePoint(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2]); 
	}
	
	/**
	 * Copy the given vector into the max value of the aa box.
	 * 
	 * @param m Coordinate of the max-corner, null = <0, 0, 0>
	 */
	public void setMax(Vector3f m) {
		if (m == null)
			worldMax.set(0f, 0f, 0f);
		else
			worldMax.set(m);
	}

	/**
	 * Copy the given vector into the min value of the aa box.
	 * 
	 * @param m Coordinate of the min-corner, null = <0, 0, 0>
	 */
	public void setMin(Vector3f m) {
		if (m == null)
			worldMin.set(0f, 0f, 0f);
		else
			worldMin.set(m);
	}

	/**
	 * Set the max value of this aa box.
	 * 
	 * @see #setMax(Vector3f)
	 */
	public void setMax(float x, float y, float z) {
		worldMax.set(x, y, z);
	}

	/**
	 * Set the min value of this aa box.
	 * 
	 * @see #setMin(Vector3f)
	 */
	public void setMin(float x, float y, float z) {
		worldMin.set(x, y, z);
	}

	/**
	 * Get the max vector for this aa box. Any changes to this vector will be
	 * reflected in the AA_Box. It should not be modified so that any components
	 * are less than the respective values in getMin().
	 * 
	 * @return Max of the aabb along xyz axis
	 */
	public Vector3f getMax() {
		return worldMax;
	}

	/**
	 * Get the min vector for this aa box. Any changes to this vector will be
	 * reflected in the AA_Box. It should not be modified so that any components
	 * are greater than the respective values in getMin().
	 * 
	 * @return Min of the aabb along xyz axis
	 */
	public Vector3f getMin() {
		return worldMin;
	}

	/**
	 * Compute the center of this aa box and store the result in out. If out is
	 * null, create a new vector. Returns out.
	 * 
	 * @param out Storage for the center location
	 * @return Center vector, out or new vector if out is null
	 */
	public Vector3f getCenter(Vector3f out) {
		out = worldMin.add(worldMax, out);
		return out.scale(.5f, out);
	}

	@Override
	public void transform(Transform trans) {
		if (trans == null)
			return;

		float s = trans.getScale();
		Vector3f t = trans.getTranslation();
		worldMax.scale(s);
		worldMin.scale(s);

		Vector3f c = AxisAlignedBox.c.get();
		Matrix3f m = AxisAlignedBox.m.get();

		getCenter(c);
		worldMin.sub(c);
		worldMax.sub(c);

		Matrix3f b = trans.getRotation();
		m.m00 = Math.abs(b.m00);
		m.m01 = Math.abs(b.m01);
		m.m02 = Math.abs(b.m02);
		m.m10 = Math.abs(b.m10);
		m.m11 = Math.abs(b.m11);
		m.m12 = Math.abs(b.m12);
		m.m20 = Math.abs(b.m20);
		m.m21 = Math.abs(b.m21);
		m.m22 = Math.abs(b.m22);

		m.mul(worldMin);
		m.mul(worldMax);

		b.mul(c, c);
		worldMin.add(c).add(t);
		worldMax.add(c).add(t);
	}

	@Override
	public BoundVolume clone(BoundVolume result) {
		if (result == null || !(result instanceof AxisAlignedBox))
			result = new AxisAlignedBox();
		
		AxisAlignedBox b = (AxisAlignedBox) result;
		b.worldMax.set(worldMax);
		b.worldMin.set(worldMin);
		return b;
	}

	@Override
	public void enclose(BoundVolume child) {
		if (child == null)
			return;
		if (child instanceof AxisAlignedBox)
			mergeAABB((AxisAlignedBox) child);
		else if (child instanceof BoundSphere)
			mergeSphere((BoundSphere) child);
		else
			throw new UnsupportedOperationException("Unabled to merge given bound volume: " + child);
	}

	@Override
	public FrustumIntersection testFrustum(Frustum frustum, PlaneState planeState) {
		if (frustum == null)
			throw new NullPointerException("Cannot test a null frustum");

		FrustumIntersection result = FrustumIntersection.INSIDE;
		float distMax;
		float distMin;
		int plane = 0;

		Vector3f n = AxisAlignedBox.n.get();
		Vector3f c = AxisAlignedBox.c.get();

		Plane p;
		for (int i = Frustum.NUM_PLANES; i >= 0; i--) {
			if (i == lastFailedPlane || (i == Frustum.NUM_PLANES && lastFailedPlane < 0))
				continue;

			if (i == Frustum.NUM_PLANES)
				plane = lastFailedPlane;
			else
				plane = i;

			if (planeState == null || planeState.isTestRequired(plane)) {
				p = frustum.getFrustumPlane(plane);
				n.set(p.getA(), p.getB(), p.getC());
				getExtent(n, false, c);
				distMax = p.signedDistance(c);
				getExtent(n, true, c);
				distMin = p.signedDistance(c);

				if (distMax < 0) {
					lastFailedPlane = plane;
					return FrustumIntersection.OUTSIDE;
				} else if (distMin < 0) {
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
	public Vector3f getExtent(Vector3f dir, boolean reverse, Vector3f out) {
		if (dir == null)
			throw new NullPointerException("Can't find extent along a null direction vector");
		if (out == null)
			out = new Vector3f();

		if (reverse)
			getMinWorldVertices(dir, out);
		else
			getMaxWorldVertices(dir, out);

		return out;
	}

	/**
	 * Currently can handle intersections between AxisAlignedBoxes and
	 * BoundSpheres.
	 */
	@Override
	public boolean intersects(BoundVolume other) {
		if (other == null)
			return false;

		if (other instanceof AxisAlignedBox) {
			AxisAlignedBox a = (AxisAlignedBox) other;
			return ((a.worldMax.x >= worldMin.x && a.worldMax.x <= worldMax.x) 
					|| (a.worldMin.x >= worldMin.x && a.worldMin.x <= worldMax.x)) && 
				   ((a.worldMax.y >= worldMin.y && a.worldMax.y <= worldMax.y) 
					|| (a.worldMin.y >= worldMin.y && a.worldMin.y <= worldMax.y)) && 
				   ((a.worldMax.z >= worldMin.z && a.worldMax.z <= worldMax.z) 
					|| (a.worldMin.z >= worldMin.z && a.worldMin.z <= worldMax.z));
		} else if (other instanceof BoundSphere) {
			// Idea taken from "Simple Intersection Tests for Games" by Miguel
			// Gomez - Gamasutra
			BoundSphere s = (BoundSphere) other;
			Vector3f sphereCenter = s.getCenter();
			float totalDistance = 0;

			float borderDistance;
			// x (or i == 0)
			if (sphereCenter.x < worldMin.x) {
				borderDistance = worldMin.x - sphereCenter.x;
				totalDistance += borderDistance * borderDistance;
			} else if (sphereCenter.x > worldMax.x) {
				borderDistance = sphereCenter.x - worldMax.x;
				totalDistance += borderDistance * borderDistance;
			}

			// y (or i == 1)
			if (sphereCenter.y < worldMin.y) {
				borderDistance = worldMin.y - sphereCenter.y;
				totalDistance += borderDistance * borderDistance;
			} else if (sphereCenter.y > worldMax.y) {
				borderDistance = sphereCenter.y - worldMax.y;
				totalDistance += borderDistance * borderDistance;
			}

			// z (or i == 2)
			if (sphereCenter.z < worldMin.z) {
				borderDistance = worldMin.z - sphereCenter.z;
				totalDistance += borderDistance * borderDistance;
			} else if (sphereCenter.z > worldMax.z) {
				borderDistance = sphereCenter.z - worldMax.z;
				totalDistance += borderDistance * borderDistance;
			}

			return totalDistance <= s.getRadius() * s.getRadius();
		} else
			throw new UnsupportedOperationException("Unable to compute intersection for type: " + other);
	}

	@Override
	public String toString() {
		return "(AxisAlignedBox min: " + worldMin + " max: " + worldMax + ")";
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AxisAlignedBox))
			return false;
		AxisAlignedBox that = (AxisAlignedBox) o;
		return that.worldMax.equals(worldMax) && that.worldMin.equals(worldMin);
	}
	
	@Override
	public int hashCode() {
		return worldMax.hashCode() ^ worldMin.hashCode();
	}

	private void mergeAABB(AxisAlignedBox aabb) {
		worldMax.x = Math.max(worldMax.x, aabb.worldMax.x);
		worldMax.y = Math.max(worldMax.y, aabb.worldMax.y);
		worldMax.z = Math.max(worldMax.z, aabb.worldMax.z);

		worldMin.x = Math.min(worldMin.x, aabb.worldMin.x);
		worldMin.y = Math.min(worldMin.y, aabb.worldMin.y);
		worldMin.z = Math.min(worldMin.z, aabb.worldMin.z);
	}

	private void mergeSphere(BoundSphere sphere) {
		Vector3f c = sphere.getCenter();
		float r = sphere.getRadius();
		worldMax.x = Math.max(worldMax.x, c.x + r);
		worldMax.y = Math.max(worldMax.y, c.y + r);
		worldMax.z = Math.max(worldMax.z, c.z + r);

		worldMin.x = Math.min(worldMin.x, c.x - r);
		worldMin.y = Math.min(worldMin.y, c.y - r);
		worldMin.z = Math.min(worldMin.z, c.z - r);
	}

	private void getMinWorldVertices(Vector3f normal, Vector3f min) {
		if (normal.x > 0) {
			if (normal.y > 0) {
				if (normal.z > 0) {
					min.x = worldMin.x;
					min.y = worldMin.y;
					min.z = worldMin.z;
				} else {
					min.x = worldMin.x;
					min.y = worldMin.y;
					min.z = worldMax.z;
				}
			} else {
				if (normal.z > 0) {
					min.x = worldMin.x;
					min.y = worldMax.y;
					min.z = worldMin.z;
				} else {
					min.x = worldMin.x;
					min.y = worldMax.y;
					min.z = worldMax.z;
				}
			}
		} else {
			if (normal.y > 0) {
				if (normal.z > 0) {
					min.x = worldMax.x;
					min.y = worldMin.y;
					min.z = worldMin.z;
				} else {
					min.x = worldMax.x;
					min.y = worldMin.y;
					min.z = worldMax.z;
				}
			} else {
				if (normal.z > 0) {
					min.x = worldMax.x;
					min.y = worldMax.y;
					min.z = worldMin.z;
				} else {
					min.x = worldMax.x;
					min.y = worldMax.y;
					min.z = worldMax.z;
				}
			}
		}
	}

	private void getMaxWorldVertices(Vector3f normal, Vector3f max) {
		if (normal.x > 0) {
			if (normal.y > 0) {
				if (normal.z > 0) {
					max.x = worldMax.x;
					max.y = worldMax.y;
					max.z = worldMax.z;
				} else {
					max.x = worldMax.x;
					max.y = worldMax.y;
					max.z = worldMin.z;
				}
			} else {
				if (normal.z > 0) {
					max.x = worldMax.x;
					max.y = worldMin.y;
					max.z = worldMax.z;
				} else {
					max.x = worldMax.x;
					max.y = worldMin.y;
					max.z = worldMin.z;
				}
			}
		} else {
			if (normal.y > 0) {
				if (normal.z > 0) {
					max.x = worldMin.x;
					max.y = worldMax.y;
					max.z = worldMax.z;
				} else {
					max.x = worldMin.x;
					max.y = worldMax.y;
					max.z = worldMin.z;
				}
			} else {
				if (normal.z > 0) {
					max.x = worldMin.x;
					max.y = worldMin.y;
					max.z = worldMax.z;
				} else {
					max.x = worldMin.x;
					max.y = worldMin.y;
					max.z = worldMin.z;
				}
			}
		}
	}
	
	private void enclosePoint(float x, float y, float z) {
		worldMax.x = Math.max(worldMax.x, x);
		worldMax.y = Math.max(worldMax.y, y);
		worldMax.z = Math.max(worldMax.z, z);

		worldMin.x = Math.min(worldMin.x, x);
		worldMin.y = Math.min(worldMin.y, y);
		worldMin.z = Math.min(worldMin.z, z);
	}
	
	private static final ThreadLocal<Vector3f> c = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> n = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Matrix3f> m = new ThreadLocal<Matrix3f>() {
		@Override
		protected Matrix3f initialValue() {
			return new Matrix3f();
		}
	};
}
