package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLContext;

/**
 * Common interface for either a pbuffer or onscreen shadow context.
 * 
 * @author Michael Ludwig
 */
public interface ShadowContext extends FrameworkGLEventListener {
	/**
	 * Return the GLContext that all render surfaces' glContexts must share
	 * resources with.
	 */
	public GLContext getContext();

	/** Clean up the shadow context resources so that it's no longer usable. */
	public void destroy();
}
