package com.ferox.renderer.impl.jogl.record;

import javax.media.opengl.GL2;

/**
 * Class that stores the state of how points, lines, and polygons are rendered.
 * Polygon stippling is assumed to not be in use, and JoglPolygonDrawStyle
 * assumes this. Care should be given if using polygon stippling.
 * 
 * @author Michael Ludwig
 */
public class RasterizationRecord {
	public float pointSize = 1f;
	public boolean enablePointSmooth = false;
	public boolean enablePointSprite = false;
	public boolean enableVertexShaderSize = false;

	public float pointSizeMin = 0f;
	public float pointSizeMax = -1f;
	/** This value is undefined until explicitly set. */
	public float pointFadeThresholdSize = 1f;
	public final float[] pointDistanceAttenuation = { 1f, 0f, 0f };
	public int pointSpriteOrigin = GL2.GL_UPPER_LEFT;

	public float lineWidth = 1f;
	public boolean enableLineSmooth = false;
	public boolean enableLineStipple = false;
	public short lineStipplePattern = ~0;
	public int lineStippleRepeat = 1;

	public boolean enableCullFace = false;
	public int cullFaceMode = GL2.GL_BACK;
	public int frontFace = GL2.GL_CCW;

	public boolean enablePolygonSmooth = false;
	public int polygonFrontMode = GL2.GL_FILL;
	public int polygonBackMode = GL2.GL_FILL;
	public float polygonOffsetFactor = 0f;
	public float polygonOffsetUnits = 0f;

	public boolean enablePolygonOffsetPoint = false;
	public boolean enablePolygonOffsetLine = false;
	public boolean enablePolygonOffsetFill = false;
}
