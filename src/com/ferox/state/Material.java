package com.ferox.state;

import com.ferox.math.Color;

/** 
 * A material represents the surface color properties of an appearance.
 * The default material has an ambient of <.2, .2, .2, 1> (dark gray),
 * a diffuse of <.8, .8, .8, 1> (light gray) and a white specular, with low
 * shininess. 
 * 
 * Whenever colors are used, they are stored by reference, so any changes
 * to the color object will be immediately reflected in the scene and in
 * any material object that uses that color object.
 * 
 * Materials support alpha values, but it is likely that for values < 1, 
 * you will only get correct results if it is combined with a blend mode state.
 * 
 * A material affects both the front side and back side of something rendered
 * equally.  Different surface materials may be added later on.
 * 
 * @author Michael Ludwig
 *
 */
public class Material implements State {
	private static final Color DEFAULT_AMBIENT = new Color(.2f, .2f, .2f);
	private static final Color DEFAULT_DIFFUSE = new Color(.8f, .8f, .8f);
	private static final Color DEFAULT_SPEC = new Color(1f, 1f, 1f);
	private static final float DEFAULT_SHININESS = 5f;
	
	private Color amb;
	private Color diff;
	private Color spec;
	private float shininess;
	
	private boolean smooth;
	
	private Object renderData;
	
	/** Creates a material with default colors and shiniess. */
	public Material() {
		this(null);
	}
	
	/** Creates a material with the default ambient and spec, and default shininess 
	 * and the given diffuse. If diffuse is null, set to default. */
	public Material(Color diff) {
		this(diff, null, DEFAULT_SHININESS);
	}

	/** Creates a material with the default ambient, and given diffuse, specular colors
	 * and shininess.  If either diff or spec are null, uses the defaults.  Shininess
	 * is clamped to be above (or equal to) 0. */
	public Material(Color diff, Color spec, float shininess) {
		this(diff, spec, null, shininess);
	}
	
	/** Creates a material with the given ambient, diffuse and specular colors
	 * and given shininess.  If null is given, default value is used.  Shininess is clamped
	 * to be above (or equal to) 0. */
	public Material(Color diff, Color spec, Color ambient, float shininess) {
		this.setDiffuse(diff);
		this.setSpecular(spec);
		this.setAmbient(ambient);
		this.setShininess(shininess);
		this.setSmoothShaded(true);
	}
	
	/** Return true if the Material will have lighting interpolated
	 * smoothly across primitives, or if it will be flat shaded. */
	public boolean isSmoothShaded() {
		return this.smooth;
	}
	
	/** Set if a primitive will be smoothly shaded (true), or
	 * if it will be "flat" or faceted. */
	public void setSmoothShaded(boolean smooth) {
		this.smooth = smooth;
	}
	
	/** Get the diffuse color used by this material.  */
	public Color getDiffuse() {
		return this.diff;
	}

	/** Get the shininess for this material. */
	public float getShininess() {
		return this.shininess;
	}

	/** Get the specular color used by this material.  */
	public Color getSpecular() {
		return this.spec;
	}
	
	/** Get the ambient color used by this material.  */
	public Color getAmbient() {
		return this.amb;
	}
	
	/** Set the diffuse color for this material.  If null, creates a new
	 * color that is the default values (<.8, .8, .8, 1>). */
	public void setDiffuse(Color diff) {
		if (diff == null)
			diff = new Color(DEFAULT_DIFFUSE);
		this.diff = diff;
	}

	/** Set the ambient color for this material.  If null, creates a new
	 * color that is the default values (<.2, .2, .2, 1>). */
	public void setAmbient(Color amb) {
		if (amb == null)
			amb = new Color(DEFAULT_AMBIENT);
		this.amb = amb;
	}
	
	/** Sets the shininess of this material, a higher value represents shinier.
	 * This is clamped to be >= 0. */
	public void setShininess(float shininess) {
		this.shininess = Math.max(0, shininess);
	}

	/** Set the specular color for this material.  If null, creates a new
	 * color that is the default values (<1, 1, 1, 1>). */
	public void setSpecular(Color spec) {
		if (spec == null)
			spec = new Color(DEFAULT_SPEC);
		this.spec = spec;
	}
	
	@Override
	public Role getRole() {
		return Role.MATERIAL;
	}

	@Override
	public Object getStateData() {
		return this.renderData;
	}
	
	@Override
	public void setStateData(Object data) {
		this.renderData = data;
	}
}
