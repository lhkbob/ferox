package com.ferox.effect;

/**
 * <p>
 * PrimitiveRenderStyle contains a collection of rendering parameters that
 * modify how primitives are rendered. Here, a primitive refers to a point, line
 * or polygon that's rendered from a geometry. The PrimitiveRenderStyle controls
 * sizing parameters for points and lines, anti-aliasing controls for all
 * primitive types. This anti-aliasing is done separately from fullscreen
 * anti-aliasing and only affects primitives rendered with it enabled; it can be
 * used in a RenderSurface that does not have FSAA enabled.
 * </p>
 * <p>
 * Lastly, PrimitiveRenderStyle controls the depth offset of a rendered
 * primitive. If depth offsets are enabled, the primitive's depth is modified by
 * small, fractional amounts. This can be used when rendering the same geometry
 * multiple times to prevent z-fighting between the different layers.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class PrimitiveRenderStyle {
	private boolean offsetEnabled;
	private float offsetFactor;
	private float offsetUnits;
	
	private float pointSize;
	private float lineSize;
	
	private boolean pointSmoothing;
	private boolean lineSmoothing;
	private boolean polySmoothing;

	/**
	 * Create a new PrimitiveRenderStyle that has an offset factor of 0, offset
	 * units of 0, a width of 1, not anti-aliasing, and depth offsets are
	 * disabled.
	 */
	public PrimitiveRenderStyle() {
		offsetEnabled = false;
		offsetFactor = 0f;
		offsetUnits = 0f;
		
		setWidth(1f);
		setAntiAliasingEnabled(false);
	}

	/**
	 * Get the offset factor to be applied to polygons, lines, and points
	 * rendered with this PrimitiveRenderStyle if depth offsets are enabled.
	 * 
	 * @return The offset factor to use
	 */
	public float getOffsetFactor() {
		return offsetFactor;
	}

	/**
	 * Get the offset units used when computing the final depth offset for
	 * rendered primitives. This works in conjunction with the offset factor,
	 * and only applies if depth offsets are enabled.
	 * 
	 * @return The offset units to use
	 */
	public float getOffsetUnits() {
		return offsetUnits;
	}

	/**
	 * Set the offset factor and offset units to use with the depth offset. The
	 * exact effect of the factor and units are dependent on the graphics card
	 * and OpenGL drivers used on the current machine. These values will have no
	 * effect unless depth offsets are enabled with
	 * {@link #setOffsetEnabled(boolean)}.
	 * 
	 * @param factor The new offset factor to use
	 * @param units The new offset units to use
	 * @return This PrimitiveRenderStyle
	 */
	public PrimitiveRenderStyle setOffset(float factor, float units) {
		offsetFactor = factor;
		offsetUnits = units;
		
		return this;
	}

	/**
	 * Return whether or not depth offsets are enabled. If this returns true
	 * then the offset factor and units will modify the final depth value of a
	 * rendered primitive. This can be used to create billboarded polygons that
	 * don't z-fight with previously rendered surfaces.
	 * 
	 * @return True if depth offsets are enabled
	 */
	public boolean getOffsetEnabled() {
		return offsetEnabled;
	}

	/**
	 * Set whether or not depth offsets are enabled. See
	 * {@link #getOffsetEnabled()}, {@link #getOffsetFactor()}, and
	 * {@link #getOffsetUnits()} for a description of what depth offsets
	 * accomplishes.
	 * 
	 * @param enabled True if depth offsets should be enabled
	 * @return This PrimitiveRenderStyle
	 */
	public PrimitiveRenderStyle setOffsetEnabled(boolean enabled) {
		offsetEnabled = enabled;
		
		return this;
	}

	/**
	 * Set both the line width and the point width to the given value. This
	 * float value must be at least 1, and is in pixels. Width values with
	 * fractional values will appear more correct when anti-aliasing is enabled
	 * for the primitive type.
	 * 
	 * @param width The new point and line width
	 * @return This PrimitiveRenderStyle
	 * @throws IllegalArgumentException if width < 1
	 */
	public PrimitiveRenderStyle setWidth(float width) {
		return setLineWidth(width).setPointWidth(width);
	}

	/**
	 * Return the pixel width that lines will be rendered in when using this
	 * PrimitiveRenderStyle. If line anti-aliasing is disabled, the actual
	 * rendered width may not always equal this width because it has to
	 * rasterize the line onto discrete samples.
	 * 
	 * @return The line width
	 */
	public float getLineWidth() {
		return lineSize;
	}

	/**
	 * Set the line width of rendered lines for this PrimitiveRenderStyle. This
	 * width must be at least 1.
	 * 
	 * @param width The new line width
	 * @return This PrimitiveRenderStyle
	 * @throws IllegalArgumentException if width < 1
	 */
	public PrimitiveRenderStyle setLineWidth(float width) {
		if (width < 1f)
			throw new IllegalArgumentException("Width must be at least 1");
		lineSize = width;
		return this;
	}

	/**
	 * <p>
	 * Return the pixel width that points will be rendered in when using this
	 * PrimitiveRenderStyle. If point anti-aliasing is disabled, the actual
	 * rendered width may not always equal this width because it has to
	 * rasterize the line onto discrete samples.
	 * </p>
	 * <p>
	 * When anti-aliasing is enabled, the rendered point will appear as a circle
	 * of this width. Without anti-aliasing, the rendered point is a square.
	 * When point sprites are used, the point width represents the side length
	 * of the point sprite quad rendered automatically.
	 * </p>
	 * 
	 * @return The point width
	 */
	public float getPointWidth() {
		return pointSize;
	}
	
	/**
	 * Set the point width of rendered points for this PrimitiveRenderStyle. This
	 * width must be at least 1.
	 * 
	 * @param width The new point width
	 * @return This PrimitiveRenderStyle
	 * @throws IllegalArgumentException if width < 1
	 */
	public PrimitiveRenderStyle setPointWidth(float width) {
		if (width < 1f)
			throw new IllegalArgumentException("Width must be at least 1");
		pointSize = width;
		return this;
	}

	/**
	 * Set whether or not primitive anti-aliasing is enabled for points, lines,
	 * and polygons. Note that this anti-aliasing is different than fullscreen
	 * anti-aliasing that's applied via a RenderSurface.
	 * 
	 * @param enabled True if primitives should be smoothed
	 * @return This PrimitiveRenderStyle
	 */
	public PrimitiveRenderStyle setAntiAliasingEnabled(boolean enabled) {
		return setAntiAliasingEnabled(enabled, enabled, enabled);
	}

	/**
	 * Return whether or not polygon edges are anti-aliased. For complex models,
	 * this should generally disabled since the edges of adjacent triangles will
	 * appear to pull away from each other.
	 * 
	 * @return True if polygon anti-aliasing is enabled
	 */
	public boolean getPolygonAntiAliasingEnabled() {
		return polySmoothing;
	}
	
	/**
	 * Return whether or not points are anti-aliased. When points are anti-aliased,
	 * they appear as circles instead of squares.  If point sprites are enabled, point
	 * anti-aliasing is ignored.
	 * 
	 * @return True if point anti-aliasing is enabled
	 */
	public boolean getPointAntiAliasingEnabled() {
		return pointSmoothing;
	}
	
	/**
	 * Return whether or not lines are anti-aliased. 
	 * 
	 * @return True if line anti-aliasing is enabled
	 */
	public boolean getLineAntiAliasingEnabled() {
		return lineSmoothing;
	}

	/**
	 * Set whether or not polygons, lines and points are anti-aliased. This
	 * performs the same function as {@link #setAntiAliasingEnabled(boolean)},
	 * except it allows for greater granularity by primitive type.
	 * 
	 * @param polys Enabled boolean for polygons
	 * @param lines Enabled boolean for lines
	 * @param points Enabled boolean for points
	 * @return This PrimitiveRenderStyle
	 */
	public PrimitiveRenderStyle setAntiAliasingEnabled(boolean polys, boolean lines, 
													   boolean points) {
		polySmoothing = polys;
		lineSmoothing = lines;
		pointSmoothing = points;
		
		return this;
	}
}
