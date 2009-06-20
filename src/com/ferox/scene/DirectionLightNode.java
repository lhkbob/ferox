package com.ferox.scene;

import com.ferox.effect.DirectionLight;
import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;

/**
 * A subclass of LightNode that works with DirectionLights. It provides
 * constructors that mirror the provided constructors in DirectionLight.
 * 
 * @author Michael Ludwig
 */
public class DirectionLightNode extends LightNode<DirectionLight> {
	/**
	 * Create a DirectionLightNode with a DirectionLight created with the given
	 * arguments.
	 * 
	 * @param direction
	 */
	public DirectionLightNode(Vector3f direction) {
		super(new DirectionLight(direction));
	}

	/**
	 * Create a DirectionLightNode with a DirectionLight created with the given
	 * arguments.
	 * 
	 * @param direction
	 * @param diffuse
	 */
	public DirectionLightNode(Vector3f direction, Color4f diffuse) {
		super(new DirectionLight(direction, diffuse));
	}

	/**
	 * Create a DirectionLightNode with a DirectionLight created with the given
	 * arguments.
	 * 
	 * @param direction
	 * @param diffuse
	 * @param specular
	 * @param ambient
	 */
	public DirectionLightNode(Vector3f direction, Color4f diffuse,
		Color4f specular, Color4f ambient) {
		super(new DirectionLight(direction, diffuse, specular, ambient));
	}
}
