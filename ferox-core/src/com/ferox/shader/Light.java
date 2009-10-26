package com.ferox.shader;

import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;

/**
 * <p>
 * Light represents the necessary state to describe a single light source that
 * affects rendered primitives. Each light source can be enabled or disabled.
 * When enabled, a primitive's color is modified by combining this Light's
 * ambient, diffuse, and specular colors with the current Material colors taking
 * into account the Light's contributions based on relative direction to the
 * surface's normal, etc.
 * </p>
 * <p>
 * The color from all light sources are computed as follows:
 * <ol>
 * <li>Me = emissive color of the current Material</li>
 * <li>Ma = ambient color of the current Material</li>
 * <li>Md = diffuse color of the Material</li>
 * <li>Ms = specular color of the Material</li>
 * <li>ms = shininess of material</li>
 * <li>Ga = global ambient light color</li>
 * <li>Lai = ambient color for ith light</li>
 * <li>Ldi = diffuse color for ith light</li>
 * <li>Lsi = specular color for ith light</li>
 * <li>N = normal vector of surface</li>
 * <li>H = half vector between normal and eye vectors</li>
 * <li>V = vector from view point to the surface</li>
 * <li>D = vector from light to surface (either V - P for point/spot lights, or
 * (Px, Py, Pz) for direction lights)</li>
 * <li>Ati = attenuation, which is 1 / (c + l * d + q * d^2), where d = |D| and
 * c = constant att., l = linear att. and q = quadratic att.</li>
 * <li>Spi = spot light factor, which cuts off lighting if outside of the
 * spotlight cone</li>
 * </ol>
 * The light color is computed as:
 * 
 * <pre>
 * Fc = Me + Ma * Ga + sum(0, num_lights:
 *                         Ati * Spi * (Ma * Lai + 
 *                                      max(n ¥ D, 0) * Md * Ldi +
 *                                      max(n ¥ H, 0)&circ;ms * Ms * Lsi))
 * </pre>
 * 
 * Note that vector multiplication with '*' is done component wise, while the
 * '¥' operator represents a dot product. Also, for max(n ¥ H, 0)^ms, we
 * evaluate 0^0 as 0.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Light {
	private final Color4f ambient;
	private final Color4f diffuse;
	private final Color4f specular;

	private final Vector4f position;

	private final Vector3f spotDirection;
	private float spotAngle;

	private float constAttenuation;
	private float linearAttenuation;
	private float quadAttenuation;

	private boolean enabled;

	/**
	 * Construct a new Light that has an ambient color of (0, 0, 0, 1), a
	 * specular and diffuse color of (1, 1, 1, 1), a spot light direction of (0,
	 * 0, -1), a spot angle of 180 degrees, no attenuation. The Light's position
	 * is configured so that it's a direction light shining down the negative
	 * z-axis. It starts out disabled.
	 */
	public Light() {
		ambient = new Color4f(0f, 0f, 0f, 1f);
		diffuse = new Color4f(1f, 1f, 1f, 1f);
		specular = new Color4f(1f, 1f, 1f, 1f);
		
		position = new Vector4f(0f, 0f, 1f, 0f);
		spotDirection = new Vector3f(0f, 0f, -1f);
		
		setSpotlightAngle(180f);

		setConstantAttenuation(1f);
		setLinearAttenuation(0f);
		setQuadraticAttenuation(0f);

		setEnabled(false);
	}

	/**
	 * Return true if this Light is enabled, and should contribute to the
	 * overall lighting of primitives.
	 * 
	 * @return True if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set whether or not this Light is enabled. If it is enabled, it will help
	 * contribute to the overall lighting of a shape, assuming that lighting in
	 * general is also enabled. If no lights are enabled, a primitive will only
	 * be lit by the global ambient light.
	 * 
	 * @param enabled Whether or not this Light is enabled
	 * @return This Light
	 */
	public Light setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	/**
	 * Return the current position vector instance used by this Light. If the
	 * 4th component is 0, then this Light is an infinite point light ( a
	 * direction light shining in the opposite direction).
	 * 
	 * @see #setPosition(Vector4f)
	 * @return The Light's position
	 */
	public Vector4f getPosition() {
		return position;
	}

	/**
	 * <p>
	 * Copy pos into this Light's position, which is used in two ways: light rendered
	 * polygons from the given position, and implicitly determine the light's
	 * type.  If pos is null, then it uses (0, 0, 1, 0) instead.
	 * </p>
	 * <p>
	 * Because the position is a 4-dimensional vector, the 4th component can be
	 * interpreted as a 0 or a 1. If the 4th component is 0, the Light is an
	 * infinitely far away point light, or a direction light. If the direction
	 * light has a position of <x, y, z, 0>, then it shines in the direction
	 * <-x, -y, -z>.
	 * </p>
	 * 
	 * @param pos The new light position vector
	 * @return This Light
	 */
	public Light setPosition(Vector4f pos) {
		if (pos == null)
			position.set(0f, 0f, 1f, 0f);
		else
			position.set(pos);
		return this;
	}

	/**
	 * Return the direction that this Light shines if it's configured as a spot
	 * light with a cutoff angle less than 180.
	 * 
	 * @return The spot light's direction
	 */
	public Vector3f getSpotlightDirection() {
		return spotDirection;
	}

	/**
	 * Copy dir into the light direction used for spot lights. If dir is null, then it
	 * uses (0, 0, -1). The spot light direction only matters if the light is
	 * not a direction light, and the spot angle cutoff is not 180 degrees. It
	 * is assumed that the given vector is normalized.
	 * 
	 * @param dir The new light direction for spot lights
	 * @return This Light
	 */
	public Light setSpotlightDirection(Vector3f dir) {
		if (dir == null)
			spotDirection.set(0f, 0f, -1f);
		else
			spotDirection.set(dir);
		return this;
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
	 * Copy ambient into the ambient color of this light.
	 * 
	 * @param ambient The Color4f to use, if null uses (0,0,0,1)
	 * @return This Light
	 */
	public Light setAmbient(Color4f ambient) {
		if (ambient == null)
			this.ambient.set(0f, 0f, 0f, 1f);
		else
			this.ambient.set(ambient);
		return this;
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
	 * Copy diffuse into the diffuse color of this light.
	 * 
	 * @param diffuse The Color4f to use, if null uses (1,1,1,1)
	 * @return This Light
	 */
	public Light setDiffuse(Color4f diffuse) {
		if (diffuse == null)
			this.diffuse.set(1f, 1f, 1f, 1f);
		else
			this.diffuse.set(diffuse);
		return this;
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
	 * Copy specular into the specular color of this light.
	 * 
	 * @param specular The Color4f to use, if null uses (1,1,1,1)
	 * @return This Light
	 */
	public Light setSpecular(Color4f specular) {
		if (specular == null)
			this.specular.set(1f, 1f, 1f, 1f);
		else
			this.specular.set(specular);
		return this;
	}

	/**
	 * Get the constant attenuation factor for this light. Attenuation only
	 * affects the lighting if this light is configured as a spot or point
	 * light.
	 * 
	 * @return Constant attenuation (>= 0)
	 */
	public float getConstantAttenuation() {
		return constAttenuation;
	}

	/**
	 * Set the constant attenuation factor for this light. Attenuation only
	 * affects the lighting if this light is configured as a spot or point
	 * light.
	 * 
	 * @param constAtt Constant attenuation
	 * @return This Light
	 * @throws IllegalArgumentException if constAtt < 0
	 */
	public Light setConstantAttenuation(float constAtt) {
		if (constAtt < 0)
			throw new IllegalArgumentException("Constant attenuation must be > 0");
		constAttenuation = constAtt;
		return this;
	}

	/**
	 * Get the linear attenuation factor for this spot light. Attenuation only
	 * affects the lighting if this light is configured as a spot or point
	 * light.
	 * 
	 * @return Linear attenuation (>= 0)
	 */
	public float getLinearAttenuation() {
		return linearAttenuation;
	}

	/**
	 * Set the linear attenuation factor for this spot light. Attenuation only
	 * affects the lighting if this light is configured as a spot or point
	 * light.
	 * 
	 * @param linAtt Linear attenuation
	 * @return This Light
	 * @throws IllegalArgumentException if linAtt is < 0
	 */
	public Light setLinearAttenuation(float linAtt) {
		if (linAtt < 0f)
			throw new IllegalArgumentException("Linear attenuation must be > 0");
		linearAttenuation = linAtt;
		return this;
	}

	/**
	 * Get the quadratic attenuation factor for this spot light. Attenuation
	 * only affects the lighting if this light is configured as a spot or point
	 * light.
	 * 
	 * @return Quadratic attenuation (>= 0)
	 */
	public float getQuadraticAttenuation() {
		return quadAttenuation;
	}

	/**
	 * Set the quadratic attenuation factor for this spot light. Attenuation
	 * only affects the lighting if this light is configured as a spot or point
	 * light.
	 * 
	 * @param quadAtt Quadratic attenuation
	 * @return This Light
	 * @throws IllegalArgumentException if quadAtt is < 0
	 */
	public Light setQuadraticAttenuation(float quadAtt) {
		if (quadAtt < 0f)
			throw new IllegalArgumentException("Quadratic attenuation must be > 0");
		quadAttenuation = quadAtt;
		return this;
	}

	/**
	 * <p>
	 * Get the spot light cutoff angle for this Light. Primitives that are
	 * outside the cone formed by the Light's spot light direction and this
	 * angle will not be lit. It will be a value between 0 and 90 degrees, or
	 * 180 degrees. If it's 180, then the 'cone' formed above is actually a
	 * sphere and can be considered a point light.
	 * </p>
	 * <p>
	 * The spot light angle is ignored if the Light is configured as a direction
	 * light.</p.
	 * 
	 * @return The spot light cutoff angle in degrees, in [0, 90] or 180
	 */
	public float getSpotlightAngle() {
		return spotAngle;
	}

	/**
	 * Set the spot light cutoff angle for this spot light, in degrees. The
	 * angle must be in degrees and must be in [0, 90] or equal to 180. The
	 * angle only has an effect if the Light is not a direction light.
	 * 
	 * @param spotCutoff The new spotlight cutoff angle to use
	 * @return This Light
	 * @throws IllegalArgumentException if spotCutoff is not 180 and not in [0,
	 *             90]
	 */
	public Light setSpotlightAngle(float spotCutoff) {
		if (spotCutoff != 180f && (spotCutoff < 0 || spotCutoff > 90f))
			throw new IllegalArgumentException("Spot cutoff angle is not 180 and not in [0, 90]: " + spotCutoff);

		spotAngle = spotCutoff;
		return this;
	}
}
