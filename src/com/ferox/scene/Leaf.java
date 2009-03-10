package com.ferox.scene;

import com.ferox.math.BoundVolume;

/** Represents a leaf node in the scene structure.  Possible examples of
 * leaves include shapes (e.g. a visible object in the scene) or lights and 
 * fogs (e.g. nodes that influence others based on proximity).
 * 
 * @author Michael Ludwig
 *
 */
public abstract class Leaf extends Node {
	private BoundVolume localBounds;
	
	/** Overrides updateBounds() to convert this leaf's local bounds into
	 * world space.  Provides a hook via adjustLocalBounds() for subclasses to 
	 * alter the bounds before conversion. */
	@Override
	public void updateBounds() {
		this.localBounds = this.adjustLocalBounds(this.localBounds);
		if (this.localBounds != null) 
			this.worldBounds = this.localBounds.applyTransform(this.worldTransform, this.worldBounds);
		else
			this.worldBounds = null;
	}

	/** Set the local bounds of this leaf.  If null, the leaf's world bounds will be set to
	 * null after the next update.  A null bounds will cause the leaf to always 'intersect' with a frustum. */
	public void setLocalBounds(BoundVolume v) {
		this.localBounds = v;
	}
	
	/** Get the local bounds of this leaf node. */
	public BoundVolume getLocalBounds() {
		return this.localBounds;
	}

	/** Called by updateBounds() before transforming the leaf's bounds into world space. 
	 * Return the local bounds to use, which will become the BoundVolume returned by calls to getLocalBounds(). */
	protected abstract BoundVolume adjustLocalBounds(BoundVolume local);
}
