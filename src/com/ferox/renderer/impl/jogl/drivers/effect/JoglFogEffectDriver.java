package com.ferox.renderer.impl.jogl.drivers.effect;

import javax.media.opengl.GL;

import com.ferox.effect.Fog;
import com.ferox.math.Color4f;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.record.ColoringRecord;
import com.ferox.renderer.impl.jogl.record.HintRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/**
 * The EffectDriver that handles the Fog role. At the moment it only supports
 * instances of com.ferox.scene.Fog.
 * 
 * @author Michael Ludwig
 * 
 */
public class JoglFogEffectDriver extends SingleEffectDriver<Fog> {
	public JoglFogEffectDriver(JoglContextManager factory) {
		super(null, Fog.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, Fog nextState) {
		ColoringRecord cr = record.colorRecord;
		HintRecord hr = record.hintRecord;

		if (nextState == null) {
			setFogEnabled(gl, cr, false);
		} else {
			setFogEnabled(gl, cr, true);
			setRecord(gl, cr, hr, nextState.getColor(), nextState.getDensity(),
				nextState.getStartDistance(), nextState.getEndDistance(),
				JoglUtil.getGLFogMode(nextState.getEquation()), JoglUtil
				.getGLHint(nextState.getQuality()));
		}
	}
	
	private void setFogEnabled(GL gl, ColoringRecord cr, boolean enable) {
		if (cr.enableFog != enable) {
			cr.enableFog = enable;
			if (enable)
				gl.glEnable(GL.GL_FOG);
			else
				gl.glDisable(GL.GL_FOG);
		}
	}

	private void setRecord(GL gl, ColoringRecord cr, HintRecord hr,
			Color4f color, float density, float start, float end, int mode,
			int hint) {
		// color
		if (!JoglUtil.equals(color, cr.fogColor)) {
			JoglUtil.get(color, cr.fogColor);
			gl.glFogfv(GL.GL_FOG_COLOR, cr.fogColor, 0);
		}
		// density
		if (density != cr.fogDensity) {
			cr.fogDensity = density;
			gl.glFogf(GL.GL_FOG_DENSITY, cr.fogDensity);
		}
		// start
		if (start != cr.fogStart) {
			cr.fogStart = start;
			gl.glFogf(GL.GL_FOG_START, cr.fogStart);
		}
		// end
		if (end != cr.fogEnd) {
			cr.fogEnd = end;
			gl.glFogf(GL.GL_FOG_END, cr.fogEnd);
		}
		// mode
		if (mode != cr.fogMode) {
			cr.fogMode = mode;
			gl.glFogi(GL.GL_FOG_MODE, mode);
		}
		// hint
		if (hint != hr.fogHint) {
			hr.fogHint = hint;
			gl.glHint(GL.GL_FOG_HINT, hint);
		}
	}
}
