package com.ferox.renderer.impl;

import com.ferox.math.Transform;
import com.ferox.shader.View;

/**
 * <p>
 * TransformDriver provides methods to set the matrix transforms used when
 * rendering RenderAtoms.
 * </p>
 * <p>
 * Drivers can assumed that a low-level context is made current on the calling
 * thread and that low-level graphics calls are allowed.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface TransformDriver {
	/**
	 * Set the current view on the low-level graphics hardware. For OpenGL like
	 * systems, this involves setting the projection matrix and the view matrix.
	 * It can be assumed that setView() and resetView() calls are paired
	 * together.
	 * 
	 * @param view The View to be used
	 * @param width The width of the current surface, in pixels
	 * @param height The height of the current surface, in pixels
	 */
	public void setView(View view, int width, int height);

	/**
	 * Set the model transform to use. This must preserve the projection matrix
	 * and the view transform previously set with setView(). The transform will
	 * not be null and is in world space. It can be assumed that
	 * setModelTransform() and resetModel() calls are paired; also,
	 * setModelTransform() will not be called until after setView().
	 * 
	 * @param transform The Transform representing the world/model transform
	 */
	public void setModelTransform(Transform transform);

	/**
	 * Reset the model portion of the transform, so that the current cumulative
	 * modelview transform is equal to only the view transform previously set by
	 * setView()
	 */
	public void resetModel();

	/**
	 * Reset the model-view and projection matrices because the render pass has
	 * completed. This does not necessarily require actual state modification,
	 * it must just work correctly the next time setView() is called.
	 */
	public void resetView();
}
