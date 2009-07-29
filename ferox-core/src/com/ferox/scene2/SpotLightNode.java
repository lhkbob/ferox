package com.ferox.scene2;

import com.ferox.effect.SpotLight;
import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;

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
	 */
	public SpotLightNode() {
		super(new SpotLight(new Vector3f()));
	}

	/**
	 * Create a SpotLightNode with a SpotLight created with the given arguments.
	 * 
	 * @param diffuse
	 */
	public SpotLightNode(Color4f diffuse) {
		super(new SpotLight(new Vector3f(), diffuse));
	}

	/**
	 * Create a SpotLightNode with a SpotLight created with the given arguments.
	 * 
	 * @param diffuse
	 * @param specular
	 * @param ambient
	 */
	public SpotLightNode(Color4f diffuse, Color4f specular, Color4f ambient) {
		super(new SpotLight(new Vector3f(), diffuse, specular, ambient));
	}

	/**
	 * Create a SpotLightNode with a SpotLight created with the given arguments.
	 * 
	 * @param diffuse
	 * @param specular
	 * @param ambient
	 * @param direction
	 */
	public SpotLightNode(Color4f diffuse, Color4f specular, Color4f ambient, Vector3f direction) {
		super(new SpotLight(new Vector3f(), diffuse, specular, ambient, direction));
	}

	/**
	 * Identical operation to SpotLight's getConstantAttenuation().
	 * 
	 * @return This light's constant attenuation
	 */
	public float getConstantAttenutation() {
		return light.getConstantAttenuation();
	}

	/**
	 * Identical operation to SpotLight's getLinearAttenuation().
	 * 
	 * @return This light's linear attenuation
	 */
	public float getLinearAttenuation() {
		return light.getLinearAttenuation();
	}

	/**
	 * Identical operation to SpotLight's getQuadraticAttenuation().
	 * 
	 * @return This light's quadratic attenuation
	 */
	public float getQuadraticAttenuation() {
		return light.getQuadraticAttenuation();
	}

	/**
	 * Identical operation to SpotLight's getSpotCutoff().
	 * 
	 * @return This light's spot cutoff angle
	 */
	public float getSpotCutoff() {
		return light.getSpotCutoff();
	}

	/**
	 * Identical operation to SpotLight's setConstantAttenuation(float).
	 * 
	 * @param constAtt The light's constant attenuation to use
	 */
	public void setConstantAttenuation(float constAtt) {
		light.setConstantAttenuation(constAtt);
	}

	/**
	 * Identical operation to SpotLight's setLinearAttenuation(float).
	 * 
	 * @param linAtt The light's linear attenuation to use
	 */
	public void setLinearAttenuation(float linAtt) {
		light.setLinearAttenuation(linAtt);
	}

	/**
	 * Identical operation to SpotLight's setQuadraticAttenuation(float).
	 * 
	 * @param quadAtt The light's quadratic attenuation to use
	 */
	public void setQuadraticAttenuation(float quadAtt) {
		light.setQuadraticAttenuation(quadAtt);
	}

	/**
	 * Identical operation to SpotLight's setSpotCutoff(float).
	 * 
	 * @param spotCutoff The light's spotlight cutoff angle to use
	 */
	public void setSpotCutoff(float spotCutoff) {
		light.setSpotCutoff(spotCutoff);
	}

	@Override
	public boolean update(float timeDelta) {
		boolean res = super.update(timeDelta);

		Vector3f worldPos = light.getPosition();
		worldPos.set(worldTransform.getTranslation());
		return res;
	}
}
