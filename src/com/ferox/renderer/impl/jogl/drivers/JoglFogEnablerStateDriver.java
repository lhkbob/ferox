package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.ColoringRecord;
import com.ferox.state.FogReceiver;

/** This simple class enables or disables fog based on the presence of
 * FogReceiver (the default implementation for the FogEnabler role).
 * 
 * @author Michael Ludwig
 *
 */
public class JoglFogEnablerStateDriver extends SingleStateDriver<FogReceiver> {
	public JoglFogEnablerStateDriver(JoglSurfaceFactory factory) {
		super(null, FogReceiver.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglContext context, FogReceiver nextState) {
		ColoringRecord cr = context.getStateRecord().colorRecord;
		setFogEnabled(gl, cr, nextState != null);
		
		if (nextState != null) {
			// set the fog coordinate source
			int src = EnumUtil.getGLFogCoordSrc(nextState.getFogCoordinateSource());
			if (cr.fogCoordSrc != src) {
				cr.fogCoordSrc = src;
				gl.glFogi(GL.GL_FOG_COORD_SRC, src);
			}
		}
	}
	
	private static void setFogEnabled(GL gl, ColoringRecord cr, boolean enable) {
		if (cr.enableFog != enable) {
			cr.enableFog = enable;
			if (enable)
				gl.glEnable(GL.GL_FOG);
			else
				gl.glDisable(GL.GL_FOG);
		}
	}
}
