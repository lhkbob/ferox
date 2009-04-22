package com.ferox.scene;

import org.openmali.vecmath.Matrix3f;

import com.ferox.renderer.View;

/**
 * A ViewNode allows the updating of a View's location based on the node's position in a 
 * scene.  If the node doesn't have a view attached to it, it functions as any other group.
 * If multiple view node's use the same view object, there are undefined results because
 * of possible Renderer, out-of-order execution and contention for being the last view node
 * to modify the view's location.
 * 
 * @author Michael Ludwig
 *
 */
public class ViewNode extends Group {
	private View view;
	
	/** Creates a ViewNode with the given view. */
	public ViewNode(View view) {
		this.setView(view);
	}
	
	/** Set the attached View for this ViewNode. */
	public void setView(View view) {
		this.view = view;
	}
	
	/** Get the attached view, may be null. */
	public View getView() {
		return this.view;
	}
	
	/** Override updateTransform to update the attached view (if not null). */
	public void updateTransform(boolean fast) {
		super.updateTransform(fast);
		if (this.view != null) {
			this.view.setLocation(this.worldTransform.getTranslation());
			Matrix3f b = this.worldTransform.getRotation();
			this.view.getDirection().set(b.m02, b.m12, b.m22);
			this.view.getUp().set(b.m01, b.m11, b.m21);
		}
	}
}
