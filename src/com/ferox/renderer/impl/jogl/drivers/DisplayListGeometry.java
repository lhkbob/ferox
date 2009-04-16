package com.ferox.renderer.impl.jogl.drivers;

import java.util.List;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.BoundSphere;
import com.ferox.math.BoundVolume;
import com.ferox.renderer.RenderAtom;
import com.ferox.resource.Geometry;

/** Geometry implementation used internally b JoglDisplayListGeometryDriver. 
 * There is very little logic contained within this class. 
 * 
 * @author Michael Ludwig
 *
 */
public class DisplayListGeometry implements Geometry {
	private Object resourceData;
	private final int vertexCount;

	private final AxisAlignedBox box;
	private final BoundSphere sphere;

	public DisplayListGeometry(List<RenderAtom> atoms) {
		int vCount = 0;
		BoundVolume bounds = null;

		int numAtoms = atoms.size();
		RenderAtom a;
		BoundVolume aBounds;
		for (int i = 0; i < numAtoms; i++) {
			a = atoms.get(i);
			aBounds = a.getBounds();
			vCount += a.getGeometry().getVertexCount();

			if (aBounds != null) {
				if (bounds == null) 
					bounds = aBounds.clone(bounds);
				else
					bounds.enclose(aBounds);
			}
		}

		if (bounds instanceof AxisAlignedBox) {
			this.box = (AxisAlignedBox) bounds;
			this.sphere = new BoundSphere();

			this.sphere.setCenter(this.box.getCenter(null));
			this.sphere.setRadius(0f);
			this.sphere.enclose(this.box);
		} else if (bounds instanceof BoundSphere) {
			this.sphere = (BoundSphere) bounds;
			this.box = new AxisAlignedBox();

			this.box.setMax(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
			this.box.setMin(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
			this.box.enclose(this.sphere);
		} else {
			// use default bounds
			this.sphere = new BoundSphere();
			this.box = new AxisAlignedBox();
		}

		this.vertexCount = vCount;
	}

	@Override
	public boolean isAppearanceIgnored() {
		return true;
	}

	@Override
	public void getBounds(BoundVolume result) {
		if (result != null) {
			if (result instanceof AxisAlignedBox) {
				this.box.clone(result);
			} else if (result instanceof BoundSphere) {
				this.sphere.clone(result);
			}
		}
	}

	@Override
	public float getVertex(int index, int coord) throws IllegalArgumentException {
		return 0f;
	}

	@Override
	public int getVertexCount() {
		return this.vertexCount;
	}

	@Override
	public void clearDirtyDescriptor() {
		// do nothing
	}

	@Override
	public Object getDirtyDescriptor() {
		return null;
	}

	@Override
	public Object getResourceData() {
		return this.resourceData;
	}

	@Override
	public void setResourceData(Object data) {
		this.resourceData = data;
	}
}