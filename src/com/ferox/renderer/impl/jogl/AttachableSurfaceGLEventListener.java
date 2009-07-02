package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/**
 * Provides a slightly more extended interface to support the surface paradigm
 * of the Framework interface.
 * 
 * @author Michael Ludwig
 */
public interface AttachableSurfaceGLEventListener extends GLEventListener {
	/**
	 * Return the GLAutoDrawable associated that has this listener as a
	 * GLEventListener. If null is returned, then attach/assign and render()
	 * should not be used, and instead this listener must be attached to a
	 * surface with an actual drawable.
	 */
	public GLAutoDrawable getGLAutoDrawable();

	/**
	 * Attach the given surface so that it will be rendered into during this
	 * surface's execution of display(). It is assumed that the given surface
	 * hasn't already been attached for this frame. Also, the surface should
	 * have a null GLAutoDrawable, or is actually this surface. Any attached
	 * surfaces are to be cleared after display() is finished.
	 */
	public void attachRenderSurface(JoglRenderSurface surface);

	/**
	 * Assign the given runnable to executed by this surface when its display()
	 * method is called. The runnable is assumed to be the resource action
	 * passed in during renderFrame() of the factory. After display() this will
	 * be reset to null. It is assumed that this is called only if the surface
	 * has a non-null GLAutoDrawable.
	 */
	public void assignResourceAction(Runnable action);

	/**
	 * Return the JoglStateRecord associated with the context of this listener's
	 * drawable. If null is returned from that, this method must return the
	 * record of the actively displaying surface (e.g. from
	 * factory.getRecord()).
	 */
	public JoglStateRecord getStateRecord();

	/**
	 * Invoke the display() method of the GLAutoDrawable to render all attached
	 * surfaces and perform the resource action. If an exception occurs while
	 * rendering, it must be re-thrown in this thread instead of interrupting
	 * the EDT thread.
	 */
	public void render() throws RenderException;
}
