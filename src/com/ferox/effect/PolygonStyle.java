package com.ferox.effect;

import com.ferox.effect.EffectType.Type;

/**
 * <p>
 * PolygonStyle describes how a geometry's polygons are rendered. It gives the
 * ability to designate polygon winding and to set which faces to draw (and how
 * to draw them), and control polygon offset and anti-aliasing.
 * </p>
 * <p>
 * When controlling how a polygon is rendered, if a face is set to render with
 * lines or points, the rendering of those lines and points are controlled by
 * LineStyle and PointStyle states respectively.
 * </p>
 * 
 * @author Michael Ludwig
 */
@EffectType(Type.POLYGON)
public class PolygonStyle extends AbstractEffect {
	/**
	 * Represents the style of drawing to use. Depending on the primitive type
	 * being rendered, this may have no effect. Ex. lines will not be changed
	 * with a style of SOLID or LINE (POINT still does something). NONE
	 * designates that no polygon/shape should be drawn.
	 */
	public static enum DrawStyle {
		SOLID, LINE, POINT, NONE
	}

	/**
	 * Designates the winding of polygons, which is used to determine front
	 * facing or back facing for a given set of vertices.
	 */
	public static enum Winding {
		CLOCKWISE, COUNTER_CLOCKWISE
	}

	private static final DrawStyle DEFAULT_FRONT_STYLE = DrawStyle.SOLID;
	private static final DrawStyle DEFAULT_BACK_STYLE = DrawStyle.NONE;
	private static final Winding DEFAULT_WINDING = Winding.COUNTER_CLOCKWISE;

	private DrawStyle frontMode;
	private DrawStyle backMode;
	private Winding winding;

	private float offset;
	private boolean enableSmoothing;

	/** Creates a polygon style with default settings. */
	public PolygonStyle() {
		this(null, null);
	}

	/**
	 * Creates a polygon style with the given front and back styles, and default
	 * settings for everything else.
	 * 
	 * @param front DrawStyle used for front facing polygons
	 * @param back DrawStyle used for back facing polygons
	 */
	public PolygonStyle(DrawStyle front, DrawStyle back) {
		setBackStyle(back);
		setFrontStyle(front);
		setWinding(null);
		setDepthOffset(0f);
		setSmoothingEnabled(false);
	}

	/**
	 * <p>
	 * Enable or disable anti-aliasing on polygons faces that have a style of
	 * SOLID. For complex polygon surfaces, this may produce unusual results
	 * because of aliasing between edges of adjacent polygons.
	 * </p>
	 * <p>
	 * To control anti-aliasing for polygons rendered as points or lines, you'll
	 * need to use a LineStyle or PointStyle state.
	 * </p>
	 * 
	 * @param smooth If polygon edges are anti-aliased
	 */
	public void setSmoothingEnabled(boolean smooth) {
		enableSmoothing = smooth;
	}

	/**
	 * Return whether or not SOLID polygons rendered with this style will be
	 * anti-aliased. This is in addition to any fullscreen anti-aliasing
	 * effects. Default is false
	 * 
	 * @return True if polygons are smoothed
	 */
	public boolean isSmoothingEnabled() {
		return enableSmoothing;
	}

	/**
	 * Set the depth offset to use. This value will adjust the depths of all
	 * pixels that are rendered with this style amount.
	 * 
	 * @param offset Depth offset used when depth-testing and writing depths
	 */
	public void setDepthOffset(float offset) {
		this.offset = offset;
	}

	/**
	 * <p>
	 * Return the depth offset that adjusts the depths of all pixels rendered
	 * with this polygon style. This defaults to 0.
	 * </p>
	 * <p>
	 * This can be used to overlay decals on other polygons without z-fighting.
	 * </p>
	 * 
	 * @return The current depth offset
	 */
	public float getDepthOffset() {
		return offset;
	}

	/**
	 * Get the front style used by this draw mode.
	 * 
	 * @return DrawStyle used to render front polygons
	 */
	public DrawStyle getFrontStyle() {
		return frontMode;
	}

	/**
	 * Set the draw style to use for this draw mode. If front == NONE, then
	 * anything rendered with this mode will only show the back shell (assuming
	 * that that's not NONE either).
	 * 
	 * @param front DrawStyle for front facing polygons, null = SOLID
	 */
	public void setFrontStyle(DrawStyle front) {
		if (front == null)
			front = DEFAULT_FRONT_STYLE;
		frontMode = front;
	}

	/**
	 * Get the back style used by this draw mode.
	 * 
	 * @return DrawStyle for back facing polygons
	 */
	public DrawStyle getBackStyle() {
		return backMode;
	}

	/**
	 * Set the draw style to use for this draw mode. If back == NONE, then
	 * anything rendered with this mode will only show the front shell (assuming
	 * that that's not NONE either).
	 * 
	 * @param back DrawStyle for back facing polygons, null = NONE
	 */
	public void setBackStyle(DrawStyle back) {
		if (back == null)
			back = DEFAULT_BACK_STYLE;
		backMode = back;
	}

	/**
	 * Get the winding used to determine polygon orientation (front facing or
	 * back facing).
	 * 
	 * @return Winding of specified vertices in a polygon
	 */
	public Winding getWinding() {
		return winding;
	}

	/**
	 * Set the winding to use to determine polygon orientation.
	 * 
	 * @param winding Winding for vertices of a polygon, null =
	 *            COUNTER_CLOCKWISE
	 */
	public void setWinding(Winding winding) {
		if (winding == null)
			winding = DEFAULT_WINDING;
		this.winding = winding;
	}

	@Override
	public String toString() {
		return "(PolygonStyle front: " + frontMode + " back: " + backMode
			+ " winding: " + winding + " offset: " + offset + " smoothed: "
			+ enableSmoothing + ")";
	}
}
