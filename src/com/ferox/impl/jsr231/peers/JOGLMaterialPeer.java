package com.ferox.impl.jsr231.peers;

import java.util.Arrays;

import javax.media.opengl.GL;

import com.ferox.core.states.atoms.Material;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLMaterialPeer extends SimplePeer<Material, NoRecord> {
	private static final float[] defaultDiffuse = new float[] {.8f, .8f, .8f, 1f};
	private static final float[] defaultSpec = new float[] {0f, 0f, 0f, 1f};
	private static final float[] defaultAmb = new float[] {.2f, .2f, .2f, 1f};
	private static final float defaultShininess = 0f;
	private static final float[] defaultColor = new float[] {0f, 0f, 0f, 1f};
	
	public JOGLMaterialPeer(JOGLRenderContext context) {
		super(context);
	}
	
	protected void applyState(Material prevA, NoRecord prevR, Material nextA, NoRecord nextR, GL gl) {
		if (prevA == null || nextA.getShininess() != prevA.getShininess())
			gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, nextA.getShininess());
		if (prevA == null || !Arrays.equals(nextA.getDiffuseColor(), prevA.getDiffuseColor()))
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, nextA.getDiffuseColor(), 0);
		if (prevA == null || !Arrays.equals(nextA.getSpecularColor(), prevA.getSpecularColor()))
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, nextA.getSpecularColor(), 0);
		if (prevA == null || !Arrays.equals(nextA.getAmbientColor(), prevA.getAmbientColor()))
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, nextA.getAmbientColor(), 0);
		gl.glColor4fv(nextA.getDiffuseColor(), 0);
	}

	protected void restoreState(Material cleanA, NoRecord cleanR, GL gl) {
		gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, defaultShininess);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, defaultDiffuse, 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, defaultAmb, 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, defaultSpec, 0);
		gl.glColor4fv(defaultColor, 0);
	}
}
