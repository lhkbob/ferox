package com.ferox.renderer.impl.jogl.record;

import javax.media.opengl.GL2;

/**
 * Class representing the usable state for fog and coloring.
 * 
 * @author Michael Ludwig
 */
public class ColoringRecord {
	/* Fog coloring. */
	public final float[] fogColor = { 0f, 0f, 0f, 0f };
	public float fogDensity = 1f;
	public float fogStart = 0f;
	public float fogEnd = 1f;
	public int fogMode = GL2.GL_EXP;
	public boolean enableFog = false;
	public int fogCoordSrc = GL2.GL_FRAGMENT_DEPTH;

	/* Shade model. */
	public int shadeModel = GL2.GL_SMOOTH;
}
