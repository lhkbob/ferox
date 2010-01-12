package com.ferox.scene2;

import com.ferox.entity.Component;
import com.ferox.math.Matrix3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.util.ReadOnly;

/**
 * <p>
 * SceneElement represents any finite element of a rendered scene. Generally
 * subclasses are fairly logical in nature, such as lights, shapes and cameras.
 * </p>
 * <p>
 * SceneElements can be added to an EntitySystem which has a SceneController.
 * The SceneController will organize them in an efficient manor to support
 * visibility and spatial queries.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class SceneElement extends Component {
	/**
	 * Axis represents each of the three local basis vectors of a SceneElement.
	 * Axis is used when constraining one of the local axis to a vector in world
	 * space. It is also used when determining which axis constitutes the
	 * direction axis for billboarding points.
	 */
	public static enum Axis {
		X, Y, Z
	}
	
	private static final String DESCR = "3D element within a scene";
	
	private final Transform transform;
	private BoundVolume localBounds;
	private BoundVolume worldBounds;
	
	private SceneElementUpdater updater;
	private boolean updated;
	
	private boolean potentiallyVisible;
	
	private Vector3f pointTowards;
	private Axis pointAxis;
	
	private Vector3f constrainVector;
	private Axis constrainAxis;
	
	private Cell cellOwner;
	
	public SceneElement() {
		super(DESCR);
		transform = new Transform();
		updated = true;
	}

	/**
	 * Return the current Cell that owns this SceneElement. This will only be
	 * up-to-date after the SceneController has run. This may return null if the
	 * element has yet to be placed in a Cell, or if it's old Cell was removed
	 * from the SceneController before the next update.
	 * 
	 * @return The Cell that owns this element
	 */
	public Cell getCell() {
		return cellOwner;
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
	 * Set the Cell that owns this SceneElement. This should not be called
	 * except by a Cell in response to an add(), remove() or clear() call.
	 * 
	 * @param cell The Cell that owns this element
	 */
	public void setCell(Cell cell) {
		cellOwner = cell;
	}

	/**
	 * Get the point that this SceneElement will point towards. If the returned
	 * Vector3f is null, then this SceneElement will use the rotation matrix
	 * that was last set by its SceneElementUpdater (or manually). When the
	 * vector is non-null, the SceneController will adjust the rotation matrix
	 * so that the local axis specified by {@link #getBillboardDirectionAxis()}
	 * points towards this vector. After this, matrix will be updated so that
	 * the constraint axis is met.
	 * 
	 * @return The point that this SceneElement will point towards, as best its
	 *         able
	 */
	public Vector3f getBillboardPoint() {
		return pointTowards;
	}

	/**
	 * Get the Axis that determines which of the local axis represents the
	 * 'direction' that will line up towards the point returned by
	 * {@link #getBillboardPoint()}. This will return null if
	 * {@link #getBillboardPoint()} is null.
	 * 
	 * @return The direction Axis
	 */
	public Axis getBillboardDirectionAxis() {
		return pointAxis;
	}

	/**
	 * Set the billboard point and direction vector. If <tt>pointTowards</tt> is
	 * non-null, the SceneElement will have its local <tt>axis</tt> directed
	 * towards the point after each update by the SceneController. If the
	 * {@link #getConstraintVector()} is non-null, then the rotation matrix will
	 * be constrained to that vector after it's been set to point towards
	 * <tt>pointTowards</tt>
	 * 
	 * @param pointTowards The point that this SceneElement will be directed to
	 * @param axis The direction axis
	 * @throws NullPointerException if pointTowards is not null, but axis is
	 *             null
	 */
	public void setBillboardPoint(Vector3f pointTowards, Axis axis) {
		if (pointTowards != null && axis == null)
			throw new NullPointerException("Axis must be non-null when pointTowards is not null");
		this.pointTowards = pointTowards;
		pointAxis = (pointTowards == null ? null : axis);
	}

	/**
	 * Get the Axis that determines which of the three local axis are
	 * constrained to match {@link #getConstraintVector()}. This will be null if
	 * the constraint vector is null.
	 * 
	 * @return The constrained axis
	 */
	public Axis getConstraintAxis() {
		return constrainAxis;
	}

	/**
	 * Get the vector that one of the local axis of this SceneElement will be
	 * constrained to. This can be used to force elements to remain vertical,
	 * and to limit the degrees of freedom used when billboarding points. If the
	 * vector is null, then no axis are constrained.
	 * 
	 * @return The vector that {@link #getConstraintAxis()} will be set to
	 */
	public Vector3f getConstraintVector() {
		return constrainVector;
	}

	/**
	 * Set the vector and axis that will be constrained after the SceneElement
	 * has been updated, and then billboarded. If the vector is null, then no
	 * axis will be constrained.
	 * 
	 * @param vector The vector that the given axis will be set to
	 * @param axis The axis that will be constrained
	 */
	public void setConstraintAxis(Vector3f vector, Axis axis) {
		if (vector != null && axis == null)
			throw new NullPointerException("Axis must be non-null when vector is not null");
		constrainVector = vector;
		constrainAxis = (vector == null ? null : axis);
	}

	/**
	 * Return the SceneElementUpdater that should be run each time that this
	 * SceneElement is updated. If this is non-null, this element's transform
	 * will be static or updated manually.
	 * 
	 * @return The SceneElementUpdater that updates this SceneElement
	 */
	public SceneElementUpdater getUpdater() {
		return updater;
	}

	/**
	 * <p>
	 * Set the current SceneElementUpdater to automatically update the element's
	 * transform. If controller is null, then any previous updater is cleared
	 * and the transform will only be updated manually.
	 * </p>
	 * 
	 * @see #update(float)
	 * @param updater The SceneElementUpdater to use for updates
	 */
	public void setUpdater(SceneElementUpdater updater) {
		this.updater = updater;
	}

	/**
	 * @return The translation vector of {@link #getTransform()}. This vector
	 *         should be considered read-only. If it's modified, then
	 *         setUpdated(true) should be invoked.
	 */
	@ReadOnly
	public Vector3f getTranslation() {
		return transform.getTranslation();
	}

	/**
	 * Set the translation vector of {@link #getTransform()} to the vector
	 * represented by <x, y, z>. The SceneElement will also be flagged as
	 * updated.
	 * 
	 * @param x The new x translation coordinate
	 * @param y The new y translation coordinate
	 * @param z The new z translation coordinate
	 */
	public void setTranslation(float x, float y, float z) {
		transform.setTranslation(x, y, z);
		setUpdated(true);
	}

	/**
	 * Copy the given vector into this SceneElement's translation. The
	 * SceneElement will also be flagged as updated.
	 * 
	 * @param t The new translation
	 * @throws NullPointerException if t is null
	 */
	public void setTranslation(Vector3f t) {
		transform.setTranslation(t);
		setUpdated(true);
	}

	/**
	 * @return The rotation matrix of {@link #getTransform()}. This matrix
	 *         should be considered read-only. If it's modified, then
	 *         setUpdated(true) should be invoked.
	 */
	@ReadOnly
	public Matrix3f getRotation() {
		return transform.getRotation();
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
		setUpdated(true);
	}

	/**
	 * @return The Transform that represents the 3D position and orientation of
	 *         this SceneElement. This should be considered read-only. If it is
	 *         updated, then setUpdated(true) must be invoked.
	 */
	@ReadOnly
	public Transform getTransform() {
		return transform;
	}

	/**
	 * Copy t into this SceneElement's Transform. This SceneElement will also be
	 * flagged as having been updated.
	 * 
	 * @param t The new Transform for this SceneElement
	 * @throws NullPointerException if t is null
	 */
	public void setTransform(Transform t) {
		transform.set(t);
		setUpdated(true);
	}

	/**
	 * <p>
	 * Set the local bounds that represent the extents of this SceneElement
	 * within its local coordinate space. If <tt>bounds</tt> is null, then this
	 * element has no bounds and the SceneController should assign a null world
	 * bounds. Otherwise the local bounds will be transformed into world space
	 * using {@link #getTransform()} to compute the world bounds.
	 * </p>
	 * <p>
	 * This instance is not copied, so any changes will be reflected by the
	 * SceneElement. When changed, the element's setUpdated(true) method should
	 * be called.
	 * </p>
	 * 
	 * @param bounds The new local bounds
	 */
	public void setLocalBounds(BoundVolume bounds) {
		localBounds = bounds;
		setUpdated(true);
	}

	/**
	 * <p>
	 * Return the local BoundVolume instance used by this SceneElement. The
	 * local bound volume is in the local coordinate space of this SceneElement,
	 * and {@link #getTransform()} is used to compute the world bounds based
	 * using {@link BoundVolume#transform(Transform)}.
	 * </p>
	 * <p>
	 * The returned instance is not a defensive copy and it should be considered
	 * as read-only. However, if it is modified, use setUpdated() to ensure that
	 * the element will be updated properly.
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
	@ReadOnly
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

	/**
	 * @return Return true if this SceneElement has been updated since the last
	 *         processing of a SceneController. This should be true if the local
	 *         bounds or transform has been modified.
	 */
	public boolean isUpdated() {
		return updated;
	}

	/**
	 * Set whether or not this SceneElement has been modified since the last
	 * time the SceneController has processed every SceneElement.
	 * 
	 * @param u True if this SceneElement has been modified
	 */
	public void setUpdated(boolean u) {
		updated = u;
	}
}
