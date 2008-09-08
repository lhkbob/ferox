package com.ferox.core.scene.bounds;

import org.openmali.vecmath.Vector3f;

import com.ferox.core.scene.Transform;
import com.ferox.core.scene.View;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.util.io.Chunkable;

public abstract class BoundingVolume implements Chunkable {
	public static enum BoundType {
		SPHERE, AA_BOX
	}

	public abstract BoundingVolume clone(BoundingVolume result);
	public abstract void enclose(BoundingVolume toEnclose);
	public abstract void enclose(Geometry geom);
	public abstract boolean intersects(BoundingVolume other);
	public abstract void applyTransform(Transform trans);
	public abstract BoundType getBoundType();
	public abstract int testFrustum(View view, int planeState);
		
	public abstract Vector3f getFurthestExtent(Vector3f dir, Vector3f result);
	public abstract Vector3f getClosestExtent(Vector3f dir, Vector3f result);
	
	public BoundingVolume enclose(BoundingVolume toEnclose, BoundingVolume result) {
		result = this.clone(result);
		result.enclose(toEnclose);
		return result;
	}
	
	public BoundingVolume applyTransform(Transform trans, BoundingVolume result) {
		result = this.clone(result);
		result.applyTransform(trans);
		return result;
	}
	
	public BoundingVolume enclose(Geometry geom, BoundingVolume result) {
		result = this.clone(result);
		result.enclose(geom);
		return result;
	}
}
