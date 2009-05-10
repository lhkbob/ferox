package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GL;

import com.ferox.effect.PolygonStyle;
import com.ferox.effect.PolygonStyle.DrawStyle;
import com.ferox.effect.PolygonStyle.Winding;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.RasterizationRecord;

/**
 * This state driver can be used to fill the POLYGON_DRAW_STYLE role using
 * instances of PolygonStyle.
 * 
 * @author Michael Ludwig
 */
public class JoglPolygonStyleEffectDriver extends
	SingleEffectDriver<PolygonStyle> {
	public JoglPolygonStyleEffectDriver(JoglContextManager factory) {
		super(new PolygonStyle(), PolygonStyle.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, PolygonStyle nextState) {
		RasterizationRecord rr = record.rasterRecord;

		DrawStyle back = nextState.getBackStyle();
		DrawStyle front = nextState.getFrontStyle();
		int cullMode = 0; // 0 will imply disable culling
		if (front == DrawStyle.NONE)
			cullMode =
				(back == DrawStyle.NONE ? GL.GL_FRONT_AND_BACK : GL.GL_FRONT);
		else if (back == DrawStyle.NONE)
			cullMode =
				(front == DrawStyle.NONE ? GL.GL_FRONT_AND_BACK : GL.GL_BACK);

		if (cullMode != 0) {
			setCullingEnabled(gl, rr, true);
			if (cullMode != rr.cullFaceMode) {
				rr.cullFaceMode = cullMode;
				gl.glCullFace(cullMode);
			}

			if (cullMode != GL.GL_FRONT_AND_BACK) {
				if (cullMode == GL.GL_BACK)
					setFrontStyle(gl, rr, front);
				else
					setBackStyle(gl, rr, back);

				if (nextState.getWinding() == Winding.CLOCKWISE)
					setFrontFace(gl, rr, GL.GL_CW);
				else
					setFrontFace(gl, rr, GL.GL_CCW);
			} // else no face is showing, so we don't need to change anything
		} else {
			// both faces will be showing
			setCullingEnabled(gl, rr, false);
			setFrontStyle(gl, rr, front);
			setBackStyle(gl, rr, back);
		}

		if (cullMode != GL.GL_FRONT_AND_BACK) {
			// configure params if any face is visible
			setSmoothingEnabled(gl, rr, nextState.isSmoothingEnabled());

			float offset = nextState.getDepthOffset();
			if (offset == 0) {
				// disable the offsetting
				setPointOffsetEnabled(gl, rr, false);
				setLineOffsetEnabled(gl, rr, false);
				setFillOffsetEnabled(gl, rr, false);
			} else {
				// enable offsetting
				setPointOffsetEnabled(gl, rr, true);
				setLineOffsetEnabled(gl, rr, true);
				setFillOffsetEnabled(gl, rr, true);

				// set the offset
				if (rr.polygonOffsetFactor != offset
					|| rr.polygonOffsetUnits != 0) {
					rr.polygonOffsetFactor = offset;
					rr.polygonOffsetUnits = 0f; // use 0 for units, since it's
					// impl dependent
					gl.glPolygonOffset(offset, 0f);
				}
			}
		}
	}

	private static void setPointOffsetEnabled(GL gl, RasterizationRecord rr,
		boolean enabled) {
		if (rr.enablePolygonOffsetPoint != enabled) {
			rr.enablePolygonOffsetPoint = enabled;
			if (enabled)
				gl.glEnable(GL.GL_POLYGON_OFFSET_POINT);
			else
				gl.glDisable(GL.GL_POLYGON_OFFSET_POINT);
		}
	}

	private static void setLineOffsetEnabled(GL gl, RasterizationRecord rr,
		boolean enabled) {
		if (rr.enablePolygonOffsetLine != enabled) {
			rr.enablePolygonOffsetLine = enabled;
			if (enabled)
				gl.glEnable(GL.GL_POLYGON_OFFSET_LINE);
			else
				gl.glDisable(GL.GL_POLYGON_OFFSET_LINE);
		}
	}

	private static void setFillOffsetEnabled(GL gl, RasterizationRecord rr,
		boolean enabled) {
		if (rr.enablePolygonOffsetFill != enabled) {
			rr.enablePolygonOffsetFill = enabled;
			if (enabled)
				gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
			else
				gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
		}
	}

	private static void setFrontFace(GL gl, RasterizationRecord rr, int face) {
		if (rr.frontFace != face) {
			rr.frontFace = face;
			gl.glFrontFace(face);
		}
	}

	private static void setFrontStyle(GL gl, RasterizationRecord rr,
		DrawStyle style) {
		int mode = JoglUtil.getGLPolygonMode(style);
		if (mode != rr.polygonFrontMode) {
			rr.polygonFrontMode = mode;
			gl.glPolygonMode(GL.GL_FRONT, mode);
		}
	}

	private static void setBackStyle(GL gl, RasterizationRecord rr,
		DrawStyle style) {
		int mode = JoglUtil.getGLPolygonMode(style);
		if (mode != rr.polygonBackMode) {
			rr.polygonBackMode = mode;
			gl.glPolygonMode(GL.GL_BACK, mode);
		}
	}

	private static void setSmoothingEnabled(GL gl, RasterizationRecord rr,
		boolean enable) {
		if (rr.enablePolygonSmooth != enable) {
			rr.enablePolygonSmooth = enable;
			if (enable)
				gl.glEnable(GL.GL_POLYGON_SMOOTH);
			else
				gl.glDisable(GL.GL_POLYGON_SMOOTH);
		}
	}

	private static void setCullingEnabled(GL gl, RasterizationRecord rr,
		boolean enable) {
		if (rr.enableCullFace != enable) {
			rr.enableCullFace = enable;
			if (enable)
				gl.glEnable(GL.GL_CULL_FACE);
			else
				gl.glDisable(GL.GL_CULL_FACE);
		}
	}
}
