package com.ferox.scene;

import com.ferox.math.bounds.BoundVolume;

/**
 * Represents a leaf node in the scene structure. Possible examples of leaves
 * include shapes (e.g. a visible object in the scene) or lights and fogs (e.g.
 * nodes that influence others based on proximity).
 * 
 * @author Michael Ludwig
 */
public abstract class Leaf extends Node {
	private BoundVolume localBounds;

	/**
	 * Overrides updateBounds() to convert this leaf's local bounds into world
	 * space. Provides a hook via adjustLocalBounds() for subclasses to alter
	 * the bounds before conversion.
	 */
	@Override
	public void updateBounds() {
		localBounds = adjustLocalBounds(localBounds);
		if (localBounds != null)
			worldBounds = localBounds.transform(worldTransform, worldBounds);
		else
			worldBounds = null;
	}

	/**
	 * Set the local bounds of this leaf. If null, the leaf's world bounds will
	 * be set to null after the next update. A null bounds will cause the leaf
	 * to always 'intersect' with a frustum.
	 * 
	 * @param v The new local bounds instance
	 */
	public void setLocalBounds(BoundVolume v) {
		localBounds = v;
	}

	/**
	 * @return The local bounds of this leaf node.
	 */
	public BoundVolume getLocalBounds() {
		return localBounds;
	}

	/**
	 * <p>
	 * Called by updateBounds() before transforming the leaf's bounds into world
	 * space. Return the local bounds to use, which will become the BoundVolume
	 * returned by subsequent calls to getLocalBounds(); the returned instance
	 * is recommended to be local if the transformations can be done in place.
	 * </p>
	 * <p>
	 * Default implementation just returns local.
	 * </p>
	 * 
	 * @param local The current local bounds instance
	 * @return The local bounds instance to use for this Leaf
	 */
	protected BoundVolume adjustLocalBounds(BoundVolume local) {
		return local;
	}
}
