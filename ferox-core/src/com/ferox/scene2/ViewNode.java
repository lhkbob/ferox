package com.ferox.scene2;

import com.ferox.math.Matrix3f;
import com.ferox.renderer.View;

/**
 * A ViewNode allows the updating of a View's location based on the node's
 * position in a scene. If the node doesn't have a view attached to it, it
 * functions as any other group. If multiple view node's use the same view
 * object, there are undefined results because of possible Framework,
 * out-of-order execution and contention for being the last view node to modify
 * the view's location.
 * 
 * @author Michael Ludwig
 */
public class ViewNode extends AbstractSceneElement {
	private View view;

	/**
	 * Creates a ViewNode with the given view.
	 * 
	 * @param view The View that is updated by this ViewNode
	 */
	public ViewNode(View view) {
		setView(view);
	}

	/**
	 * Set the attached View for this ViewNode.
	 * 
	 * @param view The new View to use
	 */
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * @return The attached view, may be null.
	 */
	public View getView() {
		return view;
	}

	/**
	 * Override updateTransform to update the attached view (if not null).
	 * 
	 * @param fast
	 */
	@Override
	public boolean update(float timeDelta) {
		boolean res = super.update(timeDelta);
		if (view != null) {
			view.setLocation(worldTransform.getTranslation());
			Matrix3f b = worldTransform.getRotation();
			view.getDirection().set(b.m02, b.m12, b.m22);
			view.getUp().set(b.m01, b.m11, b.m21);
		}
		
		return res;
	}
}
