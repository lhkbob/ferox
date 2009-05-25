package com.ferox.effect;

import com.ferox.effect.Effect.Type;
import com.ferox.math.Color;

/**
 * <p>
 * GlobalLighting controls the various parameters that affect all Lights used in
 * an EffectSet. If a GlobalLight is not present, than any Lights will be
 * ignored (in essence the GlobalLight Effect enables lighting).
 * </p>
 * <p>
 * Because the GlobalLighting Effect is fairly simple, and likely the same
 * parameters will be used for a pass, GlobalLighting instances should be shared
 * whenever possible.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Type(EffectType.GLOBAL_LIGHTING)
public class GlobalLighting extends AbstractEffect {
	private static final Color DEFAULT_AMBIENT = new Color(0f, 0f, 0f, 1f);

	private boolean separateSpec;
	private boolean localViewer;
	private boolean useTwoSidedLighting;
	private Color globalAmb;

	/**
	 * Creates a GlobalLighting that doesn't use separate specular or local
	 * viewing, and uses the default global ambient color, and no two-sided
	 * lighting.
	 */
	public GlobalLighting() {
		this(null);
	}

	/**
	 * Creates a GlobalLighting that uses the given color for ambient lighting,
	 * with no local viewing, separate specular or two-sided lighting.
	 * 
	 * @param globalAmbient Global ambient color to use
	 */
	public GlobalLighting(Color globalAmbient) {
		this(null, false, false, false);
	}

	/**
	 * Creates a GlobalLighting with the given parameters. If globalAmbient is
	 * null, uses the default.
	 * 
	 * @param globalAmbient Ambient color to use
	 * @param separateSpecular Whether or not specular is computed separately
	 * @param useLocalViewer Whether or not specular is computed from local or
	 *            infinite viewers
	 * @param twoSided Whether or not lighting is computed separately for two
	 *            sides of a polygon
	 */
	public GlobalLighting(Color globalAmbient, boolean separateSpecular,
		boolean useLocalViewer, boolean twoSided) {
		setSeparateSpecular(separateSpecular);
		setLocalViewer(useLocalViewer);
		setTwoSidedLighting(twoSided);
		setGlobalAmbient(globalAmbient);
	}

	/**
	 * Set whether or not the back face of a rendered primitive should have its
	 * lighting parameters computed separately. This can be slower, but look
	 * nicer. It should be set to false if the back face isn't ever showing
	 * since it'll be wasted effort then.
	 * 
	 * @param use If two-sided lighting is to be used
	 */
	public void setTwoSidedLighting(boolean use) {
		useTwoSidedLighting = use;
	}

	/**
	 * Return true if two sided lighting should be enabled when rendering. This
	 * might be slower, but will produce more realistic results for geometries
	 * that have their back face showing.
	 * 
	 * @return If two-siding is used
	 */
	public boolean getTwoSidedLighting() {
		return useTwoSidedLighting;
	}

	/**
	 * Whether or not specular highlights are computed in a second computation.
	 * True is slower, but provides better realism. Default is false.
	 * 
	 * @return If separate specular computation is used
	 */
	public boolean getSeparateSpecular() {
		return separateSpec;
	}

	/**
	 * Set whether or not to compute specular separate from other lighting
	 * computations. True is slower and more accurate. Default is false.
	 * 
	 * @param separateSpec If specular highlights are computed separately
	 */
	public void setSeparateSpecular(boolean separateSpec) {
		this.separateSpec = separateSpec;
	}

	/**
	 * Whether or not specular lighting is computed from a local viewer. True is
	 * slower, but more realistic.
	 * 
	 * @return If a local viewer is used
	 */
	public boolean isLocalViewer() {
		return localViewer;
	}

	/**
	 * Set whether or not specular lighting is computed using a local viewer, or
	 * one that is infinitely far away (changes in view won't change specular
	 * highlight). True is slower, but nicer looking. Default is false.
	 * 
	 * @param localViewer If a local viewer should be used
	 */
	public void setLocalViewer(boolean localViewer) {
		this.localViewer = localViewer;
	}

	/**
	 * Get the global ambient light applied to anything rendered. This ambient
	 * light is applied in addition to any intersecting lights, and will be
	 * present even if nothing lights the scene element.
	 * 
	 * @return Current global ambient color, own't be null
	 */
	public Color getGlobalAmbient() {
		return globalAmb;
	}

	/**
	 * Set the global ambient light color.
	 * 
	 * @param globalAmb Color to use, null defaults to black
	 */
	public void setGlobalAmbient(Color globalAmb) {
		if (globalAmb == null)
			globalAmb = new Color(DEFAULT_AMBIENT);
		this.globalAmb = globalAmb;
	}

	@Override
	public String toString() {
		return "(GlobalLighting ambient: " + globalAmb + " separateSpec: "
			+ separateSpec + " local: " + localViewer + " twoSided: "
			+ useTwoSidedLighting + ")";
	}
}
