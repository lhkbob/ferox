package com.ferox.state;

/** LineStyle controls how line segments are rendered.
 * These lines could have been produced by geometry that
 * directly renders lines, or because a PolygonStyle was
 * set to render certain faces as lines.
 * 
 * It can control width, stippling, and anti-aliasing of lines.
 * 
 * @author Michael Ludwig
 *
 */
public class LineStyle implements State {
	private boolean enableSmoothing;
	private boolean enableStippling;
	private short stipplePattern;
	private int stippleFactor;
	private float lineWidth;
	
	private Object renderData;
	
	/** Construct a LineStyle with no smoothing or stippling.  The
	 * initial pattern is all 1s with a factor of 1.  The line width is 
	 * also 1. */
	public LineStyle() {
		this.setSmoothingEnabled(false);
		this.setStipplingEnabled(false);
		this.setStipplePattern((short) ~0);
		this.setStippleFactor(1);
		this.setLineWidth(1f);
	}

	/** Return whether or not lines rendered with this style should
	 * be anti-aliased.  If they are anti-aliased, their width may 
	 * appear slightly smaller than the requested pixel width. */
	public boolean isSmoothingEnable() {
		return this.enableSmoothing;
	}

	/** Set whether or not line smoothing should be enabled. */
	public void setSmoothingEnabled(boolean enableSmoothing) {
		this.enableSmoothing = enableSmoothing;
	}

	/** Return whether or not lines rendered with this style
	 * will be stippled, based on the pattern and factor.
	 * 
	 * See setStipplePattern(). */
	public boolean isStipplingEnabled() {
		return this.enableStippling;
	}

	/** Set whether or not lines should be stippled, based
	 * on the set pattern and factor. */
	public void setStipplingEnabled(boolean enableStippling) {
		this.enableStippling = enableStippling;
	}

	/** Return the 16 bit stipple pattern that is used if
	 * isStipplingEnabled() returns true. */
	public short getStipplePattern() {
		return this.stipplePattern;
	}

	/** Set the stipple pattern to use when rendering lines
	 * of this style.  It will have no effect until stippling
	 * is also enabled with setStipplingEnabled(true).
	 * 
	 * When rendering a line, each pixel on the line has an
	 * associated value, s, representing it's position (starting at 0).
	 * A pixel on the line is visible if the (floor(s / factor) % 16)th
	 * bit of pattern is a 1.
	 * 
	 * Therefore all 1s and a factor of 1 is the same result as 
	 * disabling stippling. */
	public void setStipplePattern(short stipplePattern) {
		this.stipplePattern = stipplePattern;
	}

	/** Return the stipple factor used when stippling. */
	public int getStippleFactor() {
		return this.stippleFactor;
	}

	/** Set the stipple factor to use when stippling.
	 * This is clamped to be in [1, 256]. */
	public void setStippleFactor(int stippleRepeat) {
		this.stippleFactor = Math.max(1, Math.min(stippleRepeat, 256));
	}

	/** Return the line width, in pixels that each rendered line
	 * will take up.  Lines will be slightly thinner when they are
	 * anti-aliased. */
	public float getLineWidth() {
		return this.lineWidth;
	}

	/** Set the line width to use when rendering lines.  This
	 * will be clamped to be >= 1f.  When actually rendering
	 * lines, the hardware will clamp the width to some implementation
	 * specific width. */
	public void setLineWidth(float lineWidth) {
		this.lineWidth = Math.max(1f, lineWidth);
	}

	@Override
	public Role getRole() {
		return Role.LINE_DRAW_STYLE;
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
