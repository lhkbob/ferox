package com.ferox.shader;

import com.ferox.math.Color4f;

/**
 * <p>
 * Fog encapsulates the state necessary to achieve an approximation to a fogged
 * environment. It works by blending a fog color with the post-texturing color
 * of an incoming pixel. This fogged color is then written into the color buffer
 * per normal (possibly including blending).
 * <p>
 * <p>
 * The fogged color is computed as follows:
 * <ol>
 * <li>c = eye-space distance from camera to the pixel</li>
 * <li>d = density of the fog, as configured</li>
 * <li>e = end distance of the fog, as configured</li>
 * <li>s = start distance of the fog, as configured</li>
 * <li>f = blending factor computed from c, d, e, and/or s based on configured
 * FogEquation</li>
 * <li>Cf = 4-component fog color</li>
 * <li>Cr = 4-component textured color, before fogging</li>
 * <li>C = final fogged color</li>
 * </ol>
 * 
 * <pre>
 * Final Color:
 * C = f * Cr + (1 - f) * Cf
 * </pre>
 * 
 * </p>
 * <p>
 * The blending factor f is computed as follows:
 * 
 * <pre>
 * FogEquation |    'f'
 * ------------|------------------
 * EXP         | e&circ;(-d * c)
 * EXP_SQUARED | e&circ;((-d * c)&circ;2)
 * LINEAR      | (e - c) / (e - s)
 * </pre>
 * 
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Fog {
	/**
	 * The equation used to compute the blending factor for the fog. See above
	 * for descriptions of each equation.
	 */
	public static enum FogEquation {
		EXP, EXP_SQUARED, LINEAR
	}

	private Color4f color;
	private float start;
	private float end;
	private float density;
	private FogEquation eq;

	/**
	 * Create a new Fog state that uses a linear range of 0 to 1, has a density
	 * of 1, uses the EXP FogEquation and has a fog color of (0, 0, 0, 0).
	 */
	public Fog() {
		setColor(new Color4f(0f, 0f, 0f, 0f));
		setLinearFogRange(0f, 1f);
		setDensity(1f);
		setEquation(FogEquation.EXP);
	}

	/**
	 * Return the Fog's color, any changes to this Color4f will directly affect
	 * the Fog.  This is Cf as described above.
	 * 
	 * @return The Fog's color
	 */
	public Color4f getColor() {
		return color;
	}

	/**
	 * Assign a new Color4f as this Fog's color. This is taken as a reference,
	 * so any changes to color will affect the Fog's color.  If color is null,
	 * it uses (0, 0, 0, 0) instead.
	 * 
	 * @param color The new Fog color
	 * @return This Fog
	 */
	public Fog setColor(Color4f color) {
		this.color = (color != null ? color : new Color4f(0f, 0f, 0f, 0f));
		return this;
	}

	/**
	 * Return the linear start distance, which is used if the Fog's equation is
	 * LINEAR. This is s as described above.
	 * 
	 * @return The Fog's start distance for the LINEAR equation
	 */
	public float getLinearStartDistance() {
		return start;
	}

	/**
	 * Return the linear end distance, which is used if the Fog's equation is
	 * LINEAR. This is e as described above.
	 * 
	 * @return The Fog's end distance for the LINEAR equation
	 */
	public float getLinearEndDistance() {
		return end;
	}

	/**
	 * Set the linear fog range to use if the Fog's equation is LINEAR. The
	 * value start represents s as above, and end represents e as above. start
	 * must be greater than 0 and cannot be equal to or greater than end.
	 * 
	 * @param start The start distance for LINEAR
	 * @param end The end distance for LINEAR
	 * @return This Fog
	 * @throws IllegalArgumentException if start < 0 or if start >= end
	 */
	public Fog setLinearFogRange(float start, float end) {
		if (start < 0 || start >= end)
			throw new IllegalArgumentException("Illegal valus for fog range: " + start + " , " + end);
		this.start = start;
		this.end = end;
		return this;
	}

	/**
	 * Return the fog density that is used if the Fog's equation is EXP or
	 * EXP_SQUARED. This is d as described above.
	 * 
	 * @return The Fog's density
	 */
	public float getDensity() {
		return density;
	}

	/**
	 * Set the fog density to use if the Fog's equation is EXP or EXP_SQUARED.
	 * This is d as described above. If density is < 0, then an exception is
	 * thrown.
	 * 
	 * @param density The fog density
	 * @return This Fog
	 * @throws IllegalArgumentException if density < 0
	 */
	public Fog setDensity(float density) {
		if (density < 0f)
			throw new IllegalArgumentException("Fog density must be positive");
		this.density = density;
		return this;
	}

	/**
	 * Return the FogEquation that is used by this Fog. See above for
	 * descriptions of each FogEquation.
	 * 
	 * @return This Fog's FogEquation
	 */
	public FogEquation getEquation() {
		return eq;
	}

	/**
	 * Set the FogEquation to use for this Fog. See above for each equation's
	 * behaviors. If eq is null, then EXP is assigned.
	 * 
	 * @param eq The new FogEquation
	 * @return This Fog
	 */
	public Fog setEquation(FogEquation eq) {
		this.eq = (eq != null ? eq : FogEquation.EXP);
		return this;
	}
}
