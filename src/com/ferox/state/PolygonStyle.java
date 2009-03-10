package com.ferox.state;


/** PolygonStyle describes how a geometry's polygons
 * are rendered.  It gives the ability to designate polygon winding
 * and to set which faces to draw (and how to draw them), and control
 * polygon offset and anti-aliasing.
 * 
 * When controlling how a polygon is rendered, if a face is
 * set to render with lines or points, the rendering of those lines and points
 * are controlled by LineStyle and PointStyle states respectively.
 * 
 * @author Michael Ludwig
 *
 */
public class PolygonStyle implements State {
	/** Represents the style of drawing to use.  Depending on the 
	 * primitive type being rendered, this may have no effect.
	 * Ex. lines will not be changed with a style of SOLID or LINE (POINT still does something). 
	 * NONE designates that no polygon/shape should be drawn. */
	public static enum DrawStyle {
		SOLID, LINE, POINT, NONE
	}
	
	/** Designates the winding of polygons, which is used to determine front
	 * facing or back facing for a given set of vertices. */
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
	
	private Object renderData;
	
	/** Creates a polygon style with default settings. */
	public PolygonStyle() {
		this.setBackStyle(null);
		this.setFrontStyle(null);
		this.setWinding(null);
		this.setDepthOffset(0f);
		this.setSmoothingEnabled(false);
	}
	
	/** Enable or disable anti-aliasing on polygons faces that have
	 * a style of SOLID.  For complex polygon surfaces, this may
	 * produce unusual results because of aliasing between edges of
	 * adjacent polygons.
	 * 
	 * To control anti-aliasing for polygons rendered as points or lines,
	 * you'll need to use a LineStyle or PointStyle state. */
	public void setSmoothingEnabled(boolean smooth) {
		this.enableSmoothing = smooth;
	}
	
	/** Return whether or not SOLID polygons rendered with this
	 * style will be anti-aliased.  This is in addition to any 
	 * fullscreen anti-aliasing effects. */
	public boolean isSmoothingEnabled() {
		return this.enableSmoothing;
	}
	
	/** Set the depth offset to use.  This value will adjust
	 * the depths of all pixels that are rendered with this style
	 * amount. */
	public void setDepthOffset(float offset) {
		this.offset = offset;
	}
	
	/** Return the depth offset that adjusts the depths of all pixels
	 * rendered with this polygon style.  This defaults to 0.
	 * 
	 * This can be used to properly overlay decals on other polygons
	 * without z-fighting. */
	public float getDepthOffset() {
		return this.offset;
	}

	/** Get the front style used by this draw mode. */
	public DrawStyle getFrontStyle() {
		return this.frontMode;
	}

	/** Set the draw style to use for this draw mode.  If front == NONE,
	 * then anything rendered with this mode will only show the back shell
	 * (assuming that that's not NONE either). If front == null, uses default. */
	public void setFrontStyle(DrawStyle front) {
		if (frontMode == null)
			front = DEFAULT_FRONT_STYLE;
		this.frontMode = front;
	}

	/** Get the back style used by this draw mode. */
	public DrawStyle getBackStyle() {
		return this.backMode;
	}

	/** Set the draw style to use for this draw mode.  If back == NONE,
	 * then anything rendered with this mode will only show the front shell
	 * (assuming that that's not NONE either).  If back == null, uses default. */
	public void setBackStyle(DrawStyle back) {
		if (back == null)
			back = DEFAULT_BACK_STYLE;
		this.backMode = back;
	}

	/** Get the winding used to determine polygon orientation (front facing or
	 * back facing). */
	public Winding getWinding() {
		return this.winding;
	}

	/** Set the winding to use to determine polygon orientation. 
	 * If winding is null, uses default. */
	public void setWinding(Winding winding) {
		if (winding == null)
			winding = DEFAULT_WINDING;
		this.winding = winding;
	}

	@Override
	public Role getRole() {
		return Role.POLYGON_DRAW_STYLE;
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
