package com.ferox.core.scene.states;

import org.openmali.vecmath.Vector3f;

import com.ferox.core.scene.SpatialLeaf;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * Represents a spot light or a point light.  If you want to use a point light, set the cutoff angle to 
 * 180 degrees, otherwise the cutoff is limited between 0 and 90.  Actual spot lights are computationally
 * more expensive to use.  The position of the spot light is stored in the origin of the spot light's 
 * transform.  The direction is the <0,0,1> transformed by the transform's matrix (or the matrix's
 * 3rd column).
 * @author Michael Ludwig
 *
 */
public class SpotLight extends Light {
	private static final float RAD_TO_DEGREES = 180f / (float)Math.PI;
	private static final Vector3f dir = new Vector3f();
	private static final Vector3f light = new Vector3f();
	
	private float constantAttenuation;
	private float linearAttenuation;
	private float quadAttenuation;
	private float spotCutoff;
	
	/**
	 * Creates a white spot light with no attenuation and behaves as a point light at the origin.
	 */
	public SpotLight() {
		super();
		
		this.constantAttenuation = 1f;
		this.linearAttenuation = 0f;
		this.quadAttenuation = 0f;
		this.spotCutoff = 180f;
	}
	
	/**
	 * Creates a spot light with the given diffuse color and location, behaves as a point light.
	 */
	public SpotLight(Vector3f dir, float[] diff) {
		super(diff);
		
		this.constantAttenuation = 1f;
		this.linearAttenuation = 0f;
		this.quadAttenuation = 0f;
		this.spotCutoff = 180f;
		
		this.setLightDirection(dir);
	}
	
	/**
	 * Creates a spot light with the given diffuse/specular colors and location, behaves as a point
	 * light.
	 */
	public SpotLight(Vector3f dir, float[] diff, float[] spec) {
		super(diff, spec);
		
		this.constantAttenuation = 1f;
		this.linearAttenuation = 0f;
		this.quadAttenuation = 0f;
		this.spotCutoff = 180f;
		
		this.setLightDirection(dir);
	}
	
	/**
	 * Get the constant attenuation.
	 */
	public float getConstantAttenuation() {
		return this.constantAttenuation;
	}
	
	/**
	 * Get the linear attenuation.
	 */
	public float getLinearAttenuation() {
		return this.linearAttenuation;
	}
	
	/**
	 * Get the quadratic attenuation.
	 */
	public float getQuadAttenuation() {
		return this.quadAttenuation;
	}
	
	/**
	 * Get the spot light cutoff angle in degrees, it will be 0-90 or 180. A value of 180 represents
	 * a point light and is the default.
	 */
	public float getSpotCutoffAngle() {
		return this.spotCutoff;
	}

	/**
	 * Get the constant attenuation for the light.
	 */
	public void setConstantAttenuation(float constantAttenuation) {
		this.constantAttenuation = Math.max(0f, constantAttenuation);
	}

	/**
	 * Get the linear attenuation for the light.
	 */
	public void setLinearAttenuation(float linearAttenuation) {
		this.linearAttenuation = Math.max(0f, linearAttenuation);
	}

	/**
	 * Get the quadratic attenuation for the light.
	 */
	public void setQuadAttenuation(float quadAttenuation) {
		this.quadAttenuation = Math.max(0f, quadAttenuation);
	}

	/**
	 * Sets the spot light cutoff angle, must be between 0 and 90, or be 180.
	 */
	public void setSpotCutoffAngle(float cutoff) {
		if (cutoff != 180f) {
			cutoff = Math.max(0f, Math.min(cutoff, 90f));
		}
		this.spotCutoff = cutoff;
	}
	
	public float getInfluence(SpatialLeaf leaf) {
		dir.sub(leaf.getWorldTransform().getTranslation(), this.getWorldTransform().getTranslation());
		float dist = dir.length();
		if (this.spotCutoff != 180f) {
			this.getWorldTransform().getRotation().transform(this.getLightDirection(), light);
			
			float ang = (float)Math.acos(dir.dot(light) / (dist * light.length())) * RAD_TO_DEGREES;
			if (ang > this.spotCutoff)
				return 0f;
		}
		
		float color = super.getInfluence(leaf);
		return color / (this.constantAttenuation + dist * this.linearAttenuation + dist * dist * this.quadAttenuation);
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		float[] temp = in.getFloatArray("params");
		this.constantAttenuation = temp[0];
		this.linearAttenuation = temp[1];
		this.quadAttenuation = temp[2];
		this.spotCutoff = temp[3];
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		float[] temp = new float[] {this.constantAttenuation,
									this.linearAttenuation,
									this.quadAttenuation,
									this.spotCutoff};
		
		out.set("params", temp);
	}
}
