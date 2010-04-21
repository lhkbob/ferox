package com.ferox.math.bounds;

import com.ferox.math.Matrix4f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.math.bounds.Frustum.FrustumIntersection;

public class AxisAlignedBox {
	private final Vector3f min;
	private final Vector3f max;
	private int lastFailedPlane;
	
	public AxisAlignedBox() {
		min = new Vector3f();
		max = new Vector3f();
		lastFailedPlane = -1;
	}
	
	public AxisAlignedBox(Vector3f min, Vector3f max) {
		this();
		setMin(min);
		setMax(max);
	}
	
	public AxisAlignedBox(AxisAlignedBox aabb) {
		this();
		set(aabb);
	}
	
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
	
	private void enclosePoint(float x, float y, float z) {
		max.x = Math.max(max.x, x);
		max.y = Math.max(max.y, y);
		max.z = Math.max(max.z, z);

		min.x = Math.min(min.x, x);
		min.y = Math.min(min.y, y);
		min.z = Math.min(min.z, z);
	}
	
	public void set(AxisAlignedBox aabb) {
		min.set(aabb.min);
		max.set(aabb.max);
	}
	
	public void setMin(Vector3f min) {
		this.min.set(min);
	}
	
	public void setMax(Vector3f max) {
		this.max.set(max);
	}
	
	public Vector3f getCenter() {
		return getCenter(null);
	}
	
	public Vector3f getCenter(Vector3f result) {
		return min.add(max, result).scale(.5f);
	}
	
	public FrustumIntersection intersects(Frustum f, PlaneState planeState) {
		FrustumIntersection result = FrustumIntersection.INSIDE;
		float distMax;
		float distMin;
		int plane = 0;

		Vector3f c = TEMP1.get();

		Vector4f p;
		for (int i = Frustum.NUM_PLANES; i >= 0; i--) {
			// skip the last failed plane since that was is checked first,
			// or skip the default first check if we haven't failed yet
			if (i == lastFailedPlane || (i == Frustum.NUM_PLANES && lastFailedPlane < 0))
				continue;

			// check the last failed plane first, since we're likely to fail there again
			plane = (i == Frustum.NUM_PLANES ? lastFailedPlane : i);
			if (planeState == null || planeState.isTestRequired(plane)) {
				p = f.getFrustumPlane(plane);
				extent(p, false, c);
				distMax = Plane.getSignedDistance(p, c, true);
				
				extent(p, true, c);
				distMin = Plane.getSignedDistance(p, c, true);

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
	
	public boolean intersects(AxisAlignedBox other) {
		return (max.x >= other.min.x && min.x <= other.max.x) &&
			   (max.y >= other.min.y && min.y <= other.max.y) &&
			   (max.z >= other.min.z && min.z <= other.max.z);
	}
	
	public boolean contains(AxisAlignedBox other) {
		return (min.x <= other.min.x && max.x >= other.max.x) &&
			   (min.y <= other.min.y && max.y >= other.max.y) &&
			   (min.z <= other.min.z && max.z >= other.max.z);
	}
	
	public AxisAlignedBox intersect(AxisAlignedBox other, AxisAlignedBox result) {
		if (result == null)
			result = new AxisAlignedBox();
		
		// in the event that min > max, there is no true intersection
		result.min.set(Math.max(min.x, other.min.x), 
					   Math.max(min.y, other.min.y), 
					   Math.max(min.z, other.min.z));
		result.max.set(Math.min(max.x, other.max.x), 
			   		   Math.min(max.y, other.max.y), 
			   		   Math.min(max.z, other.max.z));
		return result;
	}
	
	public AxisAlignedBox union(AxisAlignedBox other, AxisAlignedBox result) {
		if (result == null)
			result = new AxisAlignedBox();
		
		result.min.set(Math.min(min.x, other.min.x), 
					   Math.min(min.y, other.min.y), 
					   Math.min(min.z, other.min.z));
		result.max.set(Math.max(max.x, other.max.x), 
			   		   Math.max(max.y, other.max.y), 
			   		   Math.max(max.z, other.max.z));
		return result;
	}
	
	public AxisAlignedBox transform(Transform t, AxisAlignedBox result) {
		return transform(t.get(TEMPM.get()), result);
	}
	
	public AxisAlignedBox transform(Matrix4f m, AxisAlignedBox result) {
		// we use temporary vectors because this method isn't atomic,
		// and result might be this box
		Vector3f newMin = TEMP1.get().set(m.m03, m.m13, m.m23);
		Vector3f newMax = TEMP2.get().set(m.m03, m.m13, m.m23);
		
		float av, bv, cv;
		int i, j;
		for (i = 0; i < 3; i++) {
			for (j = 0; j < 3; j++) {
				cv = m.get(i, j);
				av = cv * min.get(j);
				bv = cv * max.get(j);
				
				if (av < bv) {
					newMin.set(i, newMin.get(i) + av);
					newMax.set(i, newMax.get(i) + bv);
				} else {
					newMin.set(i, newMin.get(i) + bv);
					newMax.set(i, newMax.get(i) + av);
				}
			}
		}
		
		// assign temporary vectors to the result
		if (result != null) {
			result.min.set(newMin);
			result.max.set(newMax);
		} else
			result = new AxisAlignedBox(newMin, newMax);
		return result;
	}
	
	private void extent(Vector4f plane, boolean reverseDir, Vector3f result) {
		Vector3f sourceMin = (reverseDir ? max : min);
		Vector3f sourceMax = (reverseDir ? min : max);
		
		if (plane.x > 0) {
			if (plane.y > 0) {
				if (plane.z > 0)
					result.set(sourceMax.x, sourceMax.y, sourceMax.z);
				else
					result.set(sourceMax.x, sourceMax.y, sourceMin.z);
			} else {
				if (plane.z > 0)
					result.set(sourceMax.x, sourceMin.y, sourceMax.z);
				else
					result.set(sourceMax.x, sourceMin.y, sourceMin.z);
			}
		} else {
			if (plane.y > 0) {
				if (plane.z > 0)
					result.set(sourceMin.x, sourceMax.y, sourceMax.z);
				else
					result.set(sourceMin.x, sourceMax.y, sourceMin.z);
			} else {
				if (plane.z > 0)
					result.set(sourceMin.x, sourceMin.y, sourceMax.z);
				else
					result.set(sourceMin.x, sourceMin.y, sourceMin.z);
			}
		}
	}
	
	private static final ThreadLocal<Matrix4f> TEMPM = new ThreadLocal<Matrix4f>() {
		@Override
		public Matrix4f initialValue() { return new Matrix4f(); }
	};
	private static final ThreadLocal<Vector3f> TEMP1 = new ThreadLocal<Vector3f>() {
		@Override
		public Vector3f initialValue() { return new Vector3f(); }
	};
	private static final ThreadLocal<Vector3f> TEMP2 = new ThreadLocal<Vector3f>() {
		@Override
		public Vector3f initialValue() { return new Vector3f(); }
	};
}
