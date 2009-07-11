package com.ferox.renderer.impl.jogl.drivers.effect.gl2;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;

import com.ferox.effect.GlobalLighting;
import com.ferox.math.Color4f;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.drivers.effect.SingleEffectDriver;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.LightingRecord;

/**
 * This state driver provides global lighting control by using the
 * GlobalLighting class. When no lighting is needed, it just disables lighting.
 * 
 * @author Michael Ludwig
 */
public class JoglGlobalLightingEffectDriver extends SingleEffectDriver<GlobalLighting, GL2> {

	public JoglGlobalLightingEffectDriver(JoglContextManager factory) {
		super(null, GlobalLighting.class, factory);
	}
	
	@Override
	public GL2 convert(GL2ES2 gl) {
		return gl.getGL2();
	}

	@Override
	protected void apply(GL2 gl, JoglStateRecord record, GlobalLighting nextState) {
		LightingRecord lr = record.lightRecord;

		if (nextState == null) {
			// just need to disable lighting
			if (lr.enableLighting) {
				lr.enableLighting = false;
				gl.glDisable(GL2.GL_LIGHTING);
			}
		} else {
			// configure and enable lighting
			if (!lr.enableLighting) {
				lr.enableLighting = true;
				gl.glEnable(GL2.GL_LIGHTING);
			}

			setLightModel(gl, lr, nextState.getGlobalAmbient(), nextState.isLocalViewer(), 
						  (nextState.getSeparateSpecular() ? GL2.GL_SEPARATE_SPECULAR_COLOR 
							                               : GL2.GL_SINGLE_COLOR), 
					      nextState.getTwoSidedLighting());
		}
	}

	private void setLightModel(GL2 gl, LightingRecord lr, Color4f ambient, 
							   boolean localViewer, int colorControl, boolean twoSided) {
		// ambient color
		if (!JoglUtil.equals(ambient, lr.lightModelAmbient)) {
			JoglUtil.get(ambient, lr.lightModelAmbient);
			gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, lr.lightModelAmbient, 0);
		}
		// local viewer
		if (localViewer != lr.lightModelLocalViewer) {
			lr.lightModelLocalViewer = localViewer;
			gl.glLightModeli(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, (lr.lightModelLocalViewer ? GL.GL_TRUE 
																					    : GL.GL_FALSE));
		}
		// separate specular
		if (colorControl != lr.lightModelColorControl) {
			lr.lightModelColorControl = colorControl;
			gl.glLightModeli(GL2.GL_LIGHT_MODEL_COLOR_CONTROL, colorControl);
		}
		// two-sided lighting
		if (twoSided != lr.lightModelTwoSided) {
			lr.lightModelTwoSided = twoSided;
			gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, (lr.lightModelTwoSided ? GL.GL_TRUE 
																				 : GL.GL_FALSE));
		}
	}
}
