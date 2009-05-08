package com.ferox.scene;

import org.openmali.vecmath.Vector3f;

import com.ferox.effect.SpotLight;
import com.ferox.math.Color;

/**
 * <p>
 * SpotLightNode is a LightNode that represents a SpotLight within a given
 * scene. The spot light represented by this node takes its position from the
 * node's world transform.
 * </p>
 * <p>
 * This class adds the additional methods that necessary to mirror SpotLight's
 * public methods.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class SpotLightNode extends LightNode<SpotLight> {
	/**
	 * Create a SpotLightNode with a SpotLight created with the given arguments.
	 * 
	 * @param position
	 */
	public SpotLightNode(Vector3f position) {
		super(new SpotLight(position));
	}

	/**
	 * Create a SpotLightNode with a SpotLight created with the given arguments.
	 * 
	 * @param position
	 * @param diffuse
	 */
	public SpotLightNode(Vector3f position, Color diffuse) {
		super(new SpotLight(position, diffuse));
	}

	/**
	 * Create a SpotLightNode with a SpotLight created with the given arguments.
	 * 
	 * @param position
	 * @param diffuse
	 * @param specular
	 * @param ambient
	 */
	public SpotLightNode(Vector3f position, Color diffuse, Color specular,
		Color ambient) {
		super(new SpotLight(position, diffuse, specular, ambient));
	}

	/**
	 * Create a SpotLightNode with a SpotLight created with the given arguments.
	 * 
	 * @param position
	 * @param diffuse
	 * @param specular
	 * @param ambient
	 * @param direction
	 */
	public SpotLightNode(Vector3f position, Color diffuse, Color specular,
		Color ambient, Vector3f direction) {
		super(new SpotLight(position, diffuse, specular, ambient, direction));
	}

	/**
	 * Identical operation to SpotLight's getConstantAttenuation().
	 * 
	 * @see SpotLight.#getConstantAttenuation()
	 * @return This light's constant attenuation
	 */
	public float getConstantAttenutation() {
		return light.getConstantAttenuation();
	}

	/**
	 * Identical operation to SpotLight's getLinearAttenuation().
	 * 
	 * @see SpotLight.#getLinearAttenuation()
	 * @return This light's linear attenuation
	 */
	public float getLinearAttenuation() {
		return light.getLinearAttenuation();
	}

	/**
	 * Identical operation to SpotLight's getQuadraticAttenuation().
	 * 
	 * @see SpotLight.#getQuadraticAttenuation()
	 * @return This light's quadratic attenuation
	 */
	public float getQuadraticAttenuation() {
		return light.getQuadraticAttenuation();
	}

	/**
	 * Identical operation to SpotLight's getSpotCutoff().
	 * 
	 * @see SpotLight.#getSpotCutoff()
	 * @return This light's spot cutoff angle
	 */
	public float getSpotCutoff() {
		return light.getSpotCutoff();
	}

	/**
	 * Identical operation to SpotLight's setConstantAttenuation(float).
	 * 
	 * @see SpotLight.#setConstantAttenuation(float)
	 * @param constAtt The light's constant attenuation to use
	 */
	public void setConstantAttenuation(float constAtt) {
		light.setConstantAttenuation(constAtt);
	}

	/**
	 * Identical operation to SpotLight's setLinearAttenuation(float).
	 * 
	 * @see SpotLight.#setLinearAttenuation(float)
	 * @param linAtt The light's linear attenuation to use
	 */
	public void setLinearAttenuation(float linAtt) {
		light.setLinearAttenuation(linAtt);
	}

	/**
	 * Identical operation to SpotLight's setQuadraticAttenuation(float).
	 * 
	 * @see SpotLight.#setQuadraticAttenuation(float)
	 * @param quadAtt The light's quadratic attenuation to use
	 */
	public void setQuadraticAttenuation(float quadAtt) {
		light.setQuadraticAttenuation(quadAtt);
	}

	/**
	 * Identical operation to SpotLight's setSpotCutoff(float).
	 * 
	 * @see SpotLight.#setSpotCutoff(float)
	 * @param spotCutoff The light's spotlight cutoff angle to use
	 */
	public void setSpotCutoff(float spotCutoff) {
		light.setSpotCutoff(spotCutoff);
	}

	/**
	 * Overridden to properly this node's spot light position in world space.
	 * 
	 * @param fast
	 */
	@Override
	public void updateTransform(boolean fast) {
		super.updateTransform(fast);

		Vector3f worldPos = light.getPosition();
		worldPos.set(worldTransform.getTranslation());
	}
}
