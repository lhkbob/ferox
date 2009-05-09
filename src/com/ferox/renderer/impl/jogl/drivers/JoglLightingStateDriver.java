package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import org.openmali.vecmath.Vector3f;

import com.ferox.effect.DirectionLight;
import com.ferox.effect.Light;
import com.ferox.effect.SpotLight;
import com.ferox.math.Color;
import com.ferox.math.Transform;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.LightingRecord.LightRecord;

/**
 * This state driver supports up to a maximum of 8 lights (or less on older
 * hardware). It supports instances of SpotLight and DirectionLight fully, and
 * for any other class of Light, will set their color only.
 * 
 * @author Michael Ludwig
 * 
 */
public class JoglLightingStateDriver extends MultiStateDriver<Light> {
	private static final int MAX_LIGHTS = 8;

	private final Light[] appliedLights;
	private Vector3f p;

	public JoglLightingStateDriver(JoglContextManager factory) {
		super(null, Light.class, Math.min(MAX_LIGHTS, factory.getRenderer()
				.getCapabilities().getMaxActiveLights()), factory);
		new Transform();
		appliedLights = new Light[Math.min(MAX_LIGHTS, factory.getRenderer()
				.getCapabilities().getMaxActiveLights())];
	}

	@Override
	public void reset() {
		super.reset();

		// reset the applied lights, since it's likely that the
		// view transform has changed.
		for (int i = 0; i < appliedLights.length; i++)
			appliedLights[i] = null;
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
			if (appliedLights[unit] != next) {
				appliedLights[unit] = next;

				// set lighting colors
				setLightColors(gl, lr, glUnit, next.getAmbient(), next
						.getDiffuse(), next.getSpecular());

				// setup other properties
				if (next instanceof SpotLight)
					setupSpotLight(gl, lr, glUnit, (SpotLight) next);
				else if (next instanceof DirectionLight)
					setupDirectionLight(gl, lr, glUnit, (DirectionLight) next);
			}
			// else nothing we can do
		}
	}

	private void setLightColors(GL gl, LightRecord lr, int glUnit, Color amb,
			Color diff, Color spec) {
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

	private void setSpotParameters(GL gl, LightRecord lr, int glUnit,
			float spotCutoff, float constant, float linear, float quad) {
		// spotCutoff
		if (lr.spotCutoff != spotCutoff) {
			lr.spotCutoff = spotCutoff;
			gl.glLightf(glUnit, GL.GL_SPOT_CUTOFF, lr.spotCutoff);
		}
		// constant att.
		if (lr.constantAttenuation != constant) {
			lr.constantAttenuation = constant;
			gl.glLightf(glUnit, GL.GL_CONSTANT_ATTENUATION,
					lr.constantAttenuation);
		}
		// linear att.
		if (lr.linearAttenuation != linear) {
			lr.linearAttenuation = linear;
			gl.glLightf(glUnit, GL.GL_LINEAR_ATTENUATION, lr.linearAttenuation);
		}
		// quadratic att.
		if (lr.quadraticAttenuation != quad) {
			lr.quadraticAttenuation = quad;
			gl.glLightf(glUnit, GL.GL_QUADRATIC_ATTENUATION,
					lr.quadraticAttenuation);
		}
	}

	private void setupSpotLight(GL gl, LightRecord lr, int glUnit,
			SpotLight light) {
		// setup the pos and direction
		p = light.getDirection();
		lr.position[0] = 0f;
		lr.position[1] = 0f;
		lr.position[2] = 0f;
		lr.position[3] = 1f;

		lr.spotDirection[0] = p.x;
		lr.spotDirection[1] = p.y;
		lr.spotDirection[2] = p.z;

		factory.getTransformDriver().pushMatrix(gl, light.getWorldTransform());

		// pos and dir
		gl.glLightfv(glUnit, GL.GL_POSITION, lr.position, 0);
		gl.glLightfv(glUnit, GL.GL_SPOT_DIRECTION, lr.spotDirection, 0);

		setSpotParameters(gl, lr, glUnit, light.getSpotCutoff(), light
				.getConstantAttenuation(), light.getLinearAttenuation(), light
				.getQuadraticAttenuation());

		gl.glPopMatrix();
	}

	private void setupDirectionLight(GL gl, LightRecord lr, int glUnit,
			DirectionLight light) {
		// set the position array - we don't check, since the modelview changes
		// things
		p = light.getDirection();
		lr.position[0] = -p.x;
		lr.position[1] = -p.y;
		lr.position[2] = -p.z;
		lr.position[3] = 0f;

		// setup of the direction
		factory.getTransformDriver().pushMatrix(gl, light.getWorldTransform());
		gl.glLightfv(glUnit, GL.GL_POSITION, lr.position, 0);
		gl.glPopMatrix();
	}
}
