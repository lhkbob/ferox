package com.ferox.effect;

import com.ferox.effect.Effect.Type;
import com.ferox.math.Vector3f;


/**
 * <p>
 * A PointStyle controls the aspects of how all point primitives are rendered.
 * This includes polygons or lines rendered as points.
 * </p>
 * <p>
 * Although PointStyle provides properties for vertex shader size and point
 * sprite control, these properties will be ignored if the hardware cannot
 * support that feature.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Type(EffectType.POINT)
public class PointStyle extends AbstractEffect {
	public static enum PointSpriteOrigin {
		UPPER_LEFT, LOWER_LEFT
	}

	private float pointSize; // must be >= 1f, will be adjusted by distAtten,
	// clamped to be in [min/max], alpha'ed if to be
	// under threshold
	private Vector3f distanceAttenuation;

	private boolean enablePointSprite; // will also enable tc gen
	private PointSpriteOrigin pointSpriteOrigin;
	private int pointSpriteTextureUnit;

	private boolean enableSmoothing;
	private boolean enableVertexShaderSize;

	private float pointSizeMin;
	private float pointSizeMax;

	/** Construct a PointStyle with default values. */
	public PointStyle() {
		setPointSize(1f);
		setDistanceAttenuation(null);
		setMinMaxPointSize(1f, Float.MAX_VALUE);
		setPointSpriteOrigin(null);
		setPointSpriteTextureUnit(0);

		setSmoothingEnabled(false);
		setPointSpriteEnabled(false);
		setVertexShaderSizingEnabled(false);
	}

	/**
	 * Return true if the active vertex shader will determine the size of a
	 * rendered point, instead of using this style's point size value. Default
	 * is false.
	 * 
	 * @return True if shader is responsible for point size
	 */
	public boolean isVertexShaderSizingEnabled() {
		return enableVertexShaderSize;
	}

	/**
	 * <p>
	 * Set whether or not to use a vertex shader specified point size (e.g. by
	 * the gl_PointSize variable inside the glsl program).
	 * </p>
	 * <p>
	 * If there is no vertex shader active, this has no effect. If there is a
	 * vertex shader active, then: - true: Vertex shader must compute the
	 * derived_size (see setPointSize()). This must be above 0, or undefined
	 * results will occur. - false: This style's point size is used, but no
	 * distance attenuation is performed on the point size.
	 * </p>
	 * 
	 * @param enable True if shader assigns point size
	 */
	public void setVertexShaderSizingEnabled(boolean enable) {
		enableVertexShaderSize = enable;
	}

	/**
	 * Return the base, unmodified size of a rasterized point. Default is 1.
	 * 
	 * @see #setPointSize(float)
	 * @return Size of rendered points
	 */
	public float getPointSize() {
		return pointSize;
	}

	/**
	 * <p>
	 * Set the starting size of a rasterized point. This point will be affected
	 * by distance attenuation and the minimum and maximum sizes.
	 * Implementations also have a maximum supported size, and points will be
	 * implicitly clamped to be below that. pointSize is clamped to be >= 1f.
	 * </p>
	 * <p>
	 * The final point size is computed as follows: - Let d = distance from
	 * vertex to eye. - Let a, b, and c = 3 components of the distance att.
	 * vector. - Let size = requested point size.
	 * </p>
	 * <p>
	 * derived_size = clamp(size * sqrt(1 / (a + b*d + c*d*d))). where clamp()
	 * clamps size between the min and max point sizes.
	 * </p>
	 * <p>
	 * Also, if smoothing is enabled, the point's alpha will be faded if the
	 * multi-sampled size would go below the minimum size.
	 * </p>
	 * 
	 * @param pointSize Unmodified point size, clamped to be above 1
	 */
	public void setPointSize(float pointSize) {
		this.pointSize = Math.max(1f, pointSize);
	}

	/**
	 * Get the distance attenuation that is applied to the requested point size.
	 * Default is <1, 0, 0>.
	 * 
	 * @see #setPointSize(float)
	 * @return The 3 distance attenuation factors, not null
	 */
	public Vector3f getDistanceAttenuation() {
		return distanceAttenuation;
	}

	/**
	 * Set the distance attenuation that is applied to the base point size. See
	 * setPointSize() for how the final point size is computed.
	 * 
	 * @see #setPointSize(float)
	 * @param distanceAttenuation Distance attenuation <a, b, c> to use, null =
	 *            <1, 0, 0>
	 */
	public void setDistanceAttenuation(Vector3f distanceAttenuation) {
		if (distanceAttenuation == null)
			distanceAttenuation = new Vector3f(1f, 0f, 0f);

		this.distanceAttenuation = distanceAttenuation;
	}

	/**
	 * Return whether or not the points will be rendered as a bill-boarded quad
	 * facing the current view. Default is false
	 * 
	 * @return If points are converted to quads
	 */
	public boolean isPointSpriteEnabled() {
		return enablePointSprite;
	}

	/**
	 * Return the texture unit that the point sprite texture coordinates will be
	 * generated for. Default is 0
	 * 
	 * @return Texture unit used for generated coordinates
	 */
	public int getPointSpriteTextureUnit() {
		return pointSpriteTextureUnit;
	}

	/**
	 * Set the texture unit that will have texture coordinates generated across
	 * the bill-boarded quad face. This should match the unit of the texture
	 * used in the point's appearance.
	 * 
	 * @param unit Texture coordinate unit, clamped to be >= 0
	 */
	public void setPointSpriteTextureUnit(int unit) {
		pointSpriteTextureUnit = Math.max(0, unit);
	}

	/**
	 * Set whether or not to render points as standard points or as bill-boarded
	 * quads that face the viewer. If true, then texture coordinates will be
	 * generated for each corner based on the value returned by
	 * getPointSpriteOrigin(). Point-sprites may not be supported on all
	 * hardware. If point sprites are enabled, then point smoothing is ignored.
	 * 
	 * @param enablePointSprite Whether or not point sprites are used
	 */
	public void setPointSpriteEnabled(boolean enablePointSprite) {
		this.enablePointSprite = enablePointSprite;
	}

	/**
	 * Get the origin location for generating texture coordinates across a point
	 * sprite quad. Default is UPPER_LEFT.
	 * 
	 * @return Origin used for point sprites
	 */
	public PointSpriteOrigin getPointSpriteOrigin() {
		return pointSpriteOrigin;
	}

	/**
	 * Set the origin location for generated texture coordinates.
	 * 
	 * @param pointSpriteOrigin Origin for point sprites, null = UPPER_LEFT
	 */
	public void setPointSpriteOrigin(PointSpriteOrigin pointSpriteOrigin) {
		if (pointSpriteOrigin == null)
			pointSpriteOrigin = PointSpriteOrigin.UPPER_LEFT;
		this.pointSpriteOrigin = pointSpriteOrigin;
	}

	/**
	 * Return whether or not points will be anti-aliased. If they are
	 * anti-aliased, the actual size may be smaller than requested. The size
	 * will also be affected by the point fade threshold. Default is false.
	 * 
	 * @return True if points are smoothed
	 */
	public boolean isSmoothingEnabled() {
		return enableSmoothing;
	}

	/**
	 * Set whether or not to use anti-aliasing when rendering the points.
	 * 
	 * @param enableSmoothing Enable point anti-aliasing
	 */
	public void setSmoothingEnabled(boolean enableSmoothing) {
		this.enableSmoothing = enableSmoothing;
	}

	/**
	 * Return the minimum size of the final point size when rendering. Default
	 * is 1.
	 * 
	 * @return Minimum size threshold
	 */
	public float getPointSizeMin() {
		return pointSizeMin;
	}

	/**
	 * Return the maximum size of the final point size when rendering. This will
	 * also be subject to the supported maximum point size. Default is
	 * Float.MAX_VALUE.
	 * 
	 * @return Maximum clamped point size
	 */
	public float getPointSizeMax() {
		return pointSizeMax;
	}

	/**
	 * Set the minimum and maximum point sizes for a rendered point.
	 * 
	 * @see #setPointSize(float)
	 * @param min Minimum point size, clamped to be above 1
	 * @param max Maximum point size, clamped to be above 1
	 * @throws IllegalArgumentException if min > max
	 */
	public void setMinMaxPointSize(float min, float max) {
		if (min > max)
			throw new IllegalArgumentException(
				"Cannot specify a minimum point distance that's less than the max: "
					+ min + " " + max);
		pointSizeMin = Math.max(1f, min);
		pointSizeMax = Math.max(1f, max);
	}

	@Override
	public String toString() {
		return "(" + super.toString() + " size: " + pointSize + ")";
	}
}
