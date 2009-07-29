package com.ferox.scene2;

import com.ferox.effect.Light;
import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.BoundSphere;

/**
 * A LightNode represents an leaf that can be added to a scene that represents a
 * Light effect. It will properly update the light effect to represent its world
 * position within the scene. It also provides convenience methods that directly
 * modify the light effect.
 * 
 * @author Michael Ludwig
 * @param <T> The specific sub-type of Light that should be used
 */
public class LightNode<T extends Light> extends AbstractSceneElement {
	/** Light instance returned by getLight(), and passed into the constructor. */
	protected final T light;

	private final Vector3f localDirection;

	/**
	 * <p>
	 * Create a new LightNode that uses the given Light instance when setting
	 * the colors and other properties of the light. This Light instance will be
	 * used throughout the node's lifetime.
	 * </p>
	 * <p>
	 * Subclasses should provide a public constructor that mimics the
	 * constructor of lights, and automatically instantiate the light instance
	 * to use here. The initial light direction is copied from the given
	 * instance's direction.
	 * </p>
	 * 
	 * @param light The Light that is held by this LightNode
	 */
	protected LightNode(T light) {
		if (light == null)
			throw new NullPointerException("Sub-classes must pass in a non-null light");
		this.light = light;
		this.localDirection = new Vector3f(light.getDirection());
	}

	/**
	 * Convenience method to set this LightNode's local bounds to a new
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
	 * @return The Light effect that represents this LightNode.
	 */
	public T getLight() {
		return light;
	}

	/**
	 * Identical operation to Light's getAmbient().
	 * 
	 * @return Color4f instance used for the light's ambient color
	 */
	public Color4f getAmbient() {
		return light.getAmbient();
	}

	/**
	 * Identical operation to Light's getDiffuse().
	 * 
	 * @return Color4f instance used for the light's diffuse color
	 */
	public Color4f getDiffuse() {
		return light.getDiffuse();
	}

	/**
	 * Identical operation to Light's getSpecular().
	 * 
	 * @return Color4f instance used for the light's specular color
	 */
	public Color4f getSpecular() {
		return light.getSpecular();
	}

	/**
	 * Return the direction of this Light, relative to its local coordinate
	 * system. This Node's light effect will use a direction vector that has
	 * been properly converted into world space.
	 * 
	 * @return Vector3f instance used for the light's local direction
	 */
	public Vector3f getLocalDirection() {
		return localDirection;
	}

	/**
	 * Identical operation to Light's setAmbient(color).
	 * 
	 * @param ambient The ambient color to use
	 */
	public void setAmbient(Color4f ambient) {
		light.setAmbient(ambient);
	}

	/**
	 * Identical operation to Light's setDiffuse(color).
	 * 
	 * @param diffuse The diffuse color to use
	 */
	public void setDiffuse(Color4f diffuse) {
		light.setDiffuse(diffuse);
	}

	/**
	 * Identical operation to Light's setSpecular(color).
	 * 
	 * @param specular The specular color to use
	 */
	public void setSpecular(Color4f specular) {
		light.setSpecular(specular);
	}

	/**
	 * <p>
	 * Copy the values of direction into this LightNode's local light direction.
	 * The final direction will be converted into world space by this
	 * LightNode's world transform.
	 * </p>
	 * <p>
	 * If direction is null, it is set to <0, 0, 1>
	 * </p>
	 * 
	 * @param direction The direction vector to be copied
	 */
	public void setLocalDirection(Vector3f direction) {
		if (direction == null)
			localDirection.set(0f, 0f, 1f);
		else
			localDirection.set(direction);
	}
	
	@Override
	public boolean update(float timeDelta) {
		boolean res = super.update(timeDelta);
		
		Vector3f worldDir = light.getDirection();
		worldTransform.getRotation().mul(localDirection, worldDir);
		return res;
	}
}
