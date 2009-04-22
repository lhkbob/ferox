package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.math.Color;
import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.ColoringRecord;
import com.ferox.renderer.impl.jogl.record.HintRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.scene.Fog;
import com.ferox.scene.Fog.FogEquation;
import com.ferox.state.State.Quality;

/** The StateDriver that handles the Fog role.  At the moment it only
 * supports instances of com.ferox.scene.Fog.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglFogColorStateDriver extends SingleStateDriver<Fog> {
	private static final Color DEFAULT_COLOR = new Color(0f, 0f, 0f, 0f);
	
	public JoglFogColorStateDriver(JoglSurfaceFactory factory) {
		super(new Fog(new Color(), 0, 1, 1, FogEquation.EXP, Quality.DONT_CARE), Fog.class, factory);
	}
	
	@Override
	protected void restore(GL gl, JoglStateRecord record) {
		ColoringRecord cr = record.colorRecord;
		HintRecord hr = record.hintRecord;
		
		this.setRecord(gl, cr, hr, DEFAULT_COLOR, 1f, 0f, 1f, GL.GL_EXP, GL.GL_DONT_CARE);
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, Fog nextState) {
		ColoringRecord cr = record.colorRecord;
		HintRecord hr = record.hintRecord;
		
		this.setRecord(gl, cr, hr, nextState.getColor(), nextState.getDensity(), 
					   nextState.getStartDistance(), nextState.getEndDistance(), 
					   EnumUtil.getGLFogMode(nextState.getEquation()), 
					   EnumUtil.getGLHint(nextState.getQuality()));
	}
	
	private void setRecord(GL gl, ColoringRecord cr, HintRecord hr, Color color, float density, float start, float end, int mode, int hint) {
		// color
		if (!color.equals(cr.fogColor)) {
			color.get(cr.fogColor);
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
