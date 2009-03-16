package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.math.Color;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.LightingRecord;
import com.ferox.state.LightReceiver;

/** This state driver provides global lighting control by using the LightReceiver
 * class.  When no lighting is needed, it just disables lighting.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglGlobalLightingStateDriver extends SingleStateDriver<LightReceiver> {
	public JoglGlobalLightingStateDriver(JoglSurfaceFactory factory) {
		super(null, LightReceiver.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, LightReceiver nextState) {
		LightingRecord lr = record.lightRecord;
		
		if (nextState == null) {
			// just need to disable lighting
			if (lr.enableLighting) {
				lr.enableLighting = false;
				gl.glDisable(GL.GL_LIGHTING);
			}
		} else {
			// configure and enable lighting
			if (!lr.enableLighting) {
				lr.enableLighting = true;
				gl.glEnable(GL.GL_LIGHTING);
			}
			
			// ambient color
			Color c = nextState.getGlobalAmbient();
			if (!c.equals(lr.lightModelAmbient)) {
				c.get(lr.lightModelAmbient);
				gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, lr.lightModelAmbient, 0);
			}
			// local viewer
			if (nextState.isLocalViewer() != lr.lightModelLocalViewer) {
				lr.lightModelLocalViewer = nextState.isLocalViewer();
				gl.glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER, (lr.lightModelLocalViewer ? GL.GL_TRUE : GL.GL_FALSE));
			}
			// separate specular
			int control = (nextState.getSeparateSpecular() ? GL.GL_SEPARATE_SPECULAR_COLOR : GL.GL_SINGLE_COLOR);
			if (control != lr.lightModelColorControl) {
				lr.lightModelColorControl = control;
				gl.glLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL, control);
			}
			// two-sided lighting
			if (nextState.getTwoSidedLighting() != lr.lightModelTwoSided) {
				lr.lightModelTwoSided = nextState.getTwoSidedLighting();
				gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, (lr.lightModelTwoSided ? GL.GL_TRUE : GL.GL_FALSE));
			}
		}
	}
}
