package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import org.openmali.vecmath.Vector3f;

import com.ferox.math.Color;
import com.ferox.math.Transform;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.LightingRecord.LightRecord;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Light;
import com.ferox.scene.SpotLight;

/** This state driver supports up to a maximum of 8 lights (or less on
 * older hardware).  It supports instances of SpotLight and DirectionLight
 * fully, and for any other class of Light, will set their color only.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglLightingStateDriver extends MultiStateDriver<Light> {
	private static final int MAX_LIGHTS = 8;
	
	private static final Color DEFAULT_L0_DIFF_SPEC = new Color(1f, 1f, 1f, 1f);
	private static final Color DEFAULT_Li_COLOR = new Color(0f, 0f, 0f, 1f);
	
	private final Light[] appliedLights;
	private Vector3f p;
	
	public JoglLightingStateDriver(JoglSurfaceFactory factory) {
		super(null, Light.class, Math.min(MAX_LIGHTS, factory.getRenderer().getCapabilities().getMaxActiveLights()), factory);
		new Transform();
		this.appliedLights = new Light[Math.min(MAX_LIGHTS, factory.getRenderer().getCapabilities().getMaxActiveLights())];
	}

	@Override
	public void reset() {
		super.reset();
		
		// reset the applied lights, since it's likely that the
		// view transform has changed.
		for (int i = 0; i < this.appliedLights.length; i++) {
			this.appliedLights[i] = null;
		}
	}
	
	@Override
	protected void restore(GL gl, JoglStateRecord record, int unit) {
		LightRecord lr = record.lightRecord.lightUnits[unit];
		int glUnit = GL.GL_LIGHT0 + unit;
		
		if (lr.enabled) {
			lr.enabled = false;
			gl.glDisable(glUnit);
		}
		
		this.setLightColors(gl, lr, glUnit, DEFAULT_Li_COLOR, (unit == 0 ? DEFAULT_L0_DIFF_SPEC : DEFAULT_Li_COLOR), 
							(unit == 0 ? DEFAULT_L0_DIFF_SPEC : DEFAULT_Li_COLOR));
		this.setSpotParameters(gl, lr, glUnit, 180f, 1f, 0f, 0f);
		
		lr.spotDirection[0] = 0f; lr.spotDirection[1] = 0f; lr.spotDirection[2] = -1f;
		gl.glLightfv(glUnit, GL.GL_SPOT_DIRECTION, lr.spotDirection, 0);
		
		lr.position[0] = 0f; lr.position[1] = 0f; lr.position[2] = 1f; lr.position[3] = 0f;
		gl.glLightfv(glUnit, GL.GL_POSITION, lr.position, 0);
	}
	
	@Override
	protected void apply(GL gl, JoglStateRecord record, int unit, Light next) {
		LightRecord lr = record.lightRecord.lightUnits[unit];
		int glUnit = GL.GL_LIGHT0 + unit;
		
		if (next == null) {
			// disable the light
			if (lr.enabled) {
				lr.enabled = false;
				gl.glDisable(glUnit);
			}
		} else {
			// enable and configure the lighting params
			if (!lr.enabled) {
				lr.enabled = true;
				gl.glEnable(glUnit);
			}
			
			// only make the other changes if the light is different
			if (this.appliedLights[unit] != next) {
				this.appliedLights[unit] = next;
				
				// set lighting colors
				this.setLightColors(gl, lr, glUnit, next.getAmbient(), next.getDiffuse(), next.getSpecular());

				// setup other properties
				if (next instanceof SpotLight)
					this.setupSpotLight(gl, lr, glUnit, (SpotLight) next);
				else if (next instanceof DirectionLight)
					this.setupDirectionLight(gl, lr, glUnit, (DirectionLight) next);
			}
			// else nothing we can do
		}
	}
	
	private void setLightColors(GL gl, LightRecord lr, int glUnit, Color amb, Color diff, Color spec) {
		// ambient
		if (!amb.equals(lr.ambient)) {
			amb.get(lr.ambient);
			gl.glLightfv(glUnit, GL.GL_AMBIENT, lr.ambient, 0);
		}
		// diffuse
		if (!diff.equals(lr.diffuse)) {
			diff.get(lr.diffuse);
			gl.glLightfv(glUnit, GL.GL_DIFFUSE, lr.diffuse, 0);
		}
		// specular
		if (!spec.equals(lr.specular)) {
			spec.get(lr.specular);
			gl.glLightfv(glUnit, GL.GL_SPECULAR, lr.specular, 0);
		}
	}
	
	private void setSpotParameters(GL gl, LightRecord lr, int glUnit, float spotCutoff, float constant, float linear, float quad) {
		// spotCutoff
		if (lr.spotCutoff != spotCutoff) {
			lr.spotCutoff = spotCutoff;
			gl.glLightf(glUnit, GL.GL_SPOT_CUTOFF, lr.spotCutoff);
		}
		// constant att.
		if (lr.constantAttenuation != constant) {
			lr.constantAttenuation = constant;
			gl.glLightf(glUnit, GL.GL_CONSTANT_ATTENUATION, lr.constantAttenuation);
		}
		// linear att.
		if (lr.linearAttenuation != linear) {
			lr.linearAttenuation = linear;
			gl.glLightf(glUnit, GL.GL_LINEAR_ATTENUATION, lr.linearAttenuation);
		}
		// quadratic att.
		if (lr.quadraticAttenuation != quad) {
			lr.quadraticAttenuation = quad;
			gl.glLightf(glUnit, GL.GL_QUADRATIC_ATTENUATION, lr.quadraticAttenuation);
		}
	}
	
	private void setupSpotLight(GL gl, LightRecord lr, int glUnit, SpotLight light) {
		// setup the pos and direction
		this.p = light.getDirection();
		lr.position[0] = 0f; 
		lr.position[1] = 0f; 
		lr.position[2] = 0f; 
		lr.position[3] = 1f;
		
		lr.spotDirection[0] = this.p.x; 
		lr.spotDirection[1] = this.p.y; 
		lr.spotDirection[2] = this.p.z;
		
		this.factory.getTransformDriver().pushMatrix(gl, light.getWorldTransform());
		
		// pos and dir
		gl.glLightfv(glUnit, GL.GL_POSITION, lr.position, 0);
		gl.glLightfv(glUnit, GL.GL_SPOT_DIRECTION, lr.spotDirection, 0);
		
		this.setSpotParameters(gl, lr, glUnit, light.getSpotCutoff(), light.getConstantAttenuation(), 
							   light.getLinearAttenuation(), light.getQuadraticAttenuation());
		
		gl.glPopMatrix();
	}
	
	private void setupDirectionLight(GL gl, LightRecord lr, int glUnit, DirectionLight light) {
		// set the position array - we don't check, since the modelview changes things
		this.p = light.getDirection();
		lr.position[0] = -this.p.x;
		lr.position[1] = -this.p.y;
		lr.position[2] = -this.p.z;
		lr.position[3] = 0f;
		
		// setup of the direction
		this.factory.getTransformDriver().pushMatrix(gl, light.getWorldTransform());
		gl.glLightfv(glUnit, GL.GL_POSITION, lr.position, 0);
		gl.glPopMatrix();
	}
}
