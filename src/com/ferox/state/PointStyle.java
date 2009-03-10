package com.ferox.state;

import org.openmali.vecmath.Vector3f;

/** A PointStyle controls the aspects of how all
 * point primitives are rendered.  This includes polygons
 * or lines rendered as points. 
 * 
 * Although PointStyle provides properties for vertex shader
 * size and point sprite control, these properties will be ignored
 * if the hardware cannot support that feature.
 * 
 * @author Michael Ludwig
 *
 */
public class PointStyle implements State {
	public static enum PointSpriteOrigin {
		UPPER_LEFT, LOWER_LEFT
	}
	
	private float pointSize; // must be >= 1f, will be adjusted by distAtten, clamped to be in [min/max], alpha'ed if to be under threshold
	private Vector3f distanceAttenuation;
	
	private boolean enablePointSprite; // will also enable tc gen
	private PointSpriteOrigin pointSpriteOrigin;
	private int pointSpriteTextureUnit;
	
	private boolean enableSmoothing;
	private boolean enableVertexShaderSize;
	
	private float pointSizeMin;
	private float pointSizeMax;
	
	private Object renderData;
	
	/** Construct a PointStyle with default values. */
	public PointStyle() {
		this.setPointSize(1f);
		this.setDistanceAttenuation(null);
		this.setMinMaxPointSize(1f, Float.MAX_VALUE);
		this.setPointSpriteOrigin(null);
		this.setPointSpriteTextureUnit(0);
		
		this.setSmoothingEnabled(false);
		this.setPointSpriteEnabled(false);
		this.setVertexShaderSizingEnabled(false);
	}
	
	/** Return true if the active vertex shader will determine
	 * the size of a rendered point, instead of using this style's
	 * point size value. */
	public boolean isVertexShaderSizingEnabled() {
		return this.enableVertexShaderSize;
	}
	
	/** Set whether or not to use a vertex shader specified point
	 * size (e.g. by the gl_PointSize variable inside the glsl program).
	 * 
	 * If there is no vertex shader active, this has no effect.  If there
	 * is a vertex shader active, then:
	 *  - true: Vertex shader must compute the derived_size (see setPointSize()).
	 *  	    This must be above 0, or undefined results will occur.
	 *  - false: This style's point size is used, but no distance attenuation is
	 *  		 performed on the point size.
	 */
	public void setVertexShaderSizingEnabled(boolean enable) {
		this.enableVertexShaderSize = enable;
	}

	/** Return the base, unmodified size of a rasterized point. */
	public float getPointSize() {
		return this.pointSize;
	}

	/** Set the starting size of a rasterized point.  This point
	 * will be affected by distance attenuation and the minimum
	 * and maximum sizes. 
	 * 
	 * Implementations also have a maximum supported size, and points
	 * will be implicitly clamped to be below that. 
	 * pointSize is clamped to be >= 1f. 
	 * 
	 * The final point size is computed as follows:
	 * 		- Let d = distance from vertex to eye.
	 * 		- Let a, b, and c = 3 components of the distance att. vector.
	 * 		- Let size = requested point size. 
	 * 	
	 * 		derived_size = clamp(size * sqrt(1 / (a + b*d + c*d*d))). 
	 * 			where clamp() clamps size between the min and max point sizes.
	 * 
	 * Also, if smoothing is enabled, the point's alpha will be faded if
	 * the multi-sampled size would go below the minimum size. */
	public void setPointSize(float pointSize) {
		this.pointSize = Math.max(1f, pointSize);
	}

	/** Get the distance attenuation that is applied to the requested
	 * point size. */
	public Vector3f getDistanceAttenuation() {
		return this.distanceAttenuation;
	}

	/** Set the distance attenuation that is applied to the base
	 * point size.  See setPointSize() for how the final point size
	 * is computed. 
	 * 
	 * If null is given, the attenuation is set to <1, 0, 0>. */
	public void setDistanceAttenuation(Vector3f distanceAttenuation) {
		if (distanceAttenuation == null) 
			distanceAttenuation = new Vector3f(1f, 0f, 0f);
		
		this.distanceAttenuation = distanceAttenuation;
	}

	/** Return whether or not the points will be rendered as a 
	 * bill-boarded quad facing the current view. */
	public boolean isPointSpriteEnabled() {
		return this.enablePointSprite;
	}
	
	/** Return the texture unit that the point sprite texture
	 * coordinates will be generated for. */
	public int getPointSpriteTextureUnit() {
		return this.pointSpriteTextureUnit;
	}
	
	/** Set the texture unit that will have texture coordinates
	 * generated across the bill-boarded quad face.  This should
	 * match the unit of the texture used in the point's appearance. 
	 * 
	 * It is clamped to be >= 0. */
	public void setPointSpriteTextureUnit(int unit) {
		this.pointSpriteTextureUnit = Math.max(0, unit);
	}

	/** Set whether or not to render points as standard points or
	 * as bill-boarded quads that face the viewer.
	 * 
	 * If true, then texture coordinates will be generated for each
	 * corner based on the value returned by getPointSpriteOrigin().
	 * 
	 * Point-sprites may not be supported on all hardware. If point
	 * sprites are enabled, then point smoothing is ignored. */
	public void setPointSpriteEnabled(boolean enablePointSprite) {
		this.enablePointSprite = enablePointSprite;
	}

	/** Get the origin location for generating texture coordinates
	 * across a point sprite quad. */
	public PointSpriteOrigin getPointSpriteOrigin() {
		return this.pointSpriteOrigin;
	}

	/** Set the origin location for generated texture coordinates.
	 * If null is used, it defaults to UPPER_LEFT. */
	public void setPointSpriteOrigin(PointSpriteOrigin pointSpriteOrigin) {
		if (pointSpriteOrigin == null)
			pointSpriteOrigin = PointSpriteOrigin.UPPER_LEFT;
		this.pointSpriteOrigin = pointSpriteOrigin;
	}

	/** Return whether or not points will be anti-aliased.  If they
	 * are anti-aliased, the actual size may be smaller than requested.
	 * The size will also be affected by the point fade threshold. */
	public boolean isSmoothingEnabled() {
		return this.enableSmoothing;
	}

	/** Set whether or not to use anti-aliasing when rendering the points. */
	public void setSmoothingEnabled(boolean enableSmoothing) {
		this.enableSmoothing = enableSmoothing;
	}

	/** Return the minimum size of the final point size when rendering. */
	public float getPointSizeMin() {
		return this.pointSizeMin;
	}

	/** Return the maximum size of the final point size when rendering.
	 * This will also be subject to the supported maximum point size. */
	public float getPointSizeMax() {
		return this.pointSizeMax;
	}
	
	/** Set the minimum and maximum point sizes for a rendered point.
	 * See setPointSize() for how the final point size is computed. 
	 * 
	 * Throws an exception if min < max.  Both min and max are clamped
	 * to be above 1. */
	public void setMinMaxPointSize(float min, float max) throws IllegalArgumentException {
		if (min >= max)
			throw new IllegalArgumentException("Cannot specify a minimum point distance that's less than the max: " + min + " " + max);
		this.pointSizeMin = Math.max(1f, min);
		this.pointSizeMax = Math.max(1f, max);
	}

	@Override
	public Role getRole() {
		return Role.POINT_DRAW_STYLE;
	}

	@Override
	public Object getStateData() {
		return this.renderData;
	}

	@Override
	public void setStateData(Object data) {
		this.renderData = data;
	}
}
