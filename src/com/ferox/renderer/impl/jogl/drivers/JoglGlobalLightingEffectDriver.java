package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.effect.GlobalLighting;
import com.ferox.math.Color;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.LightingRecord;

/**
 * This state driver provides global lighting control by using the
 * GlobalLighting class. When no lighting is needed, it just disables lighting.
 * 
 * @author Michael Ludwig
 */
public class JoglGlobalLightingEffectDriver extends
	SingleStateDriver<GlobalLighting> {

	public JoglGlobalLightingEffectDriver(JoglContextManager factory) {
		super(null, GlobalLighting.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, GlobalLighting nextState) {
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

			setLightModel(gl, lr, nextState.getGlobalAmbient(), nextState
				.isLocalViewer(), (nextState.getSeparateSpecular()
				? GL.GL_SEPARATE_SPECULAR_COLOR : GL.GL_SINGLE_COLOR),
				nextState.getTwoSidedLighting());
		}
	}

	private void setLightModel(GL gl, LightingRecord lr, Color ambient,
		boolean localViewer, int colorControl, boolean twoSided) {
		// ambient color
		if (!JoglUtil.equals(ambient, lr.lightModelAmbient)) {
			JoglUtil.get(ambient, lr.lightModelAmbient);
			gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, lr.lightModelAmbient,
				0);
		}
		// local viewer
		if (localViewer != lr.lightModelLocalViewer) {
			lr.lightModelLocalViewer = localViewer;
			gl.glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER,
				(lr.lightModelLocalViewer ? GL.GL_TRUE : GL.GL_FALSE));
		}
		// separate specular
		if (colorControl != lr.lightModelColorControl) {
			lr.lightModelColorControl = colorControl;
			gl.glLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL, colorControl);
		}
		// two-sided lighting
		if (twoSided != lr.lightModelTwoSided) {
			lr.lightModelTwoSided = twoSided;
			gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, (lr.lightModelTwoSided
				? GL.GL_TRUE : GL.GL_FALSE));
		}
	}
}
