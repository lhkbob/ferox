package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.math.Color;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.ColoringRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.LightingRecord;
import com.ferox.state.Material;

/** This is a simple driver that handles the MATERIAL role for 
 * states.  At the moment, it only supports instances of Material.
 * Because Material assumes its colors are applied to the front
 * and back, this driver only checks and front material colors,
 * and sets the colors for FRONT_AND_BACK.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglMaterialStateDriver extends SingleStateDriver<Material> {
	public JoglMaterialStateDriver(JoglSurfaceFactory factory) {
		super(new Material(), Material.class, factory);
	}

	@Override
	protected void apply(GL gl, JoglStateRecord record, Material nextState) {
		// we have to update lighting each time, since if the case is:
		// <m1, no_lighting> then <m1, lighting>, the 2nd time, m1 will not
		// be applied since it was "already applied"
		LightingRecord lr = record.lightRecord;

		// shininess
		float shiny = nextState.getShininess();
		if (lr.matFrontShininess != shiny || lr.matBackShininess != shiny) {
			gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, shiny);
			lr.matFrontShininess = shiny;
			lr.matBackShininess = shiny;
		}
		// ambient
		Color c = nextState.getAmbient();
		if (!c.equals(lr.matFrontAmbient) || !c.equals(lr.matBackAmbient)) {
			c.get(lr.matFrontAmbient);
			c.get(lr.matBackAmbient);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, lr.matFrontAmbient, 0);
		}
		// specular
		c = nextState.getSpecular();
		if (!c.equals(lr.matFrontSpecular) || !c.equals(lr.matFrontSpecular)) {
			c.get(lr.matFrontSpecular);
			c.get(lr.matBackSpecular);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, lr.matFrontSpecular, 0);
		}
		// diffuse
		c = nextState.getDiffuse();
		if (!c.equals(lr.matFrontDiffuse) || !c.equals(lr.matBackDiffuse)) {
			c.get(lr.matFrontDiffuse);
			c.get(lr.matBackDiffuse);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, lr.matFrontDiffuse, 0);
		}
		// no lighting, so just set the color to use
		gl.glColor4f(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
		
		// set the smoothing
		setSmoothingEnabled(gl, record.colorRecord, nextState.isSmoothShaded());
	}
	
	private static void setSmoothingEnabled(GL gl, ColoringRecord cr, boolean enabled) {
		int mode = enabled ? GL.GL_SMOOTH : GL.GL_FLAT;
		if (mode != cr.shadeModel) {
			cr.shadeModel = mode;
			gl.glShadeModel(mode);
		}
	}
}
