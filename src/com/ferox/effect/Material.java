package com.ferox.effect;

import com.ferox.effect.EffectType.Type;
import com.ferox.math.Color;

/**
 * <p>
 * A material represents the surface color properties of an appearance. The
 * default material has an ambient of <.2, .2, .2, 1> (dark gray), a diffuse of
 * <.8, .8, .8, 1> (light gray) and a white specular, with low shininess.
 * </p>
 * <p>
 * Whenever colors are used, they are stored by reference, so any changes to the
 * color object will be immediately reflected in the scene and in any material
 * object that uses that color object.
 * </p>
 * <p>
 * Materials support alpha values, but it is likely that for values < 1, you
 * will only get correct results if it is combined with a blend mode state.
 * </p>
 * <p>
 * A material affects both the front side and back side of something rendered
 * equally. Different surface materials may be added later on.
 * </p>
 * 
 * @author Michael Ludwig
 */
@EffectType({Type.MATERIAL})
public class Material extends AbstractEffect {
	private static final Color DEFAULT_AMBIENT = new Color(.2f, .2f, .2f);
	private static final Color DEFAULT_DIFFUSE = new Color(.8f, .8f, .8f);
	private static final Color DEFAULT_SPEC = new Color(1f, 1f, 1f);
	private static final float DEFAULT_SHININESS = 5f;

	private Color amb;
	private Color diff;
	private Color spec;
	private float shininess;

	private boolean smooth;

	/** Creates a material with default colors and shiniess. */
	public Material() {
		this(null);
	}

	/**
	 * Creates a material with the default ambient and spec, and default
	 * shininess and the given diffuse.
	 * 
	 * @param diff Diffuse color, null uses default
	 */
	public Material(Color diff) {
		this(diff, null, DEFAULT_SHININESS);
	}

	/**
	 * Creates a material with the default ambient, and given diffuse, specular
	 * colors and shininess. Shininess is clamped to be above (or equal to) 0.
	 * 
	 * @param diff Diffuse color, null uses default
	 * @param spec Specular color, null uses default
	 * @param shininess Shininess, clamped >= 0
	 */
	public Material(Color diff, Color spec, float shininess) {
		this(diff, spec, null, shininess);
	}

	/**
	 * Creates a material with the given ambient, diffuse and specular colors
	 * and given shininess. If null is given, default value is used. Shininess
	 * is clamped to be above (or equal to) 0.
	 * 
	 * @param diff Diffuse color, null uses default
	 * @param spec Specular color, null uses default
	 * @param ambient Ambient color, null uses default
	 * @param shininess Shininess, clamped >= 0
	 */
	public Material(Color diff, Color spec, Color ambient, float shininess) {
		setDiffuse(diff);
		setSpecular(spec);
		setAmbient(ambient);
		setShininess(shininess);
		setSmoothShaded(true);
	}

	/**
	 * Return true if the Material will have lighting interpolated smoothly
	 * across primitives, or if it will be flat shaded.
	 * 
	 * @return If lighting is smoothed over polygons
	 */
	public boolean isSmoothShaded() {
		return smooth;
	}

	/**
	 * Set if a primitive will be smoothly shaded (true), or if it will be
	 * "flat" or faceted.
	 * 
	 * @param smooth Whether or not smooth lighting is used
	 */
	public void setSmoothShaded(boolean smooth) {
		this.smooth = smooth;
	}

	/**
	 * Get the diffuse color used by this material.
	 * 
	 * @return Diffuse color, not null
	 */
	public Color getDiffuse() {
		return diff;
	}

	/**
	 * Get the shininess for this material.
	 * 
	 * @return Shininess, will be >= 0
	 */
	public float getShininess() {
		return shininess;
	}

	/**
	 * Get the specular color used by this material.
	 * 
	 * @return Specular color, not null
	 */
	public Color getSpecular() {
		return spec;
	}

	/**
	 * Get the ambient color used by this material.
	 * 
	 * @return Ambient color, not null
	 */
	public Color getAmbient() {
		return amb;
	}

	/**
	 * Set the diffuse color for this material.
	 * 
	 * @param diff Diffuse color, null = <.8, .8, .8, 1>
	 */
	public void setDiffuse(Color diff) {
		if (diff == null)
			diff = new Color(DEFAULT_DIFFUSE);
		this.diff = diff;
	}

	/**
	 * Set the ambient color for this material. If null, creates a new color
	 * that is the default values (<.2, .2, .2, 1>).
	 * 
	 * @param amb Ambient color, null = <.2, .2, .2, 1>
	 */
	public void setAmbient(Color amb) {
		if (amb == null)
			amb = new Color(DEFAULT_AMBIENT);
		this.amb = amb;
	}

	/**
	 * Sets the shininess of this material, a higher value represents shinier.
	 * 
	 * @param shininess Shininess, clamped >= 0
	 */
	public void setShininess(float shininess) {
		this.shininess = Math.max(0, shininess);
	}

	/**
	 * Set the specular color for this material.
	 * 
	 * @param spec Specular color, null = <1, 1, 1, 1>
	 */
	public void setSpecular(Color spec) {
		if (spec == null)
			spec = new Color(DEFAULT_SPEC);
		this.spec = spec;
	}

	@Override
	public String toString() {
		return "(Material ambient: " + amb + " diffuse: " + diff
				+ " specular: " + spec + " shininess: " + shininess
				+ " smoothed: " + smooth + ")";
	}
}
