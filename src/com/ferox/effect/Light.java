package com.ferox.effect;

import com.ferox.effect.Effect.Type;
import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;

/**
 * <p>
 * Represents a light within a scene. At the moment there are two subclasses:
 * DirectionLight and SpotLight, which represent the two main type of lights
 * allowed in dynamic lighting with low-level graphics APIs.
 * </p>
 * <p>
 * Each Light has a direction (and for SpotLights, it also has a position).
 * These vectors represent where the light source is, in the same coordinate
 * space of the RenderAtoms that are affected by it.
 * </p>
 * <p>
 * Because Light has an Type of LIGHT, multiple Lights may be added to an
 * EffectSet. There is likely to be a Framework imposed limit on the actual
 * number of Lights that affect a rendered atom.
 * </p>
 * <p>
 * All colors are stored as references, any changes to the color objects will be
 * reflected in the rendering and other lights/materials using the color
 * objects.
 * </p>
 * <p>
 * Default diffuse color is <.8, .8, .8, 1>, ambient is <.2, .2, .2, 1>,
 * specular is <1, 1, 1, 1> and the default direction is <0, 0, 1>.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Type(EffectType.LIGHT)
public abstract class Light extends AbstractEffect {
	private static final Color4f DEFAULT_AMBIENT = new Color4f(.2f, .2f, .2f);
	private static final Color4f DEFAULT_DIFFUSE = new Color4f(.8f, .8f, .8f);
	private static final Color4f DEFAULT_SPEC = new Color4f(1f, 1f, 1f);

	private Color4f ambient;
	private Color4f diffuse;
	private Color4f specular;

	private Vector3f direction;

	/**
	 * Create a light with default ambient, diffuse, specular colors and default
	 * direction.
	 */
	public Light() {
		this(null);
	}

	/**
	 * Create a light with default ambient, specular colors and direction, and
	 * the given diffuse color.
	 * 
	 * @param diffuse The diffuse color to use for the light, null = default
	 */
	public Light(Color4f diffuse) {
		this(diffuse, null, null);
	}

	/**
	 * Create a light with the given colors and the default direction.
	 * 
	 * @param diffuse The diffuse color to use for the light, null = default
	 * @param specular The specular color to use for the light, null = default
	 * @param ambient The ambient color to use for the light, null = default
	 */
	public Light(Color4f diffuse, Color4f specular, Color4f ambient) {
		this(diffuse, specular, ambient, null);
	}

	/**
	 * Create a light with the given colors, shining in the given direction. If
	 * any input are null, the default value is assumed.
	 * 
	 * @param diffuse The diffuse color to use for the light, null = default
	 * @param specular The specular color to use for the light, null = default
	 * @param ambient The ambient color to use for the light, null = default
	 * @param direction The initial direction that the light is shining
	 */
	public Light(Color4f diffuse, Color4f specular, Color4f ambient,
		Vector3f direction) {
		setAmbient(ambient);
		setDiffuse(diffuse);
		setSpecular(specular);
		setDirection(direction);
	}

	/**
	 * Get the ambient color of this light.
	 * 
	 * @return The ambient Color4f instance
	 */
	public Color4f getAmbient() {
		return ambient;
	}

	/**
	 * Set the ambient color of this light.
	 * 
	 * @param ambient The Color4f to use, if null uses default
	 */
	public void setAmbient(Color4f ambient) {
		if (ambient == null)
			ambient = new Color4f(DEFAULT_AMBIENT);
		this.ambient = ambient;
	}

	/**
	 * Get the diffuse color of this light.
	 * 
	 * @return The diffuse Color4f instance
	 */
	public Color4f getDiffuse() {
		return diffuse;
	}

	/**
	 * Set the diffuse color of this light.
	 * 
	 * @param diffuse The Color4f to use, if null uses default
	 */
	public void setDiffuse(Color4f diffuse) {
		if (diffuse == null)
			diffuse = new Color4f(DEFAULT_DIFFUSE);
		this.diffuse = diffuse;
	}

	/**
	 * Get the specular color of this light.
	 * 
	 * @return The specular Color4f instance
	 */
	public Color4f getSpecular() {
		return specular;
	}

	/**
	 * Set the specular color of this light.
	 * 
	 * @param specular The Color4f to use, if null uses the default
	 */
	public void setSpecular(Color4f specular) {
		if (specular == null)
			specular = new Color4f(DEFAULT_SPEC);
		this.specular = specular;
	}

	/**
	 * Get the direction that this light is shining. In some cases, e.g. a spot
	 * light with 180 degree spotlight arc (a point light), the direction of the
	 * light is meaningless.
	 * 
	 * @return The direction of the light
	 */
	public Vector3f getDirection() {
		return direction;
	}

	/**
	 * Set the direction that this light is shining. Has no visible effect when
	 * called on a point light, since the light shines everywhere, however the
	 * vector is still stored.
	 * 
	 * @param direction The new light direction, if null uses the default
	 */
	public void setDirection(Vector3f direction) {
		if (direction == null)
			direction = new Vector3f(0f, 0f, 1f);
		this.direction = direction;
	}
}
