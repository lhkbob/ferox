package com.ferox.renderer.impl.jogl.record;

import javax.media.opengl.GL;

/**
 * Class that encapsulates the state for the different pixel operations and
 * tests.
 * 
 * The actual box for the scissor's test is not included because tracking it
 * would be too difficult.
 * 
 * @author Michael Ludwig
 * 
 */
public class PixelOpRecord {
	public boolean enableScissorTest = false;

	public boolean enableAlphaTest = false;
	public int alphaTestFunc = GL.GL_ALWAYS;
	public float alphaTestRef = 0f;

	public boolean enableStencilTest = false;
	public int stencilFunc = GL.GL_ALWAYS;
	public int stencilValueMask = ~0;
	public int stencilRef = 0;
	public int stencilFail = GL.GL_KEEP;
	public int stencilPassDepthFail = GL.GL_KEEP;
	public int stencilPassDepthPass = GL.GL_KEEP;

	public int stencilBackFunc = GL.GL_ALWAYS;
	public int stencilBackValueMask = ~0;
	public int stencilBackRef = 0;
	public int stencilBackFail = GL.GL_KEEP;
	public int stencilBackPassDepthFail = GL.GL_KEEP;
	public int stencilBackPassDepthPass = GL.GL_KEEP;

	public boolean enableDepthTest = false;
	public int depthFunc = GL.GL_LESS;

	public boolean enableBlend = false;
	public int blendSrcRgb = GL.GL_ONE;
	public int blendSrcAlpha = GL.GL_ONE;
	public int blendDstRgb = GL.GL_ZERO;
	public int blendDstAlpha = GL.GL_ZERO;

	public int blendEquationRgb = GL.GL_FUNC_ADD;
	public int blendEquationAlpha = GL.GL_FUNC_ADD;
	public final float[] blendColor = { 0f, 0f, 0f, 0f };
}
