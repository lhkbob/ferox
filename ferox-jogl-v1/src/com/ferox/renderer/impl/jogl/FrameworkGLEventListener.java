package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/**
 * Provides a slightly more extended interface to support the surface paradigm
 * of the Framework interface.
 * 
 * @author Michael Ludwig
 */
public interface FrameworkGLEventListener extends GLEventListener {
	/**
	 * Return the GLAutoDrawable associated that has this listener as a
	 * GLEventListener. If null is returned, then attach/assign and render()
	 * should not be used, and instead this listener must be attached to a
	 * surface with an actual drawable.
	 */
	public GLAutoDrawable getGLAutoDrawable();

	/**
	 * Return the JoglStateRecord associated with the context of this listener's
	 * drawable. If null is returned from that, this method must return the
	 * record of the actively displaying surface (e.g. from
	 * factory.getRecord()).
	 */
	public JoglStateRecord getStateRecord();
	
	public Action getPreRenderAction();
	
	public Action getPostRenderAction();
	
	/**
	 * Invoke the display() method of the GLAutoDrawable to render all attached
	 * surfaces and perform the resource action. If an exception occurs while
	 * rendering, it must be re-thrown in this thread instead of interrupting
	 * the EDT thread.
	 */
	public void render(Action actions) throws RenderException;
}
