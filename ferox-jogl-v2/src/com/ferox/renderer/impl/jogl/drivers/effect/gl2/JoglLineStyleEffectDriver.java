package com.ferox.renderer.impl.jogl.drivers.effect.gl2;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;

import com.ferox.effect.LineStyle;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.drivers.effect.SingleEffectDriver;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.RasterizationRecord;

/**
 * This state driver provides a basic implementation for the LINE_DRAW_STYLE
 * role by using the LineStyle state class.
 * 
 * @author Michael Ludwig
 */
public class JoglLineStyleEffectDriver extends SingleEffectDriver<LineStyle, GL2> {
	public JoglLineStyleEffectDriver(JoglContextManager factory) {
		super(new LineStyle(), LineStyle.class, factory);
	}
	
	@Override
	public GL2 convert(GL2ES2 gl) {
		return gl.getGL2();
	}

	@Override
	protected void apply(GL2 gl, JoglStateRecord record, LineStyle nextState) {
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
				gl.glEnable(GL2.GL_LINE_STIPPLE);
			}

			short pattern = nextState.getStipplePattern();
			int repeat = nextState.getStippleFactor();
			if (rr.lineStipplePattern != pattern || rr.lineStippleRepeat != repeat) {
				rr.lineStipplePattern = pattern;
				rr.lineStippleRepeat = repeat;
				gl.glLineStipple(repeat, pattern);
			}
		} else // disable it
		if (rr.enableLineStipple) {
			rr.enableLineStipple = false;
			gl.glDisable(GL2.GL_LINE_STIPPLE);
		}
	}

	private void setSmoothingEnabled(GL gl, RasterizationRecord rr, boolean enable) {
		if (rr.enableLineSmooth != enable) {
			rr.enableLineSmooth = enable;
			if (enable)
				gl.glEnable(GL2.GL_LINE_SMOOTH);
			else
				gl.glDisable(GL2.GL_LINE_SMOOTH);
		}
	}
}
