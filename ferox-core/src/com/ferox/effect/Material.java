package com.ferox.effect;

import com.ferox.math.Color4f;

/**
 * Material contains the color and shininess values representative of a
 * surface's coloring and the way lights interact with it. Each color used in a
 * Material represents how much of each component of a light type is reflected
 * off of its surface. For example, if a Material has a diffuse color of red
 * (e.g. <1, 0, 0>), then only the red component of diffuse light is reflected
 * from it. A green or blue diffuse light will cause the surface to appear
 * black.
 * 
 * @author Michael Ludwig
 */
public class Material {
	private final Color4f amb;
	private final Color4f diff;
	private final Color4f spec;
	private final Color4f em;
	
	private float shininess;

	/**
	 * Creates a material with an ambient color of (.2, .2, .2, 1), a diffuse
	 * color of (.8, .8, .8, 1), a specular and emissive color of (0, 0, 0, 1),
	 * and a shininess of 0.
	 */
	public Material() {
		amb = new Color4f(.2f, .2f, .2f, 1f);
		diff = new Color4f(.8f, .8f, .8f, 1f);
		spec = new Color4f();
		em = new Color4f();
		
		setShininess(0f);
	}
	
	/**
	 * Get the emissive color used by this Material.
	 * 
	 * @return Emissive color, not null
	 */
	public Color4f getEmissive() {
		return em;
	}

	/**
	 * Get the diffuse color used by this Material.
	 * 
	 * @return Diffuse color, not null
	 */
	public Color4f getDiffuse() {
		return diff;
	}

	/**
	 * Get the shininess for this Material.
	 * 
	 * @return Shininess, will be >= 0
	 */
	public float getShininess() {
		return shininess;
	}

	/**
	 * Get the specular color used by this Material.
	 * 
	 * @return Specular color, not null
	 */
	public Color4f getSpecular() {
		return spec;
	}

	/**
	 * Get the ambient color used by this Material.
	 * 
	 * @return Ambient color, not null
	 */
	public Color4f getAmbient() {
		return amb;
	}
	
	/**
	 * Copy em into the emissive color for this Material.
	 * 
	 * @param em Emissive color, null = <0, 0, 0, 1>
	 * @return This Material
	 */
	public Material setEmissive(Color4f em) {
		if (em == null)
			em = new Color4f(0f, 0f, 0f, 1f);
		this.em.set(em);
		return this;
	}

	/**
	 * Copy diff into the diffuse color for this Material.
	 * 
	 * @param diff Diffuse color, null = <.8, .8, .8, 1>
	 * @return This Material
	 */
	public Material setDiffuse(Color4f diff) {
		if (diff == null)
			diff = new Color4f(.8f, .8f, .8f, 1f);
		this.diff.set(diff);
		return this;
	}

	/**
	 * Copy amb into the ambient color for this Material.
	 * 
	 * @param amb Ambient color, null = <.2, .2, .2, 1>
	 * @return This Material
	 */
	public Material setAmbient(Color4f amb) {
		if (amb == null)
			amb = new Color4f(.2f, .2f, .2f, 1f);
		this.amb.set(amb);
		return this;
	}

	/**
	 * Sets the shininess of this Material, a higher value represents shinier.
	 * 
	 * @param shininess Shininess, must be in [0, 128]
	 * @return This Material
	 * @throws IllegalArgumentException if shininess < 0 or > 128
	 */
	public Material setShininess(float shininess) {
		if (shininess < 0 || shininess > 128)
			throw new IllegalArgumentException("Shininess out of range [0, 128]: " + shininess);
		this.shininess = shininess;
		return this;
	}

	/**
	 * Copy spec into the specular color for this material.
	 * 
	 * @param spec Specular color, null = <0, 0, 0, 1>
	 * @return This Material
	 */
	public Material setSpecular(Color4f spec) {
		if (spec == null)
			spec = new Color4f(0f, 0f, 0f, 1f);
		this.spec.set(spec);
		return this;
	}
}
