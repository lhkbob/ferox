package com.ferox.core.scene.states;

import org.openmali.vecmath.Vector3f;


/**
 * DirectionalLight represents a light source that is far enough away that lighting seems to come
 * from the same angle no matter what (ie sunlight).  Vector directions for the light are from light
 * "source" to the objects.
 * @author Michael Ludwig
 *
 */
public class DirectionLight extends Light {
	/**
	 * Creates a white direction light, pointing down the positive z axis.
	 */
	public DirectionLight() {
		super();

		this.setLightDirection(new Vector3f(0f, 0f, 1f));
	}

	/**
	 * Creates a direction light in the given direction (dir) with the given diffuse color.
	 */
	public DirectionLight(Vector3f dir, float[] diff) {
		super(diff);

		this.setLightDirection(dir);
	}

	/**
	 * Creates a direction light in direction (dir) with the diffuse and specular colors.
	 */
	public DirectionLight(Vector3f dir, float[] diff, float[] spec) {
		super(diff, spec);

		this.setLightDirection(dir);
	}
}
