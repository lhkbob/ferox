package com.ferox.scene2;

import com.ferox.math.Frustum;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.util.Bag;

/**
 * <p>
 * A Cell represents a reasonably static partitioning of the space within a
 * Scene. Different Cells can be optimized for different types of Scenes. For
 * example, the BoundedOctreeCell is useful in open/outdoor scenes and the
 * PortalCell is useful for classic FPS style play.
 * </p>
 * <p>
 * Cells are created, configured and then added to a Scene where they help
 * partition the Scene's elements into manageable areas to improve the speed of
 * both visibility and spatial culling.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Cell {
	/**
	 * Return the Scene that this Cell was last added to. This instance must
	 * match the instance specified in the last call to setScene(). If null is
	 * returned, then this cell is not part of a Scene.
	 * 
	 * @return The Scene that owns this cell
	 */
	public Scene getScene();
	
	/**
	 * Set the Scene that owns this Cell. This should not be called except by a
	 * Scene in response to an add() or remove() call.
	 * 
	 * @param scene The Scene that owns this cell
	 */
	public void setScene(Scene scene);
	
	/**
	 * <p>
	 * Return the priority of this Cell when determining the order in which
	 * SceneElements are tested against the Cells of a Scene. In the event that
	 * more than one Cell would return true from an add(element) for the same
	 * element, the Cell with the higher priority will have the element added to
	 * it.
	 * </p>
	 * <p>
	 * By default this should be set to 0.
	 * </p>
	 * 
	 * @return The current priority of the Cell
	 */
	public int getPriority();
	
	/**
	 * <p>
	 * Assign a priority to this Cell for the purposes of resolving ownership
	 * over SceneElements that could technically be in multiple Cells of a
	 * Scene. The Cell with a higher priority will get the element. Changes to a
	 * Cell's priority will be visible after the next update to its Scene.
	 * </p>
	 * <p>
	 * The priority is allowed to be any int value and the range should be
	 * chosen by the programmer based on the cells in use. Higher priorities
	 * should be reserved for cells that are more restrictive.
	 * </p>
	 * 
	 * @param priority The new priority for the cell
	 */
	public void setPriority(int priority);
	
	/**
	 * <p>
	 * Determine whether or not the given SceneElement should be assigned to
	 * this Cell, and potentially add it to the Cell. If the Cell can contain
	 * the element, then it should remove the element from any previous owning
	 * Cell, update the element's Cell reference, adjust its internal data
	 * structures, and finally return true.
	 * </p>
	 * <p>
	 * If the element should not be added to this Cell, then the element should
	 * not be modified and false should be returned. It can be assumed that
	 * elements are tested against Cells in the correct order based on the cell
	 * priority.
	 * </p>
	 * 
	 * @param element The candidate element for addition
	 * @return True if the element was successfully assigned to this Cell
	 */
	public boolean add(SceneElement element);
	
	/**
	 * Remove the given element from this Cell so that it will no longer be
	 * considered for any query results. After a successful remove(), the
	 * element's cell reference should be assigned to null. If element is not
	 * presently owned by this Cell, then remove() should be a no-op.
	 * 
	 * @param element The element to be removed.
	 */
	public void remove(SceneElement element);
	
	/**
	 * <p>
	 * Add the given SceneElement type as an index to enable more efficient
	 * queries. An index does not have to be explicitly added to be used in a
	 * query, but in scenarios where the SceneElement type is uncommon compared
	 * to most of a Scene's elements, indexing can greatly improve performance.
	 * </p>
	 * <p>
	 * If index is null, equal to SceneElement, or already added to this Cell,
	 * then this method is a no-op. It does not make sense to add SceneElement
	 * as an index, because it would index all elements, anyway.
	 * </p>
	 * 
	 * @param index The class type to use as an index
	 */
	public void addIndex(Class<? extends SceneElement> index);
	
	/**
	 * <p>
	 * Remove the given index from this Cell. After a call to this method,
	 * this Cell will no longer explicitly index SceneElements based off of the
	 * given index. Depending on the size of this Scene and the types of
	 * queries, this may save memory or make queries slower or both.
	 * </p>
	 * <p>
	 * If index is null, equal to SceneElement, or not already an index for this
	 * Cell, then this method is a no-op and false is returned.
	 * </p>
	 * 
	 * @param index The class type to no longer use as an index
	 * @return True if the index was successfully removed.
	 */
	public void removeIndex(Class<? extends SceneElement> index);
	
	/**
	 * Perform a visibility query as described in Scene.query(). The primary
	 * difference is that result can be assumed to be non-null and that all
	 * matching SceneElements in this Cell will be added to the bag without
	 * first clearing it. The Cell should only test SceneElements that have been
	 * assigned to it.
	 * 
	 * @see Scene.#query(Frustum, Class, Bag)
	 * @param query The Frustum to test against
	 * @param index The SceneElement type index
	 * @param result The Bag to add all matching SceneElements to
	 */
	public void query(Frustum query, Class<? extends SceneElement> index, Bag<SceneElement> result);
	
	/**
	 * Perform a spatial query as described in Scene.query(). The primary
	 * difference is that result can be assumed to be non-null and that all
	 * matching SceneElements in this Cell will be added to the bag without
	 * first clearing it. The Cell should only test SceneElements that have been
	 * assigned to it.
	 * 
	 * @see Scene.#query(BoundVolume, Class, Bag)
	 * @param query The BoundVolume to test against
	 * @param index The SceneElement type index
	 * @param result The Bag to add all matching SceneElements to
	 */
	public void query(BoundVolume query, Class<? extends SceneElement> index, Bag<SceneElement> result);
	
	/**
	 * Clear this Cell so that it is completely reverted to its initial state.
	 * All currently added SceneElements should be removed as per remove(), and
	 * all indices should be removed as per removeIndex(). The Cell's priority
	 * and Scene should be left unchanged.
	 */
	public void clear();
	
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
	 * The most common cause of this would be a change in priority. There could
	 * also be other circumstances specific to a Cell implementation.
	 * </p>
	 * 
	 * @param timeDelta The time change since the last update, in seconds
	 * @return True if the Cell's contents need to be forcibly reassigned
	 */
	public boolean update(float timeDelta);
}
