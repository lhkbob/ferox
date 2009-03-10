package com.ferox.math;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Vector3f;

import com.ferox.renderer.View;
import com.ferox.renderer.View.FrustumIntersection;

/**
 * An AxisAlignedBox is a BoundVolume that represents a rectangular box that stays aligned to the
 * xyz axis of world space.
 * 
 * @author Michael Ludwig
 *
 */
public class AxisAlignedBox extends AbstractBoundVolume {
	private static final ThreadLocal<Vector3f> c = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> n = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Matrix3f> m = new ThreadLocal<Matrix3f>() {
		protected Matrix3f initialValue() {
			return new Matrix3f();
		}
	};
	
	private final Vector3f worldMax;
	private final Vector3f worldMin;
	private int lastFailedPlane;
	
	/** Create a cube with sides of 1. */
	public AxisAlignedBox() {
		super();
		this.worldMax = new Vector3f(.5f, .5f, .5f);
		this.worldMin = new Vector3f(-.5f, -.5f, -.5f);
		this.lastFailedPlane = -1;
	}
	
	/** Create a box with copies of the given min and max vectors. */
	public AxisAlignedBox(Vector3f min, Vector3f max) {
		this(min.x, min.y, min.z, max.x, max.y, max.z);
	}
	
	/** Create a box with the given half extents, which represents a vector of the dimensions. */
	public AxisAlignedBox(Vector3f halfExtents) {
		this(halfExtents.x, halfExtents.y, halfExtents.z);
	}
	
	/** Create a box with the given min and max values. */
	public AxisAlignedBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		this();
		this.setMin(minX, minY, minZ);
		this.setMax(maxX, maxY, maxZ);
	}
	
	/** Create a box with the given dimensions, which is centered at the origin. */
	public AxisAlignedBox(float width, float height, float depth) {
		this();
		this.setMin(-width / 2f, -height / 2f, -depth / 2f);
		this.setMax(width / 2f, height / 2f, depth / 2f);
	}
	
	/** Copy the given vector into the max value of the aa box. 
	 * If m is null, uses <0, 0, 0> */
	public void setMax(Vector3f m) {
		if (m == null)
			this.worldMax.set(0f, 0f, 0f);
		else
			this.worldMax.set(m);
	}
	
	/** Copy the given vector into the min value of the aa box. 
	 * If m is null, uses <0, 0, 0> */
	public void setMin(Vector3f m) { 
		if (m == null)
			this.worldMin.set(0f, 0f, 0f);
		else
			this.worldMin.set(m);
	}
	
	/** Set the max value of this aa box. */
	public void setMax(float x, float y, float z) {
		this.worldMax.set(x, y, z);
	}
	
	/** Set the min value of this aa box. */
	public void setMin(float x, float y, float z) {
		this.worldMin.set(x, y, z);
	}
	
	/** Get the max vector for this aa box. Any changes to this vector
	 * will be reflected in the AA_Box.  It should not be modified so
	 * that any components are less than the respective values in getMin(). */
	public Vector3f getMax() {
		return this.worldMax;
	}
	
	/** Get the min vector for this aa box. Any changes to this vector
	 * will be reflected in the AA_Box.  It should not be modified so
	 * that any components are greater than the respective values in getMin(). */
	public Vector3f getMin() {
		return this.worldMin;
	}
	
	/** Compute the center of this aa box and store the result in out.  If out is null, create a new vector. Returns out. */
	public Vector3f getCenter(Vector3f out) {
		if (out == null)
			out = new Vector3f();
		out.add(this.worldMin, this.worldMax);
		out.scale(.5f);
		return out;
	}
	
	@Override
	public void applyTransform(Transform trans) {
		if (trans == null)
			return;
		
		float s = trans.getScale();
		Vector3f t = trans.getTranslation();
		this.worldMax.set(s * this.worldMax.x, s * this.worldMax.y, s * this.worldMax.z);
		this.worldMin.set(s * this.worldMin.x, s * this.worldMin.y, s * this.worldMin.z);
		
		Vector3f c = AxisAlignedBox.c.get();
		Matrix3f m = AxisAlignedBox.m.get();
		
		this.getCenter(c);
		this.worldMin.sub(c); 
		this.worldMax.sub(c);
		
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
		
		m.transform(this.worldMin);
		m.transform(this.worldMax);
		
		b.transform(c);
		this.worldMin.add(c);
		this.worldMax.add(c);
		
		this.worldMin.add(t);
		this.worldMax.add(t);
	}

	@Override
	public BoundVolume clone(BoundVolume result) {
		if (result == null || !(result instanceof AxisAlignedBox))
			result = new AxisAlignedBox();
		AxisAlignedBox b = (AxisAlignedBox)result;
		b.worldMax.set(this.worldMax);
		b.worldMin.set(this.worldMin);
		return b;
	}

	@Override
	public void enclose(BoundVolume child) throws UnsupportedOperationException {
		if (child == null)
			return;
		if (child instanceof AxisAlignedBox)
			this.mergeAABB((AxisAlignedBox)child);
		else if (child instanceof BoundSphere)
			this.mergeSphere((BoundSphere)child);
		else
			throw new UnsupportedOperationException("Unabled to merge given bound volume: " + child);
	}
	
	private void mergeAABB(AxisAlignedBox aabb) {
		this.worldMax.x = Math.max(this.worldMax.x, aabb.worldMax.x);
		this.worldMax.y = Math.max(this.worldMax.y, aabb.worldMax.y);
		this.worldMax.z = Math.max(this.worldMax.z, aabb.worldMax.z);
		
		this.worldMin.x = Math.min(this.worldMin.x, aabb.worldMin.x);
		this.worldMin.y = Math.min(this.worldMin.y, aabb.worldMin.y);
		this.worldMin.z = Math.min(this.worldMin.z, aabb.worldMin.z);
	}
	
	private void mergeSphere(BoundSphere sphere) {
		Vector3f c = sphere.getCenter();
		float r = sphere.getRadius();
		this.worldMax.x = Math.max(this.worldMax.x, c.x + r);
		this.worldMax.y = Math.max(this.worldMax.y, c.y + r);
		this.worldMax.z = Math.max(this.worldMax.z, c.z + r);
		
		this.worldMin.x = Math.min(this.worldMin.x, c.x - r);
		this.worldMin.y = Math.min(this.worldMin.y, c.y - r);
		this.worldMin.z = Math.min(this.worldMin.z, c.z - r);
	}

	private void getMinWorldVertices(Vector3f normal, Vector3f min) {
		if (normal.x > 0) {
			if (normal.y > 0) {
				if (normal.z > 0) {
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				} else {
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				}
			} else {
				if (normal.z > 0) {
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				} else {
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				}
			}
		} else {
			if (normal.y > 0) {
				if (normal.z > 0) {
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				} else {
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				}
			} else {
				if (normal.z > 0) {
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				} else {
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				}
			}
		}
	}
	
	private void getMaxWorldVertices(Vector3f normal, Vector3f max) {
		if (normal.x > 0) {
			if (normal.y > 0) {
				if (normal.z > 0) {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
				} else {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
				}
			} else {
				if (normal.z > 0) {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
				} else {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
				}
			}
		} else {
			if (normal.y > 0) {
				if (normal.z > 0) {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
				} else {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
				}
			} else {
				if (normal.z > 0) {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
				} else {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
				}
			}
		}
	}
	
	@Override
	public FrustumIntersection testFrustum(View view) throws NullPointerException {
		if (view == null)
			throw new NullPointerException("Cannot test a frustum with a null view");
		
		FrustumIntersection result = FrustumIntersection.INSIDE;
		int planeState = view.getPlaneState();
		float distMax;
		float distMin;
		int plane = 0;
		
		Vector3f n = AxisAlignedBox.n.get();
		Vector3f c = AxisAlignedBox.c.get();
		
		Plane p;
		for (int i = View.NUM_PLANES; i >= 0; i--) {
			if (i == this.lastFailedPlane || (i == View.NUM_PLANES && this.lastFailedPlane < 0))
				continue;
	
			if (i == View.NUM_PLANES) 
				plane = this.lastFailedPlane;
			else 
				plane = i;

			if ((planeState & (1 << plane)) == 0) {
				p = view.getWorldPlane(plane);
				n.set(p.getA(), p.getB(), p.getC());
				this.getExtent(n, false, c);
				distMax = p.signedDistance(c);
				this.getExtent(n, true, c);
				distMin = p.signedDistance(c);

				if (distMax < 0) {
					view.setPlaneState(planeState);
					this.lastFailedPlane = plane;
					return FrustumIntersection.OUTSIDE;
				} else if (distMin < 0)
					result = FrustumIntersection.INTERSECT;
				else
					planeState |= (1 << plane);
			}
		}
		
		view.setPlaneState(planeState);
		return result;
	}

	@Override
	public Vector3f getExtent(Vector3f dir, boolean reverse, Vector3f out) throws NullPointerException {
		if (dir == null)
			throw new NullPointerException("Can't find extent along a null direction vector");
		if (out == null)
			out = new Vector3f();
		
		if (reverse)
			this.getMinWorldVertices(dir, out);
		else
			this.getMaxWorldVertices(dir, out);
		
		return out;
	}

	/** Currently can handle intersections between AxisAlignedBoxes and BoundSpheres. */
	@Override
	public boolean intersects(BoundVolume other) throws UnsupportedOperationException {
		if (other == null)
			return false;
		
		if (other instanceof AxisAlignedBox) {
			AxisAlignedBox a = (AxisAlignedBox)other;
			return ((a.worldMax.x >= this.worldMin.x && a.worldMax.x <= this.worldMax.x) || (a.worldMin.x >= this.worldMin.x && a.worldMin.x <= this.worldMax.x)) &&
				   ((a.worldMax.y >= this.worldMin.y && a.worldMax.y <= this.worldMax.y) || (a.worldMin.y >= this.worldMin.y && a.worldMin.y <= this.worldMax.y)) &&
				   ((a.worldMax.z >= this.worldMin.z && a.worldMax.z <= this.worldMax.z) || (a.worldMin.z >= this.worldMin.z && a.worldMin.z <= this.worldMax.z));
		} else if (other instanceof BoundSphere) {
			Vector3f c = AxisAlignedBox.c.get();
			BoundSphere s = (BoundSphere)other;
			this.getCenter(c);
			c.sub(s.getCenter());
			this.getExtent(c, true, c);
			c.sub(s.getCenter());
			return c.lengthSquared() <= s.getRadius() * s.getRadius();
		} else
			throw new UnsupportedOperationException("Unable to compute intersection for type: " + other);
	}
	
	@Override
	public void enclose(Boundable vertices) {
		if (vertices == null)
			return;
		int vertexCount = vertices.getVertexCount();
		if (vertexCount == 0)
			return;

		this.worldMax.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		this.worldMin.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		
		float fourth, invFourth;
		for (int i = 0; i < vertexCount; i++) {
			fourth = vertices.getVertex(i, 3);
			if (Math.abs(1f - fourth) < .0001f) {
				this.enclosePoint(vertices.getVertex(i, 0), vertices.getVertex(i, 1), vertices.getVertex(i, 2));
			} else {
				invFourth = 1 / fourth;
				this.enclosePoint(vertices.getVertex(i, 0) * invFourth, vertices.getVertex(i, 1) * invFourth, vertices.getVertex(i, 2) * invFourth);
			}
		}
	}
	
	private void enclosePoint(float x, float y, float z) {
		this.worldMax.x = Math.max(this.worldMax.x, x);
		this.worldMax.y = Math.max(this.worldMax.y, y);
		this.worldMax.z = Math.max(this.worldMax.z, z);
		
		this.worldMin.x = Math.min(this.worldMin.x, x);
		this.worldMin.y = Math.min(this.worldMin.y, y);
		this.worldMin.z = Math.min(this.worldMin.z, z);
	}
}
