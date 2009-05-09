package com.ferox.renderer.impl.jogl.record;

import javax.media.opengl.GL;

/**
 * A record that holds onto the allowed hints that can be set for a context. Not
 * all of these hints are actively used, but they are included for completion.
 * 
 * @author Michael Ludwig
 * 
 */
public class HintRecord {
	public int perspectiveCorrectionHint = GL.GL_DONT_CARE;
	public int pointSmoothHint = GL.GL_DONT_CARE;
	public int lineSmoothHint = GL.GL_DONT_CARE;
	public int polySmoothHint = GL.GL_DONT_CARE;
	public int fogHint = GL.GL_DONT_CARE;
	public int generateMipmapHint = GL.GL_DONT_CARE;
	public int textureCompressionHint = GL.GL_DONT_CARE;
	public int fragShaderDerivativeHint = GL.GL_DONT_CARE;
}
