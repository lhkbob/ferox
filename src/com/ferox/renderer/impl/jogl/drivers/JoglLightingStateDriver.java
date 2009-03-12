package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import org.openmali.vecmath.Vector3f;

import com.ferox.math.Color;
import com.ferox.math.Transform;
import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
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
	
	private final Transform cache;
	private final Light[] appliedLights;
	private Vector3f p;
	
	public JoglLightingStateDriver(JoglSurfaceFactory factory) {
		super(null, Light.class, Math.min(MAX_LIGHTS, factory.getRenderer().getCapabilities().getMaxActiveLights()), factory);
		this.cache = new Transform();
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
	protected void apply(GL gl, JoglContext context, int unit, Light next) {
		LightRecord lr = context.getStateRecord().lightRecord.lightUnits[unit];
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
				// ambient
				Color c = next.getAmbient();
				if (!c.equals(lr.ambient)) {
					c.get(lr.ambient);
					gl.glLightfv(glUnit, GL.GL_AMBIENT, lr.ambient, 0);
				}
				// diffuse
				c = next.getDiffuse();
				if (!c.equals(lr.diffuse)) {
					c.get(lr.diffuse);
					gl.glLightfv(glUnit, GL.GL_DIFFUSE, lr.diffuse, 0);
				}
				// specular
				c = next.getSpecular();
				if (!c.equals(lr.specular)) {
					c.get(lr.specular);
					gl.glLightfv(glUnit, GL.GL_SPECULAR, lr.specular, 0);
				}

				// setup other properties
				if (next instanceof SpotLight)
					this.setupSpotLight(gl, lr, glUnit, (SpotLight) next);
				else if (next instanceof DirectionLight)
					this.setupDirectionLight(gl, lr, glUnit, (DirectionLight) next);
			}
			// else nothing we can do
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
		
		this.cache.mul(this.factory.getCurrentView(), light.getWorldTransform());
		this.factory.getTransformDriver().loadMatrix(gl, this.cache);
		
		// pos and dir
		gl.glLightfv(glUnit, GL.GL_POSITION, lr.position, 0);
		gl.glLightfv(glUnit, GL.GL_SPOT_DIRECTION, lr.spotDirection, 0);
		
		// spotCutoff
		if (lr.spotCutoff != light.getSpotCutoff()) {
			lr.spotCutoff = light.getSpotCutoff();
			gl.glLightf(glUnit, GL.GL_SPOT_CUTOFF, lr.spotCutoff);
		}
		// constant att.
		if (lr.constantAttenuation != light.getConstantAttenuation()) {
			lr.constantAttenuation = light.getConstantAttenuation();
			gl.glLightf(glUnit, GL.GL_CONSTANT_ATTENUATION, lr.constantAttenuation);
		}
		// linear att.
		if (lr.linearAttenuation != light.getLinearAttenuation()) {
			lr.linearAttenuation = light.getLinearAttenuation();
			gl.glLightf(glUnit, GL.GL_LINEAR_ATTENUATION, lr.linearAttenuation);
		}
		// quadratic att.
		if (lr.quadraticAttenuation != light.getQuadraticAttenuation()) {
			lr.quadraticAttenuation = light.getQuadraticAttenuation();
			gl.glLightf(glUnit, GL.GL_QUADRATIC_ATTENUATION, lr.quadraticAttenuation);
		}
	}
	
	private void setupDirectionLight(GL gl, LightRecord lr, int glUnit, DirectionLight light) {
		// set the position array - we don't check, since the modelview changes things
		this.p = light.getDirection();
		lr.position[0] = -this.p.x;
		lr.position[1] = -this.p.y;
		lr.position[2] = -this.p.z;
		lr.position[3] = 0f;
		
		// setup of the direction
		this.cache.mul(this.factory.getCurrentView(), light.getWorldTransform());		
		this.factory.getTransformDriver().loadMatrix(gl, this.cache);
		
		gl.glLightfv(glUnit, GL.GL_POSITION, lr.position, 0);		
	}
}
