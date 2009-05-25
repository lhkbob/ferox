package com.ferox.renderer;

import com.ferox.effect.EffectSet;
import com.ferox.math.Transform;
import com.ferox.resource.Geometry;

/**
 * <p>
 * A RenderAtom represents a visual object on the screen that can be rendered by
 * a Renderer implementation. This is subject to the renderer's support for the
 * geometry, effects, and resources referenced by the atom.
 * </p>
 * <p>
 * RenderAtom's have the ability to be locked by whoever constructs them, so
 * that it is possible to force them to be read-only (instances returned by
 * methods can still be modified, though). When constructed, a key can be
 * specified. If the key is not null, this instance must be passed in to each
 * setX() call or an exception will be thrown. If the specified key is null,
 * then anyone can set the instances of the render atom.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class RenderAtom {
	private Transform transform;
	private EffectSet effects;
	private Geometry geometry;

	private final Object key;

	/**
	 * Create a render atom with the given transform, geometry, and effect map that
	 * will be locked by the given key object. If key is null, this atom is not
	 * locked.
	 * 
	 * @param t The Transform to use
	 * @param g The Geometry that will be used by this atom
	 * @param e The EffectSet that is initially used
	 * @param key The key required for all setX() calls, if it's not null
	 * @throws NullPointerException if t or g are null
	 */
	public RenderAtom(Transform t, Geometry g, EffectSet e, Object key) {
		this.key = key;

		setTransform(t, key);
		setGeometry(g, key);
		setEffects(e, key);
	}

	/**
	 * Set the transform instance to use.
	 * 
	 * @param t The new transform instance to use
	 * @param key The key to allow the set to proceed
	 * @throws NullPointerException if t is null
	 * @throws IllegalArgumentException if key is incorrect
	 */
	public void setTransform(Transform t, Object key) {
		if (this.key == null || key == this.key) {
			if (t == null)
				throw new NullPointerException("Transform cannot be null");
			transform = t;
		} else
			throw new IllegalArgumentException(
					"Incorrect key specified, cannot set transform");
	}

	/**
	 * Set the EffectSet instance to use.
	 * 
	 * @param e The new effect map instance to use
	 * @param key The key to allow the set to proceed
	 * @throws IllegalArgumentException if key is incorrect
	 */
	public void setEffects(EffectSet e, Object key) {
		if (this.key == null || key == this.key)
			effects = e;
		else
			throw new IllegalArgumentException(
					"Incorrect key specified, cannot set effects");
	}

	/**
	 * Set the geometry instance to use.
	 * 
	 * @param g The new geometry instance to use
	 * @param key The key to allow the set to proceed
	 * @throws NullPointerException if g is null
	 * @throws IllegalArgumentException if key is incorrect
	 */
	public void setGeometry(Geometry g, Object key) {
		if (this.key == null || key == this.key) {
			if (g == null)
				throw new NullPointerException("Geometry cannot be null");
			geometry = g;
		} else
			throw new IllegalArgumentException(
					"Incorrect key specified, cannot set geometry");
	}

	/**
	 * Get the transform (world space) for this atom. This represents the 3D
	 * position of the atom when rendered.
	 * 
	 * @return The transform instance, will not be null
	 */
	public Transform getTransform() {
		return transform;
	}

	/**
	 * <p>
	 * Get the effects of the render atom. Essentially an atom is a vehicle for
	 * delivering the effects to the renderer, along with a transform for
	 * positioning the effects on a render surface.
	 * </p>
	 * <p>
	 * If this method returns null, the atom will be rendered with the default
	 * effects.
	 * </p>
	 * 
	 * @return The effect map to use, may be null
	 */
	public EffectSet getEffects() {
		return effects;
	}

	/**
	 * Get the Geometry that is rendered when the atom is submitted to the
	 * renderer.
	 * 
	 * @return The geometry to use, will not be null
	 */
	public Geometry getGeometry() {
		return geometry;
	}
}
