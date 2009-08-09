package com.ferox.scene;

import com.ferox.math.ReadOnly;
import com.ferox.math.Transform;
import com.ferox.math.bounds.BoundVolume;

/**
 * <p>
 * SceneElement represents any finite element of a rendered scene. Generally
 * subclasses are fairly logical in nature, such as lights, shapes and cameras.
 * </p>
 * <p>
 * SceneElements can be added to a Scene and it will organize them in an
 * efficient manor to support visibility and spatial queries. This can then be
 * used by the various RenderPass implementations to render the actual scene,
 * possibly even with fancy special effects.
 * </p>
 * 
 * @author Michael Ludwig
 */
// FIXME: should we allow Cells to set CellData onto an Element?
// this could be very useful for Portals and Octrees because then they can keep
// track of where each element was (if contained)
public interface SceneElement {
	/**
	 * Return the Scene that this SceneElement was last added to. This instance
	 * must match the instance specified in the last call to setScene(). If null
	 * is returned, then this element is not part of a Scene.
	 * 
	 * @return The Scene that owns this element
	 */
	public Scene getScene();
	
	/**
	 * Set the Scene that owns this SceneElement. This should not be called
	 * except by a Scene in response to an add() or remove() call.
	 * 
	 * @param scene The Scene that owns this element
	 */
	public void setScene(Scene scene);
	
	/**
	 * Return the current Cell that owns this SceneElement. This will only be
	 * up-to-date after an update of the Scene. This may return null if the
	 * element has yet to be placed in a Cell, or if it's old Cell was removed
	 * from the Scene before the next update. This instance must match the
	 * instance specified in the last call to setCell().
	 * 
	 * @return The Cell that owns this element
	 */
	public Cell getCell();
	
	/**
	 * Set the Cell that owns this SceneElement. This should not be called
	 * except by a Cell in response to an add(), remove() or clear() call.
	 * 
	 * @param cell The Cell that owns this element
	 */
	public void setCell(Cell cell);

	/**
	 * <p>
	 * Return the local transform instance used by this SceneElement. In many
	 * cases a local transform can be used to adjust the final world transform,
	 * based on custom UpdateControllers of a Scene. In the event that there are
	 * no Controllers that treat the local transform uniquely, the local
	 * transform will be identical to the world transform.
	 * </p>
	 * <p>
	 * The returned instance need not be a defensive copy and it should be
	 * considered as read-only. However, if it is modified, use flagDirty() to
	 * ensure that the element's update() has the correct behavior.
	 * </p>
	 * <p>
	 * Implementations must never return a null local transform. The returned
	 * instance must be different than the instance used to store world
	 * transforms.
	 * </p>
	 * 
	 * @return The local transform
	 */
	@ReadOnly
	public Transform getLocalTransform();
	
	/**
	 * <p>
	 * Return the local BoundVolume instance used by this SceneElement. The
	 * local bound volume is in the coordinate space described by the local
	 * transform, and the final world transform is used to compute the world
	 * bounds based using BoundVolume.transform().
	 * </p>
	 * <p>
	 * The returned instance need not be a defensive copy and it should be
	 * considered as read-only. However, if it is modified, use flagDirty() to
	 * ensure that the element's update() has the correct behavior.
	 * </p>
	 * <p>
	 * Implementations may return a null local bounds if the scene element is
	 * not bounded. In this case, the world bounds should also return null after
	 * an update.
	 * </p>
	 * <p>
	 * The returned instance must be different than the instance used to store
	 * world bounds.
	 * </p>
	 * 
	 * @return The local bounds
	 */
	@ReadOnly
	public BoundVolume getLocalBounds();

	/**
	 * <p>
	 * Return the world transform instance used by this SceneElement. The world
	 * transform represents the final position and rotation of the element in
	 * the same coordinate space of the View used to render a scene. All
	 * elements within the same scene have the same world transform space, but
	 * can have different local transform spaces depending on the modifying
	 * controllers.
	 * </p>
	 * <p>
	 * The returned instance need not be a defensive copy and it should be
	 * considered as read-only. Programmers should not modify the returned
	 * instance because any changes will be overwritten during update() based on
	 * the local transform. Given that, it is possible for the world transform
	 * to be stale if the local transform has been modified without an update.
	 * </p>
	 * <p>
	 * Implementations must never return a null world transform. The returned
	 * instance must be different than the instance used to store local
	 * transforms.
	 * </p>
	 * 
	 * @return The world transform
	 */
	@ReadOnly
	public Transform getWorldTransform();

	/**
	 * <p>
	 * Return the world bounds of this element. This is the local bounds
	 * translated from the element's identity coordinate space into the world
	 * space. If the element's local bounds are null, then this method should
	 * also return null.
	 * </p>
	 * <p>
	 * The returned instance need not be a defensive copy and it should be
	 * considered as read-only. Programmers should not modify the returned
	 * instance because any changes will be overwritten during update(). Given
	 * that, it is possible for the world bounds to be stale if the local
	 * transform or bounds have been modified without an update.
	 * </p>
	 * <p>
	 * The returned instance must be different than the instance used to store
	 * local bounds.
	 * </p>
	 * 
	 * @return The world bounds
	 */
	@ReadOnly
	public BoundVolume getWorldBounds();
	
	/**
	 * Notify the SceneElement that its local transform or bound state have been
	 * modified externally and that it should return true from its next
	 * update(). This will signal the Scene that it should be re-evaluated and
	 * placed into a correct Cell.
	 */
	public void setDirty();
	
	/**
	 * Return true if this SceneElement's local transform or bound state has
	 * been modified. Depending on if there's an assigned UpdateController (and
	 * how its implemented) this will generally match the return value from the
	 * next update().
	 * 
	 * @return This SceneElement's dirty status
	 */
	public boolean isDirty();
	
	/**
	 * <p>
	 * Update this SceneElement's world transform and bounds based off its local
	 * parameters. If the world transform or bounds have changed, then this
	 * method should return true so that the Scene can properly reassign its
	 * Cell.
	 * </p>
	 * <p>
	 * If there is an UpdateController, then the controller should be invoked
	 * and is responsible for modifying the world transform. If there is no
	 * controller, then the local transform should be copied into the world
	 * transform.
	 * </p>
	 * <p>
	 * The world bounds should be derived from the local bounds using
	 * BoundVolume.transform() using the computed world transform. If the local
	 * bounds are null, then the world bounds should also be null. The bounds
	 * should be computed by the SceneElement regardless of the presences of an
	 * UpdateController.
	 * </p>
	 * 
	 * @param timeDelta The time change since the last update, in seconds
	 * @return True if this SceneElement needs to be reassigned a cell
	 */
	public boolean update(float timeDelta);
	
	/**
	 * <p>
	 * Set the current UpdateController to override the default behavior of the
	 * update method. If this controller is non-null, it becomes responsible for
	 * properly updating the world transform based on the element's local
	 * parameters and whatever else may be necessary.
	 * </p>
	 * <p>
	 * If controller is null, then any previous controller is cleared and update
	 * behavior returns to normal.
	 * </p>
	 * 
	 * @see #update(float)
	 * @param controller The UpdateController to use for updates
	 */
	public void setController(UpdateController controller);
	
	/**
	 * Return the currently bound UpdateController, or null if this SceneElement
	 * is updated using the normal behavior of update(float).
	 * 
	 * @return The current UpdateController
	 */
	public UpdateController getController();
}
