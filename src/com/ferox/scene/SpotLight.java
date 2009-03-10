package com.ferox.scene;

import org.openmali.vecmath.Vector3f;

import com.ferox.math.Color;

/**
 * A SpotLight is a light at a given location, shining a cone of light in the 
 * current direction vector of the light.  The size of the cone can be controlled 
 * by a spotlight cutoff angle, where acceptable values are between 0 and 90, as well
 * as 180 (implying a point light).  There are also controls for changing the different
 * light fall-off values based on the distance from a point to the light.
 * 
 * By default, constructed SpotLights have no distance based attenuation and have a spot
 * light cutoff angle of 180.
 * 
 * If d = distance to the object, c = constant attenuation, l = linear attenuation, q = quadratic attenuation factor,
 * then the magnitude of the lighting is equal to: mag / (c + d*l + d^2*q)
 * If l = 0, then there is no linear attenuation.  If q = 0, no quadratic, if c = 0, no constant factor.  
 * l, q, and c should not all be 0 or there will be undefined behavior.  If (c, l, q) = (1, 0, 0), lighting will not be
 * affected by attenuation factors (this is the default setting).
 * 
 * @author Michael Ludwig
 *
 */
public class SpotLight extends Light {
	private float constAtt;
	private float linAtt;
	private float quadAtt;
	
	private float spotCutoff;
	
	/** Create a light with default ambient, diffuse, specular colors and default direction. */
	public SpotLight() {
		this(null);
	}
	
	/** Create a light with default ambient, specular colors and direction, and the given diffuse color. */
	public SpotLight(Color diffuse) {
		this(diffuse, null, null);
	}
	
	/** Create a light with the given colors and the default direction. */
	public SpotLight(Color diffuse, Color specular, Color ambient) {
		this(diffuse, specular, ambient, null);
	}
	
	/** Create a light with the given colors, shining in the given direction.  If any input are null, the default
	 * value is assumed. */
	public SpotLight(Color diffuse, Color specular, Color ambient, Vector3f direction) {
		super(diffuse, specular, ambient, direction);
		this.setConstantAttenuation(1f);
		this.setLinearAttenuation(0f);
		this.setQuadraticAttenuation(0f);
		this.setSpotCutoff(180f);
	}

	/** Get the constant attenuation factor for this spot light. */
	public float getConstantAttenuation() {
		return this.constAtt;
	}

	/** Set the constant attenuation factor for this spot light. Clamped above 0. */
	public void setConstantAttenuation(float constAtt) {
		this.constAtt = Math.max(0f, constAtt);
	}

	/** Get the linear attenuation factor for this spot light. */
	public float getLinearAttenuation() {
		return this.linAtt;
	}

	/** Set the linear attenuation factor for this spot light. Clamped above 0. */
	public void setLinearAttenuation(float linAtt) {
		this.linAtt = Math.max(0f, linAtt);
	}

	/** Get the quadratic attenuation factor for this spot light. */
	public float getQuadraticAttenuation() {
		return this.quadAtt;
	}

	/** Set the quadratic attenuation factor for this spot light. Clamped above 0. */
	public void setQuadraticAttenuation(float quadAtt) {
		this.quadAtt = Math.max(0f, quadAtt);
	}

	/** Get the spot light cutoff angle for this spot light.  Primitives that are outside
	 * the cone formed by the light's direction and this angle will not be lit.  It will be 
	 * a value between 0 and 90 degrees, or 180 degrees. If it's 180, then this spot light is a point light. */
	public float getSpotCutoff() {
		return this.spotCutoff;
	}

	/** Set the spot light cutoff angle for this spot light, in degrees.  Values are clamped
	 * to be between 0 and 90, or to 180, depending on whatever value they are closer to. */
	public void setSpotCutoff(float spotCutoff) {
		// get spotCutoff between 0 and 360
		while(spotCutoff < 0)
			spotCutoff += 360f;
		while(spotCutoff > 360)
			spotCutoff -= 360f;
		
		if (spotCutoff >= 0 && spotCutoff <= 90)
			this.spotCutoff = spotCutoff;
		else {
			// clamp it based on nearest valid angle
			if (spotCutoff >= 135 && spotCutoff <= 315)
				this.spotCutoff = 180f;
			else if (spotCutoff < 135)
				this.spotCutoff = 90f;
			else
				this.spotCutoff = 0f;
		}
	}
}
