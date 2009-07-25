package com.ferox.scene2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ferox.math.Frustum;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.util.Bag;

/**
 * <p>
 * A Scene represents a collection of SceneElements--leaf objects representing
 * direct participants in rendering (lights, shapes, cameras, etc.)--and Cells,
 * which are used to automatically partition the SceneElements into relevant
 * schemes for efficient queries.
 * </p>
 * <p>
 * The true complexities of a query are implemented within a Cell, but the Scene
 * provides a common entry point for a grouping of Cells and handles correctly
 * updating everything.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Scene {
	private static final Comparator<Cell> prioritySorter = new Comparator<Cell>() {
		@Override
		public int compare(Cell o1, Cell o2) {
			return o1.getPriority() - o2.getPriority();
		}
	};
	
	private final Bag<SceneElement> sceneElements;
	// cells should also be a Bag, but if it's a List we can use Collections for sorting
	// and its size should be much smaller so we don't need Bag's efficient removal
	private final List<Cell> cells;
	
	private final Set<Class<? extends SceneElement>> indices;
	
	private long lastUpdate; // ns
	private boolean cellAdded;
	
	/**
	 * Create a new Scene that's completely empty.
	 * Use add(Cell) and add(SceneElement) to place content
	 * within the Scene.
	 */
	public Scene() {
		sceneElements = new Bag<SceneElement>();
		cells = new ArrayList<Cell>();
		indices = new HashSet<Class<? extends SceneElement>>();
		
		// set up the default cell at the lowest possible priority
		UnboundedCell dfltCell = new UnboundedCell();
		dfltCell.setPriority(Integer.MIN_VALUE);
		add(dfltCell);
	}
	
	/**
	 * <p>
	 * Add the given SceneElement to this Scene, so that it may be part of the
	 * rendered scene. element will be automatically updated by this Scene and
	 * will be placed within the correct Cell each frame.
	 * </p>
	 * <p>
	 * This method does nothing if element is null or if element.getScene()
	 * returns this Scene. If element.getScene() returns a different Scene instance,
	 * it is first removed from that Scene and then added to this one.
	 * </p>
	 * 
	 * @param element The SceneElement to add to this Scene
	 */
	public void add(SceneElement element) {
		if (element != null && element.getScene() != this) {
			if (element.getScene() != null)
				element.getScene().remove(element);
			
			sceneElements.add(element);
			element.setScene(this);
		}
	}
	
	/**
	 * <p>
	 * Remove the given SceneElement from this Scene so that it is no longer
	 * rendered. If element is null or not owned by this Scene, then this method
	 * is a no-op.
	 * </p>
	 * 
	 * @param element The SceneElement to remove
	 * @return True if element was successfully removed from this Scene
	 */
	public boolean remove(SceneElement element) {
		if (element != null && element.getScene() == this) {
			sceneElements.remove(element);
			if (element.getCell() != null)
				element.getCell().remove(element);
			
			element.setScene(null);
			return true;
		}
		
		return false;
	}
	
	/**
	 * <p>
	 * Add the given Cell to this Scene, so that it can help efficiently 
	 * organize the Scene's SceneElements.  cell will be automatically updated
	 * each frame to include the correct SceneElements, based on its priority
	 * and the other Cell's added to this Scene.
	 * </p>
	 * <p>
	 * This method does nothing if cell is null or if cell.getScene()
	 * returns this Scene. If cell.getScene() returns a different Scene instance,
	 * it is first removed from that Scene and then added to this one.
	 * </p>
	 * 
	 * @param cell The Cell to add to this Scene
	 */
	public void add(Cell cell) {
		if (cell != null && cell.getScene() != this) {
			if (cell.getScene() != null)
				cell.getScene().remove(cell);
			
			cells.add(cell);
			cell.setScene(this);
			
			// add all indices configured for this Scene
			for (Class<? extends SceneElement> index: indices)
				cell.addIndex(index);
			
			cellAdded = true;
		}
	}

	/**
	 * <p>
	 * Remove the given Cell from this Scene so that it no longer partitions
	 * SceneElements. The SceneElements currently contained within the Cell will
	 * be removed from it, and all indices will be cleared. The SceneElements
	 * are still owned by this Scene and will be placed in another Cell at the
	 * next update.
	 * </p>
	 * <p>
	 * If cell is null or not owned by this Scene, then this method is a no-op.
	 * </p>
	 * 
	 * @param cell The Cell to remove
	 * @return True if cell was successfully removed from this Scene
	 */
	public boolean remove(Cell cell) {
		if (cell != null && cell.getScene() == this) {
			cells.remove(cell);
			cell.clear();
			
			cell.setScene(null);
			return true;
		}
		
		return false;
	}
	
	/**
	 * <p>
	 * Update the scene, so that all currently added SceneElements are
	 * up-to-date and are in the correct cells. If the Scene (or any of its
	 * related components, Cells, or SceneElements) is not modified, then it's
	 * guaranteed that all queries will be accurate and each SceneElement's
	 * world state and cell association will be valid.
	 * </p>
	 * <p>
	 * timeDelta is present as a parameter to allow for artificially timed
	 * updates. If this is unnecessary, the update() method should be used that
	 * takes no parameters.
	 * </p>
	 * 
	 * @param timeDelta The timeDelta since the last update, in seconds
	 * @throws IllegalArgumentException if timeDelta is negative
	 */
	public void update(float timeDelta) {
		if (timeDelta < 0f) 
			throw new IllegalArgumentException("Cannot have a negative time delta: " + timeDelta);
		
		lastUpdate = System.nanoTime();
		
		Collections.sort(cells, prioritySorter);
		
		boolean forceAllUpdate = cellAdded;
		int cellCount = cells.size();
		for (int i = 0; i < cellCount; i++) {
			forceAllUpdate |= cells.get(i).update(timeDelta);
		}
		
		SceneElement element;
		int elementCount = sceneElements.size();
		for (int i = 0; i < elementCount; i++) {
			element = sceneElements.get(i);
			if (element.update(timeDelta) || element.getCell() == null || forceAllUpdate) {
				// must determine the element's cell
				placeElement(element);
			}
		}
		
		cellAdded = false;
	}
	
	/**
	 * Update the scene, using the time delta from the last call
	 * to update() or update(float).
	 */
	public void update() {
		long delta = System.nanoTime() - lastUpdate;
		update(delta / 1e9f);
	}
	
	/**
	 * <p>
	 * Add the given SceneElement type as an index to enable more efficient
	 * queries. An index does not have to be explicitly added to be used in a
	 * query, but in scenarios where the SceneElement type is uncommon compared
	 * to most of a Scene's elements, indexing can greatly improve performance.
	 * </p>
	 * <p>
	 * If index is null, equal to SceneElement, or already added to this Scene,
	 * then this method is a no-op. It does not make sense to add SceneElement
	 * as an index, because it would index all elements, anyway.
	 * </p>
	 * 
	 * @param index The class type to use as an index
	 */
	public void addIndex(Class<? extends SceneElement> index) {
		if (index != null && !SceneElement.class.equals(index) && !indices.contains(index)) {
			indices.add(index);
			
			int length = cells.size();
			for (int i = 0; i < length; i++)
				cells.get(i).addIndex(index);
		}
	}
	
	/**
	 * <p>
	 * Remove the given index from this Scene. After a call to this method,
	 * Cell's will no longer explicitly index SceneElements based off of the
	 * given index. Depending on the size of this Scene and the types of
	 * queries, this may save memory or make queries slower or both.
	 * </p>
	 * <p>
	 * If index is null, equal to SceneElement, or not already an index for this
	 * Scene, then this method is a no-op and false is returned.
	 * </p>
	 * 
	 * @param index The class type to no longer use as an index
	 * @return True if the index was successfully removed.
	 */
	public boolean removeIndex(Class<? extends SceneElement> index) {
		if (index != null && !SceneElement.class.equals(index) && indices.contains(index)) {
			indices.remove(index);
			
			int length = cells.size();
			for (int i = 0; i < length; i++) 
				cells.get(i).removeIndex(index);
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * <p>
	 * Perform a query against all SceneElements and store the
	 * results into result. A SceneElement will be stored in results based on
	 * the following criteria:
	 * <ul>
	 * <li>If the element's world bounds are null, or intersect or lie within the
	 * view frustum.</li>
	 * <li>It's an instance of the given index type, where 'instance of' follows
	 * the definition of the instanceof keyword.</li>
	 * </ul>
	 * If index is null or equal to SceneElement, then the last point always
	 * passes.
	 * </p>
	 * <p>
	 * If results is null, a new Bag<SceneElement is instantiated and used.
	 * Before beginning the query, results will be cleared using a fast clear.
	 * A fast clear is used so that re-using Bags is as efficient as possible.
	 * When the results are returned, they will be contained in 0 to size() -1.
	 * </p>
	 * <p>
	 * This method should only be called after an update. If SceneElements or
	 * Cells are modified without a subsequent update, the Scene's internal
	 * state may be inconsistent. This will not cause the query to fail, but it
	 * may return invalid or incomplete results.
	 * </p>
	 * 
	 * @param frustum The Frustum to test visibility against
	 * @param index The required parent class for all returned SceneElements
	 * @param results The results storage for the query, may be null
	 * @return results, or a new SceneQueryResult if null
	 * @throws NullPointerException if frustum is null
	 */
	public Bag<SceneElement> query(Frustum frustum, Class<? extends SceneElement> index, 
								  Bag<SceneElement> results) {
		if (frustum == null)
			throw new NullPointerException("Cannot query against a null Frustum");
		
		if (results == null)
			results = new Bag<SceneElement>();
		results.clear(false);
		
		int length = cells.size();
		for (int i = 0; i < length; i++) {
			cells.get(i).query(frustum, index, results);
		}
		
		return results;
	}
	
	/**
	 * <p>
	 * Perform a query against all SceneElements and store the
	 * results into result. A SceneElement will be stored in results based on
	 * the following criteria:
	 * <ul>
	 * <li>If the element's world bounds are null, or intersect or lie within the
	 * given volume.</li>
	 * <li>It's an instance of the given index type, where 'instance of' follows
	 * the definition of the instanceof keyword.</li>
	 * </ul>
	 * If index is null or equal to SceneElement, then the last point always
	 * passes.
	 * </p>
	 * <p>
	 * If results is null, a new Bag<SceneElement is instantiated and used.
	 * Before beginning the query, results will be cleared using a fast clear.
	 * A fast clear is used so that re-using Bags is as efficient as possible.
	 * When the results are returned, they will be contained in 0 to size() -1.
	 * </p>
	 * <p>
	 * This method should only be called after an update. If SceneElements or
	 * Cells are modified without a subsequent update, the Scene's internal
	 * state may be inconsistent. This will not cause the query to fail, but it
	 * may return invalid or incomplete results.
	 * </p>
	 * 
	 * @param volume The BoundVolume to test intersections against
	 * @param index The required parent class for all returned SceneElements
	 * @param results The results storage for the query, may be null
	 * @return results, or a new SceneQueryResult if null
	 * @throws NullPointerException if volume is null
	 */
	public Bag<SceneElement> query(BoundVolume volume, Class<? extends SceneElement> index, 
								  Bag<SceneElement> results) {
		if (volume == null)
			throw new NullPointerException("Cannot query against a null BoundVolume");
		
		if (results == null)
			results = new Bag<SceneElement>();
		results.clear(true);
		
		int length = cells.size();
		for (int i = 0; i < length; i++) {
			cells.get(i).query(volume, index, results);
		}
		
		return results;
	}
	
	/* Place the given element into the proper Cell.
	 * This assumes that the cells are correctly sorted by priority. */
	private void placeElement(SceneElement element) {
		int cellCount = cells.size();
		for (int i = 0; i < cellCount; i++) {
			if (cells.get(i).add(element))
				break;
		}
	}
}
