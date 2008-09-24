package com.ferox.core.scene.bounds;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Vector3f;

import com.ferox.core.scene.Plane;
import com.ferox.core.scene.Transform;
import com.ferox.core.scene.View;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class AxisAlignedBox extends BoundingVolume {
	private static final Vector3f c = new Vector3f();
	private static final Matrix3f m = new Matrix3f();
	private static final Vector3f min = new Vector3f();
	private static final Vector3f max = new Vector3f();
	
	private final Vector3f worldMax;
	private final Vector3f worldMin;
	private int lastFailedPlane;
	
	public AxisAlignedBox() {
		super();
		this.worldMax = new Vector3f();
		this.worldMin = new Vector3f();
		this.lastFailedPlane = -1;
	}
	
	public AxisAlignedBox(Vector3f min, Vector3f max) {
		this(min.x, min.y, min.z, max.x, max.y, max.z);
	}
	
	public AxisAlignedBox(Vector3f halfExtents) {
		this(halfExtents.x, halfExtents.y, halfExtents.z);
	}
	
	public AxisAlignedBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		this();
		this.setMin(minX, minY, minZ);
		this.setMax(maxX, maxY, maxZ);
	}
	
	public AxisAlignedBox(float width, float height, float depth) {
		this();
		this.setMin(-width / 2f, -height / 2f, -depth / 2f);
		this.setMax(width / 2f, height / 2f, depth / 2f);
	}
	
	public void setMax(Vector3f m) {
		if (m == null)
			this.worldMax.set(0f, 0f, 0f);
		else
			this.worldMax.set(m);
	}
	
	public void setMin(Vector3f m) { 
		if (m == null)
			this.worldMin.set(0f, 0f, 0f);
		else
			this.worldMin.set(m);
	}
	
	public void setMax(float x, float y, float z) {
		this.worldMax.set(x, y, z);
	}
	
	public void setMin(float x, float y, float z) {
		this.worldMin.set(x, y, z);
	}
	
	public Vector3f getMax() {
		return this.worldMax;
	}
	
	public Vector3f getMin() {
		return this.worldMin;
	}
	
	public Vector3f getCenter(Vector3f out) {
		if (out == null)
			out = new Vector3f();
		out.add(this.worldMin, this.worldMax);
		out.scale(.5f);
		return out;
	}
	
	@Override
	public String toString() {
		return this.worldMin + " " + this.worldMax;
	}
	
	@Override
	public void applyTransform(Transform trans) throws NullPointerException {
		if (trans == null)
			throw new NullPointerException("Can't apply a null transform");
		Vector3f s = trans.getScale();
		Vector3f t = trans.getTranslation();
		this.worldMax.set(s.x * this.worldMax.x, s.y * this.worldMax.y, s.z * this.worldMax.z);
		this.worldMin.set(s.x * this.worldMin.x, s.y * this.worldMin.y, s.z * this.worldMin.z);
		
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
	public BoundingVolume clone(BoundingVolume result) {
		if (result == null || result.getBoundType() != BoundType.AA_BOX)
			result = new AxisAlignedBox();
		AxisAlignedBox b = (AxisAlignedBox)result;
		b.worldMax.set(this.worldMax);
		b.worldMin.set(this.worldMin);
		return b;
	}

	@Override
	public void enclose(Geometry geom) throws NullPointerException, IllegalArgumentException {
		if (geom == null)
			throw new NullPointerException("Cannot enclose a null geometry");
		if (!geom.getVertices().getBufferData().isDataInClientMemory())
			return;
		
		this.worldMax.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		this.worldMin.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		
		switch(geom.getVertices().getBufferData().getDataType()) {
		case FLOAT:
			this.encloseFloatBuffer((FloatBuffer)(geom.getVertices().getBufferData().getData()), geom.getVertices(), geom.getVertices().getElementSize(), geom.getVertices().getNumElements());
			break;
		case DOUBLE:
			this.encloseDoubleBuffer((DoubleBuffer)(geom.getVertices().getBufferData().getData()), geom.getVertices(), geom.getVertices().getElementSize(), geom.getVertices().getNumElements());
			break;
		case INT:
			this.encloseIntBuffer((IntBuffer)(geom.getVertices().getBufferData().getData()), geom.getVertices(), geom.getVertices().getElementSize(), geom.getVertices().getNumElements());
			break;
		case SHORT:
			this.encloseShortBuffer((ShortBuffer)(geom.getVertices().getBufferData().getData()), geom.getVertices(), geom.getVertices().getElementSize(), geom.getVertices().getNumElements());
			break;
		default:
			throw new IllegalArgumentException("Unsupported geometry vertex type");
		}
	}

	private void encloseFloatBuffer(FloatBuffer verts, VertexArray vA, int elSz, int numEl) {
		int index;
		switch(elSz) {
		case 2:
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				this.enclosePoint(verts.get(index), verts.get(index + 1), 0);
			}
			break;
		case 3:
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				this.enclosePoint(verts.get(index), verts.get(index + 1), verts.get(index + 2));
			}
			break;
		case 4:
			float fourth;
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				fourth = verts.get(index + 3);
				this.enclosePoint(verts.get(index) / fourth, verts.get(index + 1) / fourth, verts.get(index + 2) / fourth);
			}
			break;
		}
	}
	
	private void encloseDoubleBuffer(DoubleBuffer verts, VertexArray vA, int elSz, int numEl) {
		int index;
		switch(elSz) {
		case 2:
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				this.enclosePoint((float)verts.get(index), (float)verts.get(index + 1), 0);
			}
			break;
		case 3:
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				this.enclosePoint((float)verts.get(index), (float)verts.get(index + 1), (float)verts.get(index + 2));
			}
			break;
		case 4:
			float fourth;
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				fourth = (float)verts.get(index + 3);
				this.enclosePoint((float)verts.get(index) / fourth, (float)verts.get(index + 1) / fourth, (float)verts.get(index + 2) / fourth);
			}
			break;
		}
	}
	
	private void encloseIntBuffer(IntBuffer verts, VertexArray vA, int elSz, int numEl) {
		int index;
		switch(elSz) {
		case 2:
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				this.enclosePoint(verts.get(index), verts.get(index + 1), 0);
			}
			break;
		case 3:
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				this.enclosePoint(verts.get(index), verts.get(index + 1), verts.get(index + 2));
			}
			break;
		case 4:
			float fourth;
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				fourth = verts.get(index + 3);
				this.enclosePoint(verts.get(index) / fourth, verts.get(index + 1) / fourth, verts.get(index + 2) / fourth);
			}
			break;
		}
	}
	
	private void encloseShortBuffer(ShortBuffer verts, VertexArray vA, int elSz, int numEl) {
		int index;
		switch(elSz) {
		case 2:
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				this.enclosePoint(verts.get(index), verts.get(index + 1), 0);
			}
			break;
		case 3:
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				this.enclosePoint(verts.get(index), verts.get(index + 1), verts.get(index + 2));
			}
			break;
		case 4:
			float fourth;
			for (int i = 0; i < numEl; i++) {
				index = vA.getIndex(i);
				fourth = verts.get(index + 3);
				this.enclosePoint(verts.get(index) / fourth, verts.get(index + 1) / fourth, verts.get(index + 2) / fourth);
			}
			break;
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
	
	@Override
	public BoundType getBoundType() {
		return BoundType.AA_BOX;
	}

	@Override
	public void enclose(BoundingVolume child) throws NullPointerException {
		if (child == null)
			throw new NullPointerException("Cannot enclose a null bounding volume");
		switch (child.getBoundType()) {
		case AA_BOX: this.mergeAABB((AxisAlignedBox)child); break;
		case SPHERE: this.mergeSphere((BoundingSphere)child); break;
		}
	}
	
	private void mergeAABB(AxisAlignedBox aabb) {
		this.worldMax.x = Math.max(this.worldMax.x, aabb.worldMax.x);
		this.worldMax.y = Math.max(this.worldMax.y, aabb.worldMax.y);
		this.worldMax.z = Math.max(this.worldMax.z, aabb.worldMax.z);
		
		this.worldMin.x = Math.min(this.worldMin.x, aabb.worldMin.x);
		this.worldMin.y = Math.min(this.worldMin.y, aabb.worldMin.y);
		this.worldMin.z = Math.min(this.worldMin.z, aabb.worldMin.z);
	}
	
	private void mergeSphere(BoundingSphere sphere) {
		Vector3f c = sphere.getCenter();
		float r = sphere.getRadius();
		this.worldMax.x = Math.max(this.worldMax.x, c.x + r);
		this.worldMax.y = Math.max(this.worldMax.y, c.y + r);
		this.worldMax.z = Math.max(this.worldMax.z, c.z + r);
		
		this.worldMin.x = Math.min(this.worldMin.x, c.x - r);
		this.worldMin.y = Math.min(this.worldMin.y, c.y - r);
		this.worldMin.z = Math.min(this.worldMin.z, c.z - r);
	}

	private void getMinMaxWorldVertices(Vector3f normal, Vector3f min, Vector3f max) {
		if (normal.x > 0) {
			if (normal.y > 0) {
				if (normal.z > 0) {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				} else {
					max.x = this.worldMax.x; max.y = this.worldMax.y; max.z = this.worldMin.z;
					min.x = this.worldMin.x; min.y = this.worldMin.y; min.z = this.worldMax.z;
				}
			} else {
				if (normal.z > 0) {
					max.x = this.worldMax.x; max.y = this.worldMin.y; max.z = this.worldMax.z;
					min.x = this.worldMin.x; min.y = this.worldMax.y; min.z = this.worldMin.z;
				} else {
					max.x = this.worldMax.x; max.y = this.worldMin.y; max.z = this.worldMin.z;
					min.x = this.worldMin.x; min.y = this.worldMax.y; min.z = this.worldMax.z;
				}
			}
		} else {
			if (normal.y > 0) {
				if (normal.z > 0) {
					max.x = this.worldMin.x; max.y = this.worldMax.y; max.z = this.worldMax.z;
					min.x = this.worldMax.x; min.y = this.worldMin.y; min.z = this.worldMin.z;
				} else {
					max.x = this.worldMin.x; max.y = this.worldMax.y; max.z = this.worldMin.z;
					min.x = this.worldMax.x; min.y = this.worldMin.y; min.z = this.worldMax.z;
				}
			} else {
				if (normal.z > 0) {
					max.x = this.worldMin.x; max.y = this.worldMin.y; max.z = this.worldMax.z;
					min.x = this.worldMax.x; min.y = this.worldMax.y; min.z = this.worldMin.z;
				} else {
					max.x = this.worldMin.x; max.y = this.worldMin.y; max.z = this.worldMin.z;
					min.x = this.worldMax.x; min.y = this.worldMax.y; min.z = this.worldMax.z;
				}
			}
		}
	}
	
	@Override
	public int testFrustum(View view, int planeState) throws NullPointerException {
		if (view == null)
			throw new NullPointerException("Cannot test a frustum with a null view");
		Plane[] planes = view.getWorldFrustumPlanes();
		
		int result = View.INSIDE;
		float distMax;
		float distMin;
		int plane = 0;
		
		for (int i = planes.length; i >= 0; i--) {
			if (i == this.lastFailedPlane || (i == planes.length && this.lastFailedPlane < 0))
				continue;
	
			if (i == planes.length) 
				plane = this.lastFailedPlane;
			else 
				plane = i;

			if ((planeState & (1 << plane)) == 0) {
				this.getMinMaxWorldVertices(planes[plane].getNormal(), min, max);
				distMax = planes[plane].signedDistance(max);
				distMin = planes[plane].signedDistance(min);

				if (distMax < 0) {
					view.setPlaneState(planeState);
					this.lastFailedPlane = plane;
					return View.OUTSIDE;
				} else if (distMin < 0)
					result = View.INTERSECT;
				else
					planeState |= (1 << plane);
			}
		}
		
		view.setPlaneState(planeState);
		return result;
	}

	@Override
	public Vector3f getClosestExtent(Vector3f dir, Vector3f out) throws NullPointerException {
		if (dir == null)
			throw new NullPointerException("Can't compute extent with a null direction vector");
		if (out == null)
			out = new Vector3f();
		this.getMinMaxWorldVertices(dir, min, max);
		out.set(min);
		return out;
	}

	@Override
	public Vector3f getFurthestExtent(Vector3f dir, Vector3f out) throws NullPointerException {
		if (dir == null)
			throw new NullPointerException("Can't compute extent with a null direction vector");
		if (out == null)
			out = new Vector3f();
		this.getMinMaxWorldVertices(dir, min, max);
		out.set(max);
		return out;
	}

	public void readChunk(InputChunk in) {
		this.worldMax.set(in.getFloat("max_x"), in.getFloat("max_y"), in.getFloat("max_z"));
		this.worldMin.set(in.getFloat("min_x"), in.getFloat("min_y"), in.getFloat("min_z"));
	}

	public void writeChunk(OutputChunk out) {
		out.set("max_x", this.worldMax.x);
		out.set("max_y", this.worldMax.y);
		out.set("max_z", this.worldMax.z);
		
		out.set("min_x", this.worldMin.x);
		out.set("min_y", this.worldMin.y);
		out.set("min_z", this.worldMin.z);
	}

	@Override
	public boolean intersects(BoundingVolume other) {
		if (other == null)
			return false;
		if (other.getBoundType() == BoundType.AA_BOX) {
			AxisAlignedBox a = (AxisAlignedBox)other;
			return ((a.worldMax.x >= this.worldMin.x && a.worldMax.x <= this.worldMax.x) || (a.worldMin.x >= this.worldMin.x && a.worldMin.x <= this.worldMax.x)) &&
				   ((a.worldMax.y >= this.worldMin.y && a.worldMax.y <= this.worldMax.y) || (a.worldMin.y >= this.worldMin.y && a.worldMin.y <= this.worldMax.y)) &&
				   ((a.worldMax.z >= this.worldMin.z && a.worldMax.z <= this.worldMax.z) || (a.worldMin.z >= this.worldMin.z && a.worldMin.z <= this.worldMax.z));
		} else if (other.getBoundType() == BoundType.SPHERE) {
			BoundingSphere s = (BoundingSphere)other;
			this.getCenter(c);
			c.sub(s.getCenter());
			this.getClosestExtent(c, c);
			c.sub(s.getCenter());
			return c.lengthSquared() <= s.getRadius() * s.getRadius();
		}
		return false;
	}
}
