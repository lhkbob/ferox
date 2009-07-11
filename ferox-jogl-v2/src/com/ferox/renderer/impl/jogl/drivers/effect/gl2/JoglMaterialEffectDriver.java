package com.ferox.renderer.impl.jogl.drivers.effect.gl2;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;

import com.ferox.effect.Material;
import com.ferox.math.Color4f;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.drivers.effect.SingleEffectDriver;
import com.ferox.renderer.impl.jogl.record.ColoringRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.LightingRecord;

/**
 * This is a simple driver that handles the MATERIAL role for states. At the
 * moment, it only supports instances of Material. Because Material assumes its
 * colors are applied to the front and back, this driver only checks and front
 * material colors, and sets the colors for FRONT_AND_BACK.
 * 
 * @author Michael Ludwig
 */
public class JoglMaterialEffectDriver extends SingleEffectDriver<Material, GL2> {
	public JoglMaterialEffectDriver(JoglContextManager factory) {
		super(new Material(), Material.class, factory);
	}

	@Override
	public GL2 convert(GL2ES2 gl) {
		return gl.getGL2();
	}
	
	@Override
	protected void apply(GL2 gl, JoglStateRecord record, Material nextState) {
		// we have to update lighting each time, since if the case is:
		// <m1, no_lighting> then <m1, lighting>, the 2nd time, m1 will not
		// be applied since it was "already applied"
		LightingRecord lr = record.lightRecord;

		// shininess
		float shiny = nextState.getShininess();
		if (lr.matFrontShininess != shiny || lr.matBackShininess != shiny) {
			gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shiny);
			lr.matFrontShininess = shiny;
			lr.matBackShininess = shiny;
		}
		// ambient
		Color4f c = nextState.getAmbient();
		if (!JoglUtil.equals(c, lr.matFrontAmbient) || !JoglUtil.equals(c, lr.matBackAmbient)) {
			JoglUtil.get(c, lr.matFrontAmbient);
			JoglUtil.get(c, lr.matBackAmbient);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, lr.matFrontAmbient, 0);
		}
		// specular
		c = nextState.getSpecular();
		if (!JoglUtil.equals(c, lr.matFrontSpecular) || !JoglUtil.equals(c, lr.matFrontSpecular)) {
			JoglUtil.get(c, lr.matFrontSpecular);
			JoglUtil.get(c, lr.matBackSpecular);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, lr.matFrontSpecular, 0);
		}
		// diffuse
		c = nextState.getDiffuse();
		if (!JoglUtil.equals(c, lr.matFrontDiffuse) || !JoglUtil.equals(c, lr.matBackDiffuse)) {
			JoglUtil.get(c, lr.matFrontDiffuse);
			JoglUtil.get(c, lr.matBackDiffuse);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, lr.matFrontDiffuse, 0);
		}
		// no lighting, so just set the color to use
		gl.glColor4f(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());

		// set the smoothing
		setSmoothingEnabled(gl, record.colorRecord, nextState.isSmoothShaded());
	}

	private void setSmoothingEnabled(GL2ES1 gl, ColoringRecord cr, boolean enabled) {
		int mode = enabled ? GL2.GL_SMOOTH : GL2.GL_FLAT;
		if (mode != cr.shadeModel) {
			cr.shadeModel = mode;
			gl.glShadeModel(mode);
		}
	}
}
