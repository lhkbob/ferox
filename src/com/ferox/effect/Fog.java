package com.ferox.effect;

import com.ferox.effect.EffectType.Type;
import com.ferox.math.Color;

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
	 * 
	 * @param color The color to use
	 */
	public Fog(Color color) {
		this(color, DEFAULT_START, DEFAULT_END, DEFAULT_DENSITY);
	}

	/**
	 * Creates a fog with the given color, fog range and density, and the
	 * default equation and quality.
	 * 
	 * @param color The color to use
	 * @param start The start distance
	 * @param end The ending distance (distance at which fog is densest)
	 * @param density The density of the fog
	 * @throws IllegalArgumentException if start > end
	 */
	public Fog(Color color, float start, float end, float density) {
		this(color, start, end, density, null, null);
	}

	/**
	 * Creates a fog with the given color, fog range, density, equation and
	 * quality. If any of color, eq, or qual are null, then that value will be
	 * set to the default.
	 * 
	 * @param color The color to use
	 * @param start The start distance
	 * @param end The ending distance (distance at which fog is densest)
	 * @param density The density of the fog
	 * @param eq The FogEquation to use
	 * @param qual The quality of the rendered fog
	 * @throws IllegalArgumentException if start > end
	 */
	public Fog(Color color, float start, float end, float density,
		FogEquation eq, Quality qual) {
		setFogRange(start, end);
		setDensity(density);
		setEquation(eq);
		setQuality(qual);
		setColor(color);
	}

	/**
	 * @return The Color instance storing the color of this Fog
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Set the fog color. If color is null, the fog color is set to the default
	 * color.
	 * 
	 * @param color The new color to use
	 */
	public void setColor(Color color) {
		if (color == null)
			color = new Color(DEFAULT_COLOR);
		this.color = color;
	}

	/**
	 * Get the starting distance, in eye space, of the fog. Objects closer than
	 * this value to the eye will not be fogged. This only affects a linear fog.
	 * 
	 * @return The start distance
	 */
	public float getStartDistance() {
		return start;
	}

	/**
	 * Get the ending distance, in eye space, of the fog. Eyes farther away than
	 * this distance will be completely fogged. This only affects a linear fog
	 * 
	 * @return The ending distance of the fog
	 */
	public float getEndDistance() {
		return end;
	}

	/**
	 * Set the start and end distances for this fog.
	 * 
	 * @see #getEndDistance()
	 * @see #getStartDistance()
	 * @param start The new start distance
	 * @param end The new ending distance
	 * @throws IllegalArgumentException if start > end, or start < 0
	 */
	public void setFogRange(float start, float end) {
		if (start < 0 || start > end)
			throw new IllegalArgumentException("Illegal valus for fog range: "
				+ start + " - " + end);
		this.start = start;
		this.end = end;
	}

	/**
	 * @return The fog density, a value greater than or equal to 0
	 */
	public float getDensity() {
		return density;
	}

	/**
	 * Set the fog density, it will be clamped to be above 0.
	 * 
	 * @param density The new density value
	 */
	public void setDensity(float density) {
		this.density = Math.max(0f, density);
	}

	/** @return The fog equation used by this fog. */
	public FogEquation getEquation() {
		return eq;
	}

	/**
	 * Set the fog equation used by this fog. If eq is null, the equation is set
	 * to the default.
	 * 
	 * @param eq The new fog equation
	 */
	public void setEquation(FogEquation eq) {
		if (eq == null)
			eq = DEFAULT_EQ;
		this.eq = eq;
	}

	/**
	 * @return The quality of fog computation.
	 */
	public Quality getQuality() {
		return qual;
	}

	/**
	 * Set the quality of fog computation. If qual is null, the quality is set
	 * to the default.
	 * 
	 * @param qual The new fog rendering quality
	 */
	public void setQuality(Quality qual) {
		if (qual == null)
			qual = Quality.DONT_CARE;
		this.qual = qual;
	}
}
