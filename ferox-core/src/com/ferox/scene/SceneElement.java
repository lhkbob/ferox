package com.ferox.scene;

import com.ferox.math.Matrix3f;
import com.ferox.math.Transform;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;

/**
 * <p>
 * SceneElement represents any finite element of a rendered scene. Generally a
 * SceneElement is not useful by itself but can be combined with a number of
 * other Components to represent anything from lights, to fog, to rendered
 * geometry. These other Component types then depend on an Entity also being a
 * SceneElement. It is recommended that each Entity use a unique SceneElement
 * instance, unlike appearance related Components which can possibly be shared
 * across many Entities.
 * </p>
 * <p>
 * SceneElements can be added to an EntitySystem which has a SceneController.
 * The SceneController will organize them in an efficient manor to support
 * visibility and spatial queries.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Description("3D element within a scene")
public final class SceneElement extends AbstractComponent<SceneElement> {
	private final Transform transform;
	
	private BoundVolume localBounds;
	private BoundVolume worldBounds;

	private boolean potentiallyVisible;
	
	public SceneElement() {
		super(SceneElement.class);
		transform = new Transform();
	}

	/**
	 * Return true if this SceneElement has been determined as potentially
	 * visible. This is generally determined by the Cells that own each
	 * SceneElement, and by the Entities within an EntitySystem that represent
	 * viewable regions.
	 * 
	 * @return Whether or not the SceneElement is potentially visible
	 */
	public boolean isPotentiallyVisible() {
		return potentiallyVisible;
	}

	/**
	 * Set whether or not this SceneElement is potentially visible. If a
	 * SceneElement is potentially visible it will generally be used by
	 * Controllers that can perform useful operations only on Entities that will
	 * affect the rendered scene.
	 * 
	 * @param pv Whether or not the SceneElement is potentially visible
	 */
	public void setPotentiallyVisible(boolean pv) {
		potentiallyVisible = pv;
	}

	/**
	 * Copy the given rotation matrix into this SceneElement's rotation matrix.
	 * The SceneElement will also be flagged as updated.
	 * 
	 * @param m The new rotation matrix
	 * @throws NullPointerException if m is null
	 */
	public void setRotation(Matrix3f m) {
		transform.setRotation(m);
	}

	/**
	 * @return The Transform that represents the 3D position and orientation of
	 *         this SceneElement.
	 */
	public Transform getTransform() {
		return transform;
	}

	/**
	 * Copy t into this SceneElement's Transform.
	 * 
	 * @param t The new Transform for this SceneElement
	 * @throws NullPointerException if t is null
	 */
	public void setTransform(Transform t) {
		transform.set(t);
	}

	/**
	 * <p>
	 * Set the local bounds that represent the extents of this SceneElement
	 * within its local coordinate space. If <tt>bounds</tt> is null, then this
	 * element has no bounds and the SceneController will assign a null world
	 * bounds. Otherwise the local bounds will be transformed into world space
	 * using {@link #getTransform()} to compute the world bounds.
	 * </p>
	 * <p>
	 * This instance is not copied, so any changes to it later will be reflected by the
	 * SceneElement.
	 * </p>
	 * 
	 * @param bounds The new local bounds
	 */
	public void setLocalBounds(BoundVolume bounds) {
		localBounds = bounds;
	}

	/**
	 * <p>
	 * Return the local BoundVolume instance used by this SceneElement. The
	 * local bound volume is in the local coordinate space of this SceneElement,
	 * and {@link #getTransform()} is used to compute the world bounds based
	 * using {@link BoundVolume#transform(Transform)}.
	 * </p>
	 * <p>
	 * The returned instance is not a defensive copy.
	 * </p>
	 * <p>
	 * This may return a null local bounds if the SceneElement is not bounded.
	 * In this case, the world bounds should also return null after an update.
	 * The returned instance must be different than the instance used to store
	 * world bounds.
	 * </p>
	 * 
	 * @return The local bounds
	 */
	public BoundVolume getLocalBounds() {
		return localBounds;
	}

	/**
	 * Set the BoundVolume instance that represents the computed world bounds,
	 * based off of {@link #getTransform()} and {@link #getLocalBounds()}. This
	 * should not be called directly, but is intended for use by a
	 * SceneController.
	 * 
	 * @param bounds The new BoundVolume instance stored for world bounds
	 */
	public void setWorldBounds(BoundVolume bounds) {
		worldBounds = bounds;
	}

	/**
	 * Get the world bounds that was last assigned via
	 * {@link #setWorldBounds(BoundVolume)}. The SceneController computes the
	 * world bounds based off of the assigned local bounds and the current
	 * transform. If the local bounds are null, the world bounds will also be
	 * null.
	 * 
	 * @return The world bounds of this SceneElement
	 */
	public BoundVolume getWorldBounds() {
		return worldBounds;
	}
}
