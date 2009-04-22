package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.RasterizationRecord;
import com.ferox.state.LineStyle;

/** This state driver provides a basic implementation for the LINE_DRAW_STYLE
 * role by using the LineStyle state class.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglLineDrawStyleStateDriver extends SingleStateDriver<LineStyle> {
	private static final short DEFAULT_PATTERN = (short) ~0;
	
	public JoglLineDrawStyleStateDriver(JoglSurfaceFactory factory) {
		super(new LineStyle(), LineStyle.class, factory);
	}
	
	@Override
	protected void restore(GL gl, JoglStateRecord record) {
		RasterizationRecord rr = record.rasterRecord;
		
		setSmoothingEnabled(gl, rr, false);
		if (rr.enableLineStipple) {
			rr.enableLineStipple = false;
			gl.glDisable(GL.GL_LINE_STIPPLE);
		}
		
		if (rr.lineStipplePattern != DEFAULT_PATTERN || rr.lineStippleRepeat != 1) {
			rr.lineStipplePattern = DEFAULT_PATTERN;
			rr.lineStippleRepeat = 1;
			gl.glLineStipple(1, DEFAULT_PATTERN);
		}
		
		if (rr.lineWidth != 1f) {
			rr.lineWidth = 1f;
			gl.glLineWidth(1f);
		}
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, LineStyle nextState) {
		RasterizationRecord rr = record.rasterRecord;
		
		// width
		float width = nextState.getLineWidth();
		if (rr.lineWidth != width) {
			rr.lineWidth = width;
			gl.glLineWidth(width);
		}
		// smoothing
		setSmoothingEnabled(gl, rr, nextState.isSmoothingEnable());
		// stippling
		if (nextState.isStipplingEnabled()) {
			// enable and configure it
			if (!rr.enableLineStipple) {
				rr.enableLineStipple = true;
				gl.glEnable(GL.GL_LINE_STIPPLE);
			}
			
			short pattern = nextState.getStipplePattern();
			int repeat = nextState.getStippleFactor();
			if (rr.lineStipplePattern != pattern || rr.lineStippleRepeat != repeat) {
				rr.lineStipplePattern = pattern;
				rr.lineStippleRepeat = repeat;
				gl.glLineStipple(repeat, pattern);
			}
		} else {
			// disable it
			if (rr.enableLineStipple) {
				rr.enableLineStipple = false;
				gl.glDisable(GL.GL_LINE_STIPPLE);
			}
		}
	}
	
	private static void setSmoothingEnabled(GL gl, RasterizationRecord rr, boolean enable) {
		if (rr.enableLineSmooth != enable) {
			rr.enableLineSmooth = enable;
			if (enable)
				gl.glEnable(GL.GL_LINE_SMOOTH);
			else
				gl.glDisable(GL.GL_LINE_SMOOTH);
		}
	}
}
