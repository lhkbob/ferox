package com.ferox.scene2;

import com.ferox.effect.Fog;
import com.ferox.effect.Effect.Quality;
import com.ferox.effect.Fog.FogEquation;
import com.ferox.math.Color4f;
import com.ferox.math.bounds.BoundSphere;

/**
 * A FogNode embodies a localized Fog effect. It provides methods that mirror
 * those defined in Fog so that the Fog effect used can be easily and quickly
 * modified.
 * 
 * @author Michael Ludwig
 */
public class FogNode extends AbstractSceneElement {
	private final Fog fog;

	/**
	 * Create a FogNode with a Fog created with the given arguments.
	 */
	public FogNode() {
		fog = new Fog();
	}

	/**
	 * Create a FogNode with a Fog created with the given arguments.
	 * 
	 * @param color
	 */
	public FogNode(Color4f color) {
		fog = new Fog(color);
	}

	/**
	 * Create a FogNode with a Fog created with the given arguments.
	 * 
	 * @param color
	 * @param start
	 * @param end
	 * @param density
	 */
	public FogNode(Color4f color, float start, float end, float density) {
		fog = new Fog(color, start, end, density);
	}

	/**
	 * Create a FogNode with a Fog created with the given arguments.
	 * 
	 * @param color
	 * @param start
	 * @param end
	 * @param density
	 * @param eq
	 * @param qual
	 */
	public FogNode(Color4f color, float start, float end, float density, 
				   FogEquation eq, Quality qual) {
		fog = new Fog(color, start, end, density, eq, qual);
	}

	/**
	 * @return The Fog effect that is represented by this FogNode
	 */
	public Fog getFog() {
		return fog;
	}

	/**
	 * Convenience method to set this FogNode's local bounds to a new
	 * BoundSphere centered at its local origin, with the given radius.
	 * 
	 * @param radius The radius of the new local bounds sphere
	 * @throws IllegalArgumentException if radius <= 0
	 */
	public void setEffectRadius(float radius) {
		if (radius <= 0f)
			throw new IllegalArgumentException("Invalid effect radius: " + radius);
		BoundSphere local = new BoundSphere(radius);
		setBounds(local);
	}

	/**
	 * Identical operation to Fog's getColor().
	 * 
	 * @return The Color4f instance used for the fog color
	 */
	public Color4f getColor() {
		return fog.getColor();
	}

	/**
	 * Identical operation to Fog's getDensity().
	 * 
	 * @return The density used for this node's fog
	 */
	public float getDensity() {
		return fog.getDensity();
	}

	/**
	 * Identical operation to Fog's getStartDistance().
	 * 
	 * @return The start distance used for this node's fog
	 */
	public float getStartDistance() {
		return fog.getStartDistance();
	}

	/**
	 * Identical operation to Fog's getEndDistance().
	 * 
	 * @return The end distance used for this node's fog
	 */
	public float getEndDistance() {
		return fog.getEndDistance();
	}

	/**
	 * Identical operation to Fog's getEquation().
	 * 
	 * @return The fog equation used for this node's fog
	 */
	public FogEquation getEquation() {
		return fog.getEquation();
	}

	/**
	 * Identical operation to Fog's getQuality().
	 * 
	 * @return The quality used for this node's fog
	 */
	public Quality getQuality() {
		return fog.getQuality();
	}

	/**
	 * Identical operation to Fog's setColor(color).
	 * 
	 * @param color The new fog color
	 */
	public void setColor(Color4f color) {
		fog.setColor(color);
	}

	/**
	 * Identical operation to Fog's setDensity(density).
	 * 
	 * @param density The new start distance
	 */
	public void setDensity(float density) {
		fog.setDensity(density);
	}

	/**
	 * Identical operation to Fog's setFogRange(start, end)
	 * 
	 * @param start The start distance
	 * @param end The end distance
	 */
	public void setFogRange(float start, float end) {
		fog.setFogRange(start, end);
	}

	/**
	 * Identical operation to Fog's setEquation(eq)
	 * 
	 * @param eq The new fog equation
	 */
	public void setEquation(FogEquation eq) {
		fog.setEquation(eq);
	}

	/**
	 * Identical operation to Fog's setQuality(qual)
	 * 
	 * @param qual The new fog quality
	 */
	public void setQuality(Quality qual) {
		fog.setQuality(qual);
	}
}
