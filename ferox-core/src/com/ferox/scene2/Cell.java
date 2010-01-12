package com.ferox.scene2;

import com.ferox.math.Frustum;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.util.Bag;
import com.ferox.util.entity.Entity;

/**
 * <p>
 * A Cell represents a reasonably static partitioning of the space within a
 * SceneController. Different Cells can be optimized for different types of scenes. For
 * example, the BoundedOctreeCell is useful in open/outdoor scenes and the
 * PortalCell is useful for classic FPS style play.
 * </p>
 * <p>
 * Cells are created, configured and then added to a SceneController where they
 * help partition the scene's elements into manageable areas to improve the
 * speed of both visibility and spatial culling.
 * </p>
 * <p>
 * You should not need to call the majority of the methods in Cell because they
 * are intended for use by a Cell's managing SceneController. The following
 * methods should not be called, except by a SceneController:
 * <ul>
 * <li>{@link #add(SceneElement)}</li>
 * <li>{@link #clear()}</li>
 * <li>{@link #query(BoundVolume, Class, Bag)}</li>
 * <li>{@link #query(Frustum, Class, Bag)}</li>
 * <li>{@link #remove(SceneElement)}</li>
 * <li>{@link #update(float)}</li>
 * </ul>
 * The remaining methods are free for normal use.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class Cell {
	private int lastPriority;
	private int priority;
	
	SceneController owner;
	
	/**
	 * Create a new Cell with a priority of 0.
	 */
	public Cell() {
		lastPriority = 0;
		priority = 0;
		
		owner = null;
	}

	/**
	 * <p>
	 * Return the priority of this Cell when determining the order in which
	 * SceneElements are tested against the Cells of a Scene. In the event that
	 * more than one Cell would return a non-null Object from
	 * {@link #add(SceneElement)} for the same element, the Cell with the higher
	 * priority will have the element added to it.
	 * </p>
	 * <p>
	 * By default this is set to 0.
	 * </p>
	 * 
	 * @return The current priority of the Cell
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * <p>
	 * Assign a priority to this Cell for the purposes of resolving ownership
	 * over SceneElements that could technically be in multiple Cells of a
	 * Scene. The Cell with a higher priority will get the element. Changes to a
	 * Cell's priority will be visible after the next processing of the
	 * SceneController.
	 * </p>
	 * <p>
	 * The priority is allowed to be any int value and the range should be
	 * chosen by the programmer based on the cells in use. Higher priorities
	 * should be reserved for cells that are more restrictive.
	 * </p>
	 * 
	 * @param priority The new priority for the cell
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * <p>
	 * Determine whether or not the given SceneElement should be assigned to
	 * this Cell, and potentially add it to the Cell. If the Cell can contain
	 * the element, then it should adjust its internal data structures, and
	 * finally return a non-null Object that will be associated with
	 * <tt>element</tt> and the Cell.
	 * </p>
	 * <p>
	 * If the element should not be added to this Cell, then the element should
	 * not be modified and null should be returned. It can be assumed that
	 * elements are tested against Cells in the correct order based on the cell
	 * priority.
	 * </p>
	 * <p>
	 * <tt>e</tt> is the Entity which is tied to the <tt>element</tt> Component.
	 * If the SceneElement is to be owned by this Cell, the Entity should be
	 * recorded as well so that spatial queries can return the correct entities
	 * quickly.
	 * </p>
	 * 
	 * @param e The Entity which has the given SceneElement component
	 * @param element The candidate element for addition
	 * @param cellData The previous cell data associated with this Cell and
	 *            element, or null if element was not owned by this Cell
	 * @return A non-null instance of cell data to use if this Cell can take the
	 *         element
	 */
	public abstract Object add(Entity e, SceneElement element, Object cellData);

	/**
	 * <p>
	 * Remove the given element from this Cell so that it will no longer be
	 * considered for any query results. This will only be called by the
	 * SceneController SceneElement should be removed from the Cell. This could
	 * be because another Cell is the new owner, or if the entity, <tt>e</tt> is
	 * no longer a SceneElement.
	 * </p>
	 * <p>
	 * Because the entity can be removed after it's no longer a SceneElement,
	 * <tt>element</tt> may be null. In this case, cellData is still accurate
	 * and should be sufficient enough to correctly remove the entity.
	 * </p>
	 * 
	 * @param e The Entity to be removed, and that is associated with element
	 * @param element The element to be removed, may be null if e is no longer a
	 *            SceneElement
	 * @param cellData The cell data last returned via
	 *            {@link #add(Entity, SceneElement, Object)} for this Entity
	 */
	public abstract void remove(Entity e, SceneElement element, Object cellData);

	/**
	 * Perform a visibility query as described in
	 * {@link SceneController#query(Frustum, Bag)}. The primary difference is
	 * that result can be assumed to be non-null and that all matching Entities
	 * in this Cell will be added to the bag without first clearing it. The Cell
	 * should only test SceneElements that have been assigned to it.
	 * 
	 * @param query The Frustum to test against
	 * @param result The Bag to add all matching Entities to
	 */
	public abstract void query(Frustum query, Bag<Entity> result);

	/**
	 * Perform a visibility query as described in
	 * {@link SceneController#query(BoundVolume, Bag)}. The primary difference
	 * is that result can be assumed to be non-null and that all matching
	 * Entities in this Cell will be added to the bag without first clearing it.
	 * The Cell should only test SceneElements that have been assigned to it.
	 * 
	 * @param query The BoundVolume to test against
	 * @param result The Bag to add all matching Entities to
	 */
	public abstract void query(BoundVolume query, Bag<Entity> result);

	/**
	 * Clear this Cell so that it is completely empty. All currently added
	 * SceneElements should be removed as if via remove(). The Cell's priority
	 * should be left unchanged.
	 */
	public abstract void clear();

	/**
	 * <p>
	 * Perform an update of this Cell. This occurs during the first steps of a
	 * Scene's update. It serves as a notification to the Cell and it allows the
	 * Cell to notify the Scene of any drastic changes to it that would
	 * invalidate all SceneElements. Normally Cells are considered fairly static
	 * in that if a SceneElement isn't dirty, it won't need to be reassigned to
	 * a cell.
	 * </p>
	 * <p>
	 * Return true if all of the Cell's current elements need to be reassigned.
	 * The current implementation returns true when there's a change in
	 * priority. There could be additional circumstances specific to a Cell
	 * implementation.
	 * </p>
	 * 
	 * @param timeDelta The time change since the last update, in seconds
	 * @return True if the Cell's contents need to be forcibly reassigned
	 */
	public boolean update(float timeDelta) {
		boolean update = lastPriority != priority;
		lastPriority = priority;
		return update;
	}
}
