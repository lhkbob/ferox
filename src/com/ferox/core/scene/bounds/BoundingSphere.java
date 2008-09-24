package com.ferox.core.scene.bounds;

import java.nio.*;

import org.openmali.vecmath.Vector3f;

import com.ferox.core.scene.Transform;
import com.ferox.core.scene.View;
import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class BoundingSphere extends BoundingVolume {
	private static final Vector3f diff = new Vector3f();
	
	private static final Vector3f tempA = new Vector3f();
	private static final Vector3f tempB = new Vector3f();
	private static final Vector3f tempC = new Vector3f();
	private static final Vector3f tempD = new Vector3f();
	
	private static final Vector3f tA = new Vector3f();
	private static final Vector3f tB = new Vector3f();
	private static final Vector3f tC = new Vector3f();
	private static final Vector3f tD = new Vector3f();
	private static final Vector3f cross = new Vector3f();
	
	private static final float radiusEpsilon = 1.00001f;
	
	private float radius;
	private final Vector3f centerOffset;
	
	public BoundingSphere() {
		super();
		this.radius = 0;
		this.centerOffset = new Vector3f();
	}
	
	public BoundingSphere(float radius) {
		this();
		this.setRadius(radius);
	}
	
	public BoundingSphere(Vector3f center, float radius) {
		this();
		this.setCenter(center);
		this.setRadius(radius);
	}
	
	public BoundingSphere(float x, float y, float z, float radius) {
		this();
		this.setCenter(x, y, z);
		this.setRadius(radius);
	}
	
	@Override
	public BoundingVolume clone(BoundingVolume result) {
		if (result == null || result.getBoundType() != BoundType.SPHERE)
			result = new BoundingSphere();
		BoundingSphere s = (BoundingSphere)result;
		s.radius = this.radius;
		s.centerOffset.set(this.centerOffset);
		return s;
	}
	
	public String toString() {
		return "BoundingSphere: " + this.centerOffset + " | " + this.radius;
	}
	
	public void setRadius(float r) {
		this.radius = Math.max(radiusEpsilon - 1f, r);
	}
	
	public void setCenter(Vector3f center) {
		if (center == null)
			this.centerOffset.set(0f, 0f, 0f);
		else
			this.centerOffset.set(center);
	}
	
	public void setCenter(float x, float y, float z) {
		this.centerOffset.set(x, y, z);
	}
	
	public Vector3f getCenter() {
		return this.centerOffset;
	}
	
	public float getRadius() {
		return this.radius;
	}
	
	@Override
	public void applyTransform(Transform trans) throws NullPointerException {
		if (trans == null)
			throw new NullPointerException("Can't apply a null transform");
		trans.transform(this.centerOffset);
		Vector3f s = trans.getScale();
		float max = Math.max(s.x, Math.max(s.y, s.z));
		this.radius *= max;
	}

	@Override
	public void enclose(Geometry geom) throws NullPointerException, IllegalArgumentException {
		if (geom == null)
			throw new NullPointerException("Can't enclose a null geometry");
		if (!geom.getVertices().getBufferData().isDataInClientMemory())
			return;
		
		float[] points = new float[geom.getVertices().getNumElements() * 3];
		fillPointsArray(points, geom.getVertices());
		
		this.recurseMini(points, points.length / 3, 0, 0);
	}
	
	private void recurseMini(float[] points, int p, int b, int ap) {
		 switch (b) {
	        case 0:
	            this.radius = 0;
	            this.centerOffset.set(0f, 0f, 0f);
	            break;
	        case 1:
	            this.radius = 1f - radiusEpsilon;
	            populateFromArray(this.centerOffset, points, ap - 1);
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
	            float d = ((tempA.x - this.centerOffset.x) * (tempA.x - this.centerOffset.x) + 
		            	 (tempA.y - this.centerOffset.y) * (tempA.y - this.centerOffset.y) +  
		            	 (tempA.z - this.centerOffset.z) * (tempA.z - this.centerOffset.z));
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
		tA.sub(a, o);
		tB.sub(b, o);
		tC.sub(c, o);
		
		float denom = 2.0f * (tA.x * (tB.y * tC.z - tC.y * tB.z) - tB.x * (tA.y * tC.z - tC.y * tA.z) + tC.x * (tA.y * tB.z - tB.y * tA.z));
		if (denom == 0) {
			this.centerOffset.set(0f, 0f, 0f);
			this.radius = 0f;
		} else {
			cross.cross(tA, tB); cross.scale(tC.lengthSquared());
			tD.cross(tC, tA); tD.scale(tB.lengthSquared()); cross.add(tD);
			tD.cross(tB, tC); tD.scale(tA.lengthSquared()); cross.add(tD);
			cross.scale(1f / denom);

	        this.radius = cross.length() * radiusEpsilon;
	        this.centerOffset.add(o, cross);
		}
	}

	private void setSphere(Vector3f o, Vector3f a, Vector3f b) {
		tA.sub(a, o);
		tB.sub(b, o);
		cross.cross(tA, tB);
		
		float denom = 2f * cross.lengthSquared();

		if (denom == 0) {
			this.centerOffset.set(0f, 0f, 0f);
			this.radius = 0f;
		} else {
			tC.cross(cross, tA); tC.scale(tB.lengthSquared());
			tD.cross(tB, cross); tD.scale(tA.lengthSquared());
			tC.add(tD); tC.scale(1f / denom);

			this.radius = tC.length() * radiusEpsilon;
			this.centerOffset.add(o, tC);
		}
	}

	private void setSphere(Vector3f o, Vector3f a) {
		this.radius = (float)Math.sqrt(((a.x - o.x) * (a.x - o.x) + (a.y - o.y) * (a.y - o.y) + (a.z - o.z) * (a.z - o.z)) / 4f) + radiusEpsilon - 1f;
	    
		this.centerOffset.scale(.5f, o);
	    this.centerOffset.scaleAdd(.5f, a, this.centerOffset);
	}

	private static void fillPointsArray(float[] points, VertexArray verts) {
		BufferData.DataType type = verts.getBufferData().getDataType();
		int elSize = verts.getElementSize();
		switch(type) {
		case FLOAT: {
			FloatBuffer b = (FloatBuffer)(verts.getBufferData().getData());
			int u;
			for (int i = 0; i < verts.getNumElements(); i++) {
				u = verts.getIndex(i);
				points[i * 3] = b.get(u);
				points[i * 3 + 1] = b.get(u + 1);
				if (elSize >= 3)
					points[i * 3 + 2] = b.get(u + 2);
				else
					points[i * 3 + 2] = 0;
				if (elSize >= 4) {
					float d = b.get(u + 3);
					points[i * 3] /= d;
					points[i * 3 + 1] /= d;
					points[i * 3 + 2] /= d;
				}
			}
			break; }
		case DOUBLE: {
			DoubleBuffer b = (DoubleBuffer)(verts.getBufferData().getData());
			int u;
			for (int i = 0; i < verts.getNumElements(); i++) {
				u = verts.getIndex(i);
				points[i * 3] = (float)b.get(u);
				points[i * 3 + 1] = (float)b.get(u + 1);
				if (elSize >= 3)
					points[i * 3 + 2] = (float)b.get(u + 2);
				else
					points[i * 3 + 2] = 0;
				if (elSize >= 4) {
					float d = (float)b.get(u + 3);
					points[i * 3] /= d;
					points[i * 3 + 1] /= d;
					points[i * 3 + 2] /= d;
				}
			}
			break; }
		case INT: {
			IntBuffer b = (IntBuffer)(verts.getBufferData().getData());
			int u;
			for (int i = 0; i < verts.getNumElements(); i++) {
				u = verts.getIndex(i);
				points[i * 3] = b.get(u);
				points[i * 3 + 1] = b.get(u + 1);
				if (elSize >= 3)
					points[i * 3 + 2] = b.get(u + 2);
				else
					points[i * 3 + 2] = 0;
				if (elSize >= 4) {
					float d = b.get(u + 3);
					points[i * 3] /= d;
					points[i * 3 + 1] /= d;
					points[i * 3 + 2] /= d;
				}
			}
			break; }
		case SHORT:	{
			ShortBuffer b = (ShortBuffer)(verts.getBufferData().getData());
			int u;
			for (int i = 0; i < verts.getNumElements(); i++) {
				u = verts.getIndex(i);
				points[i * 3] = b.get(u);
				points[i * 3 + 1] = b.get(u + 1);
				if (elSize >= 3)
					points[i * 3 + 2] = b.get(u + 2);
				else
					points[i * 3 + 2] = 0;
				if (elSize >= 4) {
					float d = b.get(u + 3);
					points[i * 3] /= d;
					points[i * 3 + 1] /= d;
					points[i * 3 + 2] /= d;
				}
			}
			break; }
		default:
			throw new IllegalArgumentException("Unsupported geometry vertex type");
		}
	}

	@Override
	public BoundType getBoundType() {
		return BoundType.SPHERE;
	}

	@Override
	public void enclose(BoundingVolume child) throws NullPointerException {
		if (child == null)
			throw new NullPointerException("Can't enclose a null bounding volume");
		switch(child.getBoundType()) {
		case SPHERE: this.mergeSphere((BoundingSphere)child); break;
		case AA_BOX: this.mergeAABB((AxisAlignedBox)child); break;
		}
	}

	private void mergeAABB(AxisAlignedBox aabb) {
		aabb.getCenter(diff);
		diff.sub(this.centerOffset);
		aabb.getFurthestExtent(diff, tempB);
		diff.sub(tempB, this.centerOffset);
		float dist = diff.length();
		
		if (dist > this.radius) {
			float or = this.radius;
			this.radius = (dist + or) / 2f;
			this.centerOffset.scaleAdd((this.radius - or) / dist, diff, this.centerOffset);
		}
	}
	
	private void mergeSphere(BoundingSphere sphere) {
		diff.sub(this.centerOffset, sphere.centerOffset);
		float dist = diff.length();
		
		if (dist + sphere.radius > this.radius) {
			this.radius = (dist + sphere.radius + this.radius) / 2f;
			this.centerOffset.scaleAdd((this.radius - sphere.radius) / dist, diff, sphere.centerOffset);
		}
	}

	@Override
	public int testFrustum(View view, int planeState) throws NullPointerException {
		if (view == null)
			throw new NullPointerException("Can't test a frustum of a null view");
		if (this.radius < 0)
			return View.INTERSECT;

		diff.set(this.centerOffset);
		view.getInverseWorldTransform().transform(diff);
		return view.classify(diff.x, diff.y, diff.z, this.radius, planeState);
	}

	@Override
	public Vector3f getClosestExtent(Vector3f dir, Vector3f out) throws NullPointerException {
		if (dir == null)
			throw new NullPointerException("Can't compute extent for a null direction");
		if (out == null)
			out = new Vector3f();
		out.set(this.centerOffset);
		out.x -= dir.x * this.radius;
		out.y -= dir.y * this.radius;
		out.z -= dir.z * this.radius;
		return out;
	}

	@Override
	public Vector3f getFurthestExtent(Vector3f dir, Vector3f out) throws NullPointerException {
		if (dir == null)
			throw new NullPointerException("Can't compute extent for a null direction");
		if (out == null)
			out = new Vector3f();
		out.set(this.centerOffset);
		out.x += dir.x * this.radius;
		out.y += dir.y * this.radius;
		out.z += dir.z * this.radius;
		return out;
	}

	public void readChunk(InputChunk in) {
		this.centerOffset.set(in.getFloat("center_x"), in.getFloat("center_y"), in.getFloat("center_z"));
		this.radius = in.getFloat("radius");
	}

	public void writeChunk(OutputChunk out) {
		out.set("center_x", this.centerOffset.x);
		out.set("center_y", this.centerOffset.y);
		out.set("center_z", this.centerOffset.z);

		out.set("radius", this.radius);
	}

	@Override
	public boolean intersects(BoundingVolume other) {
		if (other == null)
			return false;
		if (other.getBoundType() == BoundType.AA_BOX) {
			return other.intersects(this);
		} else if (other.getBoundType() == BoundType.SPHERE) {
			BoundingSphere s = (BoundingSphere)other;
			cross.sub(this.centerOffset, s.centerOffset);
			return cross.lengthSquared() <= (this.radius + s.radius) * (this.radius + s.radius);
		}
		return false;
	}
}
