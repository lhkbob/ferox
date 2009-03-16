package com.ferox.renderer.impl;

import com.ferox.renderer.View;
import com.ferox.math.Transform;

/** TransformDriver provides methods to set
 * the matrix transforms used when rendering 
 * geometries.
 * 
 * Drivers can assumed that a low-level context is made current on the calling
 * thread and that low-level graphics calls are allowed.  Implementations must
 * provide a means by which to get at this information.
 * 
 * @author Michael Ludwig
 *
 */
public interface TransformDriver {
	/** Set the current view on the low-level graphics hardware.
	 * For OpenGL like systems, this involves setting the projection
	 * matrix and the view matrix.  It can be assumed that the view isn't null. */
	public void setView(View view, int width, int height);
	
	/** Set the model transform to use.  This must preserve the
	 * projection matrix and the view transform.  The transform
	 * will not be null and is in world space. */
	public void setModelTransform(Transform transform);
	
	/** Reset the model portion of the transform, so that current cumulative
	 * transform is equal to only the view transform set previously. */
	public void resetModel();
	
	/** Reset the model-view and projection matrices because the
	 * render pass has completed. */
	public void resetView();
}
