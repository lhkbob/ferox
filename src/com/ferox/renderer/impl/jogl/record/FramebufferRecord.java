package com.ferox.renderer.impl.jogl.record;


/** This class encapsulates the parts of the OpenGl state
 * record pertaining to framebuffer operations.
 * 
 * @author Michael Ludwig
 *
 */
public class FramebufferRecord {
	public final float[] clearColor = {0f, 0f, 0f, 0f};
	public float clearDepth = 1f;
	public int clearStencil = 0;
	
	public final boolean[] colorWriteMask = {true, true, true, true};
	public boolean depthWriteMask = true;
	public int stencilWriteMask = ~0;
	public int stencilBackWriteMask = ~0;
	
	public int drawFramebufferBinding = 0;
	public int readFramebufferBinding = 0;
}
