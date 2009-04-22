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
		this.radius = 1;
		this.center = new Vector3f();
		this.lastFailedPlane = -1;
	}
	
	/** Create a sphere with the given radius at the origin. */
	public BoundSphere(float radius) {
		this();
		this.setRadius(radius);
	}
	
	/** Create a sphere with the given center and radius. */
	public BoundSphere(Vector3f center, float radius) {
		this();
		this.setCenter(center);
		this.setRadius(radius);
	}
	
	/** Create a sphere with the given center and radius. */
	public BoundSphere(float x, float y, float z, float radius) {
		this();
		this.setCenter(x, y, z);
		this.setRadius(radius);
	}
	
	@Override
	public BoundVolume clone(BoundVolume result) {
		if (result instanceof BoundSphere) {
			BoundSphere s = (BoundSphere) result;
			s.radius = this.radius;
			s.center.set(this.center);
			
			return s;
		} else if (result instanceof AxisAlignedBox) {
			AxisAlignedBox b = (AxisAlignedBox) result;
			b.setMin(this.center.x - this.radius, this.center.y - this.radius, this.center.z - this.radius);
			b.setMax(this.center.x + this.radius, this.center.y + this.radius, this.center.z + this.radius);
			
			return b;
		} else
			return this.clone(new BoundSphere());
	}
	
	/** Set the radius, clamps it to be above .00001. */
	public void setRadius(float r) {
		this.radius = Math.max(radiusEpsilon - 1f, r);
	}
	
	/** Copy the vector into the center location.  If center is null, sets it to the origin. */
	public void setCenter(Vector3f center) {
		if (center == null)
			this.center.set(0f, 0f, 0f);
		else
			this.center.set(center);
	}
	
	/** Set the center location of the sphere. */
	public void setCenter(float x, float y, float z) {
		this.center.set(x, y, z);
	}
	
	/** Get the center location of the sphere. */
	public Vector3f getCenter() {
		return this.center;
	}
	
	/** Get the radius of the sphere. */
	public float getRadius() {
		return this.radius;
	}
	
	@Override
	public void applyTransform(Transform trans) {
		if (trans == null)
			return;
		trans.transform(this.center);
		float s = trans.getScale();
		this.radius *= s;
	}

	@Override
	public void enclose(BoundVolume child) {
		if (child == null)
			return;
		if (child instanceof BoundSphere)
			this.mergeSphere((BoundSphere)child);
		else if (child instanceof AxisAlignedBox)
			this.mergeAABB((AxisAlignedBox)child);
	}

	private void mergeAABB(AxisAlignedBox aabb) {
		// tempA is used in merge()
		Vector3f center = BoundSphere.tempB.get();
		Vector3f extents = BoundSphere.tempC.get();
		
		aabb.getCenter(center);
		extents.sub(aabb.getMax(), aabb.getMin());
		this.merge(center, extents.length() / 2f);
	}
	
	private void mergeSphere(BoundSphere sphere) {
		this.merge(sphere.center, sphere.radius);
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
				this.center.scaleAdd((this.radius - or) / dist, diff, this.center);
			} // else we already enclose it, so do nothing
		} else {
			// don't need to move the center, just take the largest radius
			this.radius = Math.max(radius, this.radius);
		}
	}

	/** Converts the sphere into camera space of the view and then uses the view's sphere classifier 
	 * to quickly test for frustum intersection. */
	@Override
	public FrustumIntersection testFrustum(View view) throws NullPointerException {
		if (view == null)
			throw new NullPointerException("Cannot test a frustum with a null view");
		
		FrustumIntersection result = FrustumIntersection.INSIDE;
		int planeState = view.getPlaneState();
		float dist;
		int plane = 0;
		
		for (int i = View.NUM_PLANES; i >= 0; i--) {
			if (i == this.lastFailedPlane || (i == View.NUM_PLANES && this.lastFailedPlane < 0))
				continue;
	
			if (i == View.NUM_PLANES) 
				plane = this.lastFailedPlane;
			else 
				plane = i;

			if ((planeState & (1 << plane)) == 0) {
				dist = view.getWorldPlane(plane).signedDistance(this.center);

				if (dist < -this.radius) {
					view.setPlaneState(planeState);
					this.lastFailedPlane = plane;
					return FrustumIntersection.OUTSIDE;
				} else if (dist < this.radius)
					result = FrustumIntersection.INTERSECT;
				else
					planeState |= (1 << plane);
			}
		}
		
		view.setPlaneState(planeState);
		return result;
	}

	@Override
	public Vector3f getExtent(Vector3f dir, boolean reverse, Vector3f result) throws NullPointerException {
		if (dir == null)
			throw new NullPointerException("Can't compute extent for a null direction");
		if (result == null)
			result = new Vector3f();
		result.set(this.center);

		if (reverse) {
			result.x -= dir.x * this.radius;
			result.y -= dir.y * this.radius;
			result.z -= dir.z * this.radius;
		} else {
			result.x += dir.x * this.radius;
			result.y += dir.y * this.radius;
			result.z += dir.z * this.radius;
		}
		
		return result;
	}

	/** When intersecting an AxisAlignedBox, it calls other.intersects(this). */
	@Override
	public boolean intersects(BoundVolume other) throws UnsupportedOperationException {
		if (other == null)
			return false;
		
		if (other instanceof AxisAlignedBox) {
			return other.intersects(this);
		} else if (other instanceof BoundSphere) {
			Vector3f cross = BoundSphere.tempA.get();
			
			BoundSphere s = (BoundSphere)other;
			cross.sub(this.center, s.center);
			return cross.lengthSquared() <= (this.radius + s.radius) * (this.radius + s.radius);
		} else
			throw new UnsupportedOperationException("Unable to compute intersection between the given type: " + other);
	}
	
	@Override
	public void enclose(Boundable vertices) {
		if (vertices == null || vertices.getVertexCount() == 0)
			return;
		
		float[] points = new float[vertices.getVertexCount() * 3];
		fillPointsArray(points, vertices);
		
		this.recurseMini(points, points.length / 3, 0, 0); // thanks to jME for algorithm, adapted to use float arrays
	}
	
	private void recurseMini(float[] points, int p, int b, int ap) {
		Vector3f tempA = BoundSphere.tempA.get();
		Vector3f tempB = BoundSphere.tempB.get();
		Vector3f tempC = BoundSphere.tempC.get();
		Vector3f tempD = BoundSphere.tempD.get();
		
		 switch (b) {
	        case 0:
	            this.radius = 0;
	            this.center.set(0f, 0f, 0f);
	            break;
	        case 1:
	            this.radius = 1f - radiusEpsilon;
	            populateFromArray(this.center, points, ap - 1);
	            break;
	        case 2:
	            populateFromArray(tempA, points, ap - 1);
	            populateFromArray(tempB, points, ap - 2);
	            this.setSphere(tempA, tempB);
	            break;
	        case 3:
	            populateFromArray(tempA, points, ap - 1);
	            populateFromArray(tempB, points, ap - 2);
	            populateFromArray(tempC, points, ap - 3);
	            this.setSphere(tempA, tempB, tempC);
	            break;
	        case 4:
	            populateFromArray(tempA, points, ap - 1);
	            populateFromArray(tempB, points, ap - 2);
	            populateFromArray(tempC, points, ap - 3);
	            populateFromArray(tempD, points, ap - 4);
	            this.setSphere(tempA, tempB, tempC, tempD);
	            return;
	        }
	        for (int i = 0; i < p; i++) {
	            populateFromArray(tempA, points, i + ap);
	            float d = ((tempA.x - this.center.x) * (tempA.x - this.center.x) + 
		            	   (tempA.y - this.center.y) * (tempA.y - this.center.y) +  
		            	   (tempA.z - this.center.z) * (tempA.z - this.center.z));
	            if (d - (this.radius * this.radius) > radiusEpsilon - 1f) {
	            	for (int j = i; j > 0; j--) {
	                    populateFromArray(tempB, points, j + ap);
	                    populateFromArray(tempC, points, j - 1 + ap);
	                    setInArray(tempC, points, j + ap);
	                    setInArray(tempB, points, j - 1 + ap);
	                }
	                recurseMini(points, i, b + 1, ap + 1);
	            }
	        }
	}
	
	private static void populateFromArray(Vector3f p, float[] points, int index) {
		p.x = points[index * 3];
		p.y = points[index * 3 + 1];
		p.z = points[index * 3 + 2];
	}
	
	private static void setInArray(Vector3f p, float[] points, int index) {
		if (p == null) {
			points[index * 3] = 0f;
			points[index * 3 + 1] = 0f;
			points[index * 3 + 2] = 0f;
		} else {
			points[index * 3] = p.x;
			points[index * 3 + 1] = p.y;
			points[index * 3 + 2] = p.z;
		}
	}
	
	private void setSphere(Vector3f o, Vector3f a, Vector3f b, Vector3f c) {
		Vector3f tA = BoundSphere.tA.get();
		Vector3f tB = BoundSphere.tB.get();
		Vector3f tC = BoundSphere.tC.get();
		Vector3f tD = BoundSphere.tD.get();
		Vector3f cross = BoundSphere.cross.get();
		
		tA.sub(a, o);
		tB.sub(b, o);
		tC.sub(c, o);
		
		float denom = 2.0f * (tA.x * (tB.y * tC.z - tC.y * tB.z) - tB.x * (tA.y * tC.z - tC.y * tA.z) + tC.x * (tA.y * tB.z - tB.y * tA.z));
		if (denom == 0) {
			this.center.set(0f, 0f, 0f);
			this.radius = 0f;
		} else {
			cross.cross(tA, tB); cross.scale(tC.lengthSquared());
			tD.cross(tC, tA); tD.scale(tB.lengthSquared()); cross.add(tD);
			tD.cross(tB, tC); tD.scale(tA.lengthSquared()); cross.add(tD);
			cross.scale(1f / denom);

	        this.radius = cross.length() * radiusEpsilon;
	        this.center.add(o, cross);
		}
	}

	private void setSphere(Vector3f o, Vector3f a, Vector3f b) {
		Vector3f tA = BoundSphere.tA.get();
		Vector3f tB = BoundSphere.tB.get();
		Vector3f tC = BoundSphere.tC.get();
		Vector3f tD = BoundSphere.tD.get();
		Vector3f cross = BoundSphere.cross.get();
		
		tA.sub(a, o);
		tB.sub(b, o);
		cross.cross(tA, tB);
		
		float denom = 2f * cross.lengthSquared();

		if (denom == 0) {
			this.center.set(0f, 0f, 0f);
			this.radius = 0f;
		} else {
			tC.cross(cross, tA); tC.scale(tB.lengthSquared());
			tD.cross(tB, cross); tD.scale(tA.lengthSquared());
			tC.add(tD); tC.scale(1f / denom);

			this.radius = tC.length() * radiusEpsilon;
			this.center.add(o, tC);
		}
	}

	private void setSphere(Vector3f o, Vector3f a) {
		this.radius = (float)Math.sqrt(((a.x - o.x) * (a.x - o.x) + (a.y - o.y) * (a.y - o.y) + (a.z - o.z) * (a.z - o.z)) / 4f) + radiusEpsilon - 1f;
	    
		this.center.scale(.5f, o);
	    this.center.scaleAdd(.5f, a, this.center);
	}

	private static void fillPointsArray(float[] points, Boundable verts) {
		int vertexCount = verts.getVertexCount();
		
		for (int i = 0; i < vertexCount; i++) {
			points[i * 3] = verts.getVertex(i, 0);
			points[i * 3 + 1] = verts.getVertex(i, 1);
			points[i * 3 + 2] = verts.getVertex(i, 2);
		}
	}

	// used in recurseMini and a few other places
	private static final ThreadLocal<Vector3f> tempA = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tempB = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tempC = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tempD = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	
	// used exclusively in setSphere methods
	private static final ThreadLocal<Vector3f> tA = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tB = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tC = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> tD = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	
	// used in setSphere
	private static final ThreadLocal<Vector3f> cross = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
}
