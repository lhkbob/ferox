package com.ferox.effect;

import com.ferox.effect.EffectType.Type;
import com.ferox.math.Color;
import com.ferox.scene.SceneException;

/**
 * <p>
 * Represents a region of fog within the scene. It provides methods to set
 * color, quality, range, density and equations for the fog. The default color
 * is <.5, .5, .5, 1>, the default equation is LINEAR and the default quality is
 * DONT_CARE.
 * </p>
 * <p>
 * The amount of coloring used for a pixel rendered with a Fog depends on the
 * pixels depth. Depending slightly on the fog equation used (
 * {@link FogEquation}), the farther away a pixel is, the more fogged it will
 * be.
 * </p>
 * 
 * @author Michael Ludwig
 */
@EffectType( { Type.FOG })
public class Fog extends AbstractEffect {
	/**
	 * Equation used to compute the amount of fog between start and end
	 * distances. The exact coloring of LINEAR fog depends on the start and end
	 * values, for EXP and EXP_SQUARED, density effects the coloring.
	 */
	public static enum FogEquation {
		EXP, EXP_SQUARED, LINEAR
	}

	private static final Color DEFAULT_COLOR = new Color(.5f, .5f, .5f, 1f);
	private static final float DEFAULT_START = .1f;
	private static final float DEFAULT_END = 10f;
	private static final float DEFAULT_DENSITY = 1f;
	private static final FogEquation DEFAULT_EQ = FogEquation.LINEAR;

	private Color color;
	private float start;
	private float end;
	private float density;
	private FogEquation eq;
	private Quality qual;

	/**
	 * Creates a fog with default color, equation and quality with a range of .1
	 * to 10, and a density of 1.
	 */
	public Fog() {
		this(null);
	}

	/**
	 * Creates a fog with the given color, default equation and quality, range
	 * between .1 to 10, and a density of 1.
	 */
	public Fog(Color color) {
		this(color, DEFAULT_START, DEFAULT_END, DEFAULT_DENSITY);
	}

	/**
	 * Creates a fog with the given color, fog range and density, and the
	 * default equation and quality.
	 */
	public Fog(Color color, float start, float end, float density)
			throws IllegalArgumentException {
		this(color, start, end, density, null, null);
	}

	/**
	 * Creates a fog with the given color, fog range, density, equation and
	 * quality. If any of color, eq, or qual are null, then that value will be
	 * set to the default. Fails if start > end, or if start < 0.
	 */
	public Fog(Color color, float start, float end, float density,
			FogEquation eq, Quality qual) throws SceneException {
		setFogRange(start, end);
		setDensity(density);
		setEquation(eq);
		setQuality(qual);
		setColor(color);
	}

	/** Get the fog color. Will not be null. */
	public Color getColor() {
		return color;
	}

	/**
	 * Set the fog color. If color is null, the fog color is set to the default
	 * color.
	 */
	public void setColor(Color color) {
		if (color == null)
			color = new Color(DEFAULT_COLOR);
		this.color = color;
	}

	/**
	 * Get the starting distance, in eye space, of the fog. Objects closer than
	 * this value to the eye will not be fogged.
	 */
	public float getStartDistance() {
		return start;
	}

	/**
	 * Get the ending distance, in eye space, of the fog. Eyes farther away than
	 * this distance will be completely fogged.
	 */
	public float getEndDistance() {
		return end;
	}

	/**
	 * Set the start and end distances for this fog. Fails if start > end, or if
	 * start < 0.
	 */
	public void setFogRange(float start, float end) throws SceneException {
		if (start < 0 || start > end)
			throw new SceneException("Illegal valus for fog range: " + start
					+ " - " + end);
		this.start = start;
		this.end = end;
	}

	/** Get the fog density, a value greater than or equal to 0 */
	public float getDensity() {
		return density;
	}

	/** Set the fog density, it will be clamped to be above 0. */
	public void setDensity(float density) {
		this.density = Math.max(0f, density);
	}

	/** Get the fog equation used by this fog. */
	public FogEquation getEquation() {
		return eq;
	}

	/**
	 * Set the fog equation used by this fog. If eq is null, the equation is set
	 * to the default.
	 */
	public void setEquation(FogEquation eq) {
		if (eq == null)
			eq = DEFAULT_EQ;
		this.eq = eq;
	}

	/** Get the quality of fog computation. */
	public Quality getQuality() {
		return qual;
	}

	/**
	 * Set the quality of fog computation. If qual is null, the quality is set
	 * to the default.
	 */
	public void setQuality(Quality qual) {
		if (qual == null)
			qual = Quality.DONT_CARE;
		this.qual = qual;
	}
}
