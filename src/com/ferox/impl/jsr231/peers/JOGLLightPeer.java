package com.ferox.impl.jsr231.peers;

import java.util.Arrays;

import javax.media.opengl.GL;

import org.openmali.vecmath.AxisAngle4f;
import org.openmali.vecmath.Vector3f;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.scene.states.DirectionLight;
import com.ferox.core.scene.states.Light;
import com.ferox.core.scene.states.SpotLight;
import com.ferox.core.states.*;
import com.ferox.core.states.manager.LightManager;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLLightPeer extends SimplePeer<Light, NoRecord> {
	private static final float RAD_TO_DEGREES = 180 / (float)Math.PI;
	private static final AxisAngle4f aa = new AxisAngle4f();
	private static Vector3f p;
	
	private int light;
	private float[] pos;
	private float[] dir;
	
	public JOGLLightPeer(JOGLRenderContext context) {
		super(context);
		this.light = 0;
		this.pos = new float[4];
		this.dir = new float[4];
	}
	
	protected void applyState(Light prev, NoRecord prevR, Light next, NoRecord nextR, GL gl) {
		if (prev == null)
			gl.glEnable(GL.GL_LIGHT0 + this.light);
		
		if (prev == null || !Arrays.equals(prev.getAmbientColor(), next.getAmbientColor()))
			gl.glLightfv(GL.GL_LIGHT0 + this.light, GL.GL_AMBIENT, next.getAmbientColor(), 0);
		if (prev == null || !Arrays.equals(prev.getDiffuseColor(), next.getDiffuseColor()))
			gl.glLightfv(GL.GL_LIGHT0 + this.light, GL.GL_DIFFUSE, next.getDiffuseColor(), 0);
		if (prev == null || !Arrays.equals(prev.getSpecularColor(), next.getSpecularColor()))
			gl.glLightfv(GL.GL_LIGHT0 + this.light, GL.GL_SPECULAR, next.getSpecularColor(), 0);
			
		if (prev == null) {
			if (next instanceof SpotLight)
				this.applySpotLightFull((SpotLight)next, gl);
			else
				this.applyDirLightFull((DirectionLight)next, gl);
		} else {
			if (next instanceof SpotLight) {
				if (prev instanceof SpotLight) 
					this.applySpotLightSpot((SpotLight)prev, (SpotLight)next, gl);
				else
					this.applySpotLightDir((DirectionLight)prev, (SpotLight)next, gl);
			} else {
				if (prev instanceof SpotLight) 
					this.applyDirLightSpot((SpotLight)prev, (DirectionLight)next, gl);
				else
					this.applyDirLightDir((DirectionLight)prev, (DirectionLight)next, gl);
			}
		}
	}
	
	private void applySpotLightSpot(SpotLight prev, SpotLight next, GL gl) {
		p = next.getLightDirection();
		this.pos[0] = 0f; this.pos[1] = 0f; this.pos[2] = 0f; this.pos[3] = 1f;
		this.dir[0] = p.x; this.dir[1] = p.y; this.dir[2] = p.z; this.dir[3] = 0f;
		
		gl.glPushMatrix();
		aa.set(next.getWorldTransform().getRotation());
		p = next.getWorldTransform().getTranslation();
		gl.glTranslatef(p.x, p.y, p.z);
		gl.glRotatef(aa.angle * RAD_TO_DEGREES, aa.x, aa.y, aa.z);
		
		gl.glLightfv(GL.GL_LIGHT0 + this.light, GL.GL_POSITION, this.pos, 0);
		gl.glLightfv(GL.GL_LIGHT0 + this.light, GL.GL_SPOT_DIRECTION, this.dir, 0);
		if (next.getSpotCutoffAngle() != prev.getSpotCutoffAngle())
			gl.glLightf(GL.GL_LIGHT0 + this.light, GL.GL_SPOT_CUTOFF, next.getSpotCutoffAngle());
		if (next.getConstantAttenuation() != prev.getConstantAttenuation())
			gl.glLightf(GL.GL_LIGHT0 + this.light, GL.GL_CONSTANT_ATTENUATION, next.getConstantAttenuation());
		if (next.getLinearAttenuation() != prev.getLinearAttenuation())
			gl.glLightf(GL.GL_LIGHT0 + this.light, GL.GL_LINEAR_ATTENUATION, next.getLinearAttenuation());
		if (next.getQuadAttenuation() != prev.getQuadAttenuation())
			gl.glLightf(GL.GL_LIGHT0 + this.light, GL.GL_QUADRATIC_ATTENUATION, next.getQuadAttenuation());
		gl.glPopMatrix();
	}
	
	private void applySpotLightDir(DirectionLight prev, SpotLight next, GL gl) {
		this.applySpotLightFull(next, gl);
	}
	
	private void applyDirLightSpot(SpotLight prev, DirectionLight next, GL gl) {
		this.applyDirLightFull(next, gl);
	}
	
	private void applyDirLightDir(DirectionLight prev, DirectionLight next, GL gl) {
		this.applyDirLightFull(next, gl);
	}
	
	private void applySpotLightFull(SpotLight light, GL gl) {
		p = light.getLightDirection();
		this.pos[0] = 0f; this.pos[1] = 0f; this.pos[2] = 0f; this.pos[3] = 1f;
		this.dir[0] = p.x; this.dir[1] = p.y; this.dir[2] = p.z; this.dir[3] = 0f;
		
		gl.glPushMatrix();
		aa.set(light.getWorldTransform().getRotation());
		p = light.getWorldTransform().getTranslation();
		gl.glTranslatef(p.x, p.y, p.z);
		gl.glRotatef(aa.angle * RAD_TO_DEGREES, aa.x, aa.y, aa.z);
		
		gl.glLightfv(GL.GL_LIGHT0 + this.light, GL.GL_POSITION, this.pos, 0);
		gl.glLightfv(GL.GL_LIGHT0 + this.light, GL.GL_SPOT_DIRECTION, this.dir, 0);
		gl.glLightf(GL.GL_LIGHT0 + this.light, GL.GL_SPOT_CUTOFF, light.getSpotCutoffAngle());
		gl.glLightf(GL.GL_LIGHT0 + this.light, GL.GL_CONSTANT_ATTENUATION, light.getConstantAttenuation());
		gl.glLightf(GL.GL_LIGHT0 + this.light, GL.GL_LINEAR_ATTENUATION, light.getLinearAttenuation());
		gl.glLightf(GL.GL_LIGHT0 + this.light, GL.GL_QUADRATIC_ATTENUATION, light.getQuadAttenuation());
		gl.glPopMatrix();
	}
	
	private void applyDirLightFull(DirectionLight light, GL gl) {
		p = light.getLightDirection();
		this.pos[0] = -p.x; this.pos[1] = -p.y; this.pos[2] = -p.z; this.pos[3] = 0f;
		
		gl.glPushMatrix();
		aa.set(light.getWorldTransform().getRotation());
		gl.glRotatef(aa.angle * RAD_TO_DEGREES, aa.x, aa.y, aa.z);
		
		gl.glLightfv(GL.GL_LIGHT0 + this.light, GL.GL_POSITION, this.pos, 0);
		gl.glPopMatrix();
	}

	public void disableManager(StateManager manager) {
		this.context.getGL().glDisable(GL.GL_LIGHTING);
	}

	public void prepareManager(StateManager manager, StateManager previous) {
		this.prepareManager((LightManager)manager, (LightManager)previous);
	}
	
	private void prepareManager(LightManager manager, LightManager previous) {
		if (previous == null)
			this.context.getGL().glEnable(GL.GL_LIGHTING);
		this.light = 0;
		if (previous == null || manager.isLocalViewer() != previous.isLocalViewer())
			this.context.getGL().glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER, (manager.isLocalViewer() ? GL.GL_TRUE : GL.GL_FALSE));
		if (RenderManager.getSystemCapabilities().isSeparateSpecularLightingSupported())
			if (previous == null || manager.isSeperateSpecularHighlight() != previous.isSeperateSpecularHighlight())
				this.context.getGL().glLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL, (manager.isSeperateSpecularHighlight() ? GL.GL_SEPARATE_SPECULAR_COLOR : GL.GL_SINGLE_COLOR));
		if (previous == null || !Arrays.equals(previous.getGlobalAmbientLight(), manager.getGlobalAmbientLight()))
			this.context.getGL().glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, manager.getGlobalAmbientLight(), 0);
	}

	protected void restoreState(Light cleanA, NoRecord cleanR, GL gl) {
		this.context.getGL().glDisable(GL.GL_LIGHT0 + this.light);
	}

	public void setUnit(StateUnit unit) {
		this.light = ((NumericUnit)unit).ordinal();
	}
}
