package com.ferox.effect;

import org.openmali.vecmath.Vector3f;

import com.ferox.math.Color;

/**
 * A DirectionLight treats the light direction as coming from a source
 * infinitely far away, so the angle of light hitting a surface doesn't change
 * based on the light's position. In effect, then only the rotation of this
 * light's transform has any effect on the lighting.
 * 
 * @author Michael Ludwig
 */
public class DirectionLight extends Light {
	/**
	 * Create a light with default ambient, diffuse, specular colors and given
	 * direction.
	 * 
	 * @param direction
	 */
	public DirectionLight(Vector3f direction) {
		this(direction, null);
	}

	/**
	 * Create a light with default ambient, specular colors, and the given
	 * diffuse color and direction.
	 * 
	 * @param direction
	 * @param diffuse
	 */
	public DirectionLight(Vector3f direction, Color diffuse) {
		this(direction, diffuse, null, null);
	}

	/**
	 * Create a light with the given colors and the direction. Values of null
	 * will be replaced by the defaults.
	 * 
	 * @param direction
	 * @param diffuse
	 * @param specular
	 * @param ambient
	 */
	public DirectionLight(Vector3f direction, Color diffuse, Color specular,
		Color ambient) {
		super(diffuse, specular, ambient, direction);
	}
}
