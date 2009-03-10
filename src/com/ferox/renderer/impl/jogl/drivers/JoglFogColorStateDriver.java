package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.math.Color;
import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.ColoringRecord;
import com.ferox.renderer.impl.jogl.record.HintRecord;
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
	public JoglFogColorStateDriver(JoglSurfaceFactory factory) {
		super(new Fog(new Color(), 0, 1, 1, FogEquation.EXP, Quality.DONT_CARE), Fog.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglContext context, Fog nextState) {
		ColoringRecord cr = context.getStateRecord().colorRecord;
		HintRecord hr = context.getStateRecord().hintRecord;
		
		// color
		if (!nextState.getColor().equals(cr.fogColor)) {
			nextState.getColor().get(cr.fogColor);
			gl.glFogfv(GL.GL_FOG_COLOR, cr.fogColor, 0);
		}
		// density
		if (nextState.getDensity() != cr.fogDensity) {
			cr.fogDensity = nextState.getDensity();
			gl.glFogf(GL.GL_FOG_DENSITY, cr.fogDensity);
		}
		// start
		if (nextState.getStartDistance() != cr.fogStart) {
			cr.fogStart = nextState.getStartDistance();
			gl.glFogf(GL.GL_FOG_START, cr.fogStart);
		}
		// end
		if (nextState.getEndDistance() != cr.fogEnd) {
			cr.fogEnd = nextState.getEndDistance();
			gl.glFogf(GL.GL_FOG_END, cr.fogEnd);
		}
		// mode
		int mode = EnumUtil.getGLFogMode(nextState.getEquation());
		if (mode != cr.fogMode) {
			cr.fogMode = mode;
			gl.glFogi(GL.GL_FOG_MODE, mode);
		}
		// hint
		int hint = EnumUtil.getGLHint(nextState.getQuality());
		if (hint != hr.fogHint) {
			hr.fogHint = hint;
			gl.glHint(GL.GL_FOG_HINT, hint);
		}
		// fog src
		if (cr.fogCoordSrc != GL.GL_FRAGMENT_DEPTH) {
			cr.fogCoordSrc = GL.GL_FRAGMENT_DEPTH;
			gl.glFogi(GL.GL_FOG_COORD_SRC, GL.GL_FRAGMENT_DEPTH);
		}
	}
}
