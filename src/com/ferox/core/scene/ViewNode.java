package com.ferox.core.scene;

import org.openmali.vecmath.Matrix3f;

/**
 * A ViewNode represents the physical location of a View within a given scene.
 * Multiple View's can have the same ViewNode, although this doesn't matter so much because
 * it is unlikely that the exact same scene will be used in multiple passes (if it is however,
 * then the same view node can be used with each pass's View object).
 * @author Michael Ludwig
 *
 */
public class ViewNode extends SpatialBranch {	
	private View camera;
	
	public ViewNode(View view) {
		this(view, null);
	}
	
	public ViewNode(View view, SpatialBranch parent) {
		super(parent, 1);
		this.setView(view);
	}
	
	public View getView() {
		return this.camera;
	}
	
	public void setView(View view) {
		this.camera = view;
	}
	
	public void updateTransform(boolean fast) {
		super.updateTransform(fast);
		if (this.camera != null) {
			this.camera.setLocation(this.worldTransform.getTranslation());
			Matrix3f b = this.worldTransform.getRotation();
			this.camera.getDirection().set(b.m02, b.m12, b.m22);
			this.camera.getUp().set(b.m01, b.m11, b.m21);
		}
	}
	
}
