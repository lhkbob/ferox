package com.ferox.scene;

import java.util.Comparator;
import java.util.Iterator;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.util.Bag;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;

/**
 * <p>
 * SceneController is a Controller that performs the necessary processing to
 * make SceneElements useful when describing a scene. The SceneController is
 * responsible for running all SceneElementUpdaters assigned to SceneElements
 * and computing their world bounds. In addition, the SceneController manages a
 * prioritized ordering of {@link Cell}s. These cells efficiently partition each
 * SceneElement entity based on different algorithms such as BSPs, octrees and
 * the like.
 * </p>
 * <p>
 * The SceneController does not modify whether or not a SceneElement is
 * potentially visible (see {@link SceneElement#isPotentiallyVisible()}). It is
 * assumed that additional Controllers are used to make these determinations,
 * potentially with the aid of the SceneControllers
 * {@link #query(BoundVolume, Bag)} and {@link #query(Frustum, Bag)}.
 * <p>
 * A SceneController is intended to be used with only one EntitySystem. It is
 * recommended that it is registered with the EntitySystem, as well. The
 * SceneController does not require it to perform correctly, but other
 * Controllers may depend on access to an EntitySystem's SceneController to help
 * perform spatial queries.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class SceneController implements Controller {
	private static final int BT_ID = Component.getTypeId(BillboardTarget.class);
	private static final int ED_ID = Component.getTypeId(ElementData.class);
	private static final int SE_ID = Component.getTypeId(SceneElement.class);
	
	private static final Comparator<Cell> prioritySorter = new Comparator<Cell>() {
		@Override
		public int compare(Cell o1, Cell o2) {
			return o1.getPriority() - o2.getPriority();
		}
	};

	private float deltaTime;
	private long lastProcess;
	
	private final Bag<Cell> cells;
	private boolean cellsModified;

	/**
	 * <p>
	 * Create a new SceneController that initially has a single UnboundedCell
	 * with minimum priority. This Cell cannot be removed and is used as a catch
	 * all for SceneElements that aren't assigned to any configured Cells.
	 * </p>
	 * <p>
	 * If system is not null, then this SceneController automatically registers
	 * itself with the given system. Otherwise, it's assumed that it will be
	 * registered later.
	 * </p>
	 * 
	 * @param system The EntitySystem that this SceneController registers itself
	 *            with
	 */
	public SceneController(EntitySystem system) {
		lastProcess = -1;
		cells = new Bag<Cell>();
		
		// set up the default cell at the lowest possible priority
		UnboundedCell dfltCell = new UnboundedCell();
		dfltCell.setPriority(Integer.MIN_VALUE);
		add(dfltCell);
		
		if (system != null)
			system.registerController(this);
	}

	/**
	 * <p>
	 * Add the given Cell to this SceneController, so that it can help
	 * efficiently organize any Entities that have a SceneElement component.
	 * <tt>cell</tt> will be automatically updated each frame to include the
	 * correct SceneElements, based on its priority and the other Cell's added
	 * to this SceneController.
	 * </p>
	 * <p>
	 * This method does nothing if cell is null or if cell is already held by
	 * this SceneController. If cell is owned by a different SceneController
	 * instance, it is first removed from that SceneController and then added to
	 * this one.
	 * </p>
	 * 
	 * @param cell The Cell to add to this SceneController
	 */
	public void add(Cell cell) {
		if (cell != null && cell.owner != this) {
			if (cell.owner != null)
				cell.owner.remove(cell);
			
			cells.add(cell);
			cell.owner = this;
			
			cellsModified = true;
		}
	}

	/**
	 * <p>
	 * Remove the given Cell from this SceneController so that it no longer
	 * partitions SceneElements. The Entities within cell remain in the
	 * EntitySystem and will be placed in another Cell at the next update.
	 * </p>
	 * <p>
	 * If cell is null or not owned by this SceneController, then this method is
	 * a no-op.
	 * </p>
	 * 
	 * @param cell The Cell to remove
	 * @return True if cell was successfully removed from this SceneController
	 */
	public boolean remove(Cell cell) {
		if (cell != null && cell.owner == this) {
			cells.remove(cell);
			cell.clear();
			
			cell.owner = null;
			return true;
		}
		
		return false;
	}

	/**
	 * <p>
	 * Perform a query against all Entities that are SceneElements and store the
	 * results into result. A Entity will be stored in results if it's
	 * associated SceneElement has a null world bounds, or intersects or lies
	 * within <tt>frustum</tt>.
	 * <p>
	 * If results is null, a new Bag<Entity> is instantiated and used. Before
	 * beginning the query, results will be cleared using a fast clear. A fast
	 * clear is used so that re-using Bags is as efficient as possible. When the
	 * results are returned, they will be contained in 0 to
	 * <code>size() - 1</code>.
	 * </p>
	 * <p>
	 * This method should only be called after the SceneController has been
	 * processed. If SceneElements or Cells are modified without a subsequent
	 * processing, the SceneController's internal state may be inconsistent.
	 * This will not cause the query to fail, but it may return invalid or
	 * incomplete results.
	 * </p>
	 * 
	 * @param frustum The Frustum to test visibility against
	 * @param results The results storage for the query, may be null
	 * @return results, or a new Bag, filled with the query results
	 * @throws NullPointerException if frustum is null
	 */
	public Bag<Entity> query(Frustum frustum, Bag<Entity> results) {
		if (frustum == null)
			throw new NullPointerException("Cannot query against a null Frustum");
		
		if (results == null)
			results = new Bag<Entity>();
		results.clear(false);
		
		int length = cells.size();
		for (int i = 0; i < length; i++) {
			cells.get(i).query(frustum, results);
		}
		
		return results;
	}
	
	/**
	 * <p>
	 * Perform a query against all Entities that are SceneElements and store the
	 * results into result. A Entity will be stored in results if it's
	 * associated SceneElement has a null world bounds, or intersects or lies
	 * within <tt>frustum</tt>.
	 * <p>
	 * If results is null, a new Bag<Entity> is instantiated and used. Before
	 * beginning the query, results will be cleared using a fast clear. A fast
	 * clear is used so that re-using Bags is as efficient as possible. When the
	 * results are returned, they will be contained in 0 to
	 * <code>size() - 1</code>.
	 * </p>
	 * <p>
	 * This method should only be called after the SceneController has been
	 * processed. If SceneElements or Cells are modified without a subsequent
	 * processing, the SceneController's internal state may be inconsistent.
	 * This will not cause the query to fail, but it may return invalid or
	 * incomplete results.
	 * </p>
	 * 
	 * @param volume The BoundVolume to test visibility against
	 * @param results The results storage for the query, may be null
	 * @return results, or a new Bag, filled with the query results
	 * @throws NullPointerException if volume is null
	 */
	public Bag<Entity> query(BoundVolume volume, Bag<Entity> results) {
		if (volume == null)
			throw new NullPointerException("Cannot query against a null BoundVolume");
		
		if (results == null)
			results = new Bag<Entity>();
		results.clear(false);
		
		int length = cells.size();
		for (int i = 0; i < length; i++) {
			cells.get(i).query(volume, results);
		}
		
		return results;
	}
	
	/**
	 * Set the time delta, in seconds, that will be used for the next call to
	 * {@link #process(EntitySystem)}. If <tt>time</tt> is positive, it will be
	 * the value given to any {@link SceneElementUpdater}'s referenced in the
	 * EntitySystem, and as the dt parameter to each {@link Cell#update(float)}
	 * .</p>
	 * <p>
	 * If the value is negative, then the next call to process() will compute a
	 * time delta equal to the time from the last process to the current one.
	 * </p>
	 * <p>
	 * The set time delta is cleared after each process, so if a fixed-rate
	 * update is being used, it must be set each frame.
	 * </p>
	 * 
	 * @param time The time delta in seconds
	 */
	public void setDelta(float time) {
		deltaTime = time;
	}

	/**
	 * The SceneController performs the following operations, in order, during
	 * its processing:
	 * <ol>
	 * <li>Run {@link Cell#update(float)} on all Cells added to this
	 * SceneController</li>
	 * <li>Invoke {@link #process(Entity)} on every Entity within
	 * <tt>system</tt> that's a SceneElement and a {@link BillboardTarget}</li>
	 * <li>Process every remaining SceneElement within system</li>
	 * <li>Compute the world bounds and Cell owner for every SceneElement that's
	 * been modified</li>
	 * </ol>
	 * After SceneController has finished processing, its query methods will
	 * provide accurate and up-to-date results. The SceneController first
	 * processes the BillboardTargets so any SceneElements that reference its
	 * translation, etc. will use the correct values. Because of this, it is
	 * important that BillboardTargets do not depend on other SceneElements,
	 * since a stale billboard point or constraint axis may be used.
	 * 
	 * @param system The EntitySystem that's being processed
	 */
	@Override
	public void process(EntitySystem system) {
		if (system == null)
			throw new NullPointerException("EntitySystem cannot be null");
		
		long now = System.nanoTime();
		if (deltaTime < 0f) {
			// delta hasn't been overridden so compute it
			if (lastProcess < 0)
				deltaTime = 0f;
			else
				deltaTime = (now - lastProcess) / 1e9f;
		}
		lastProcess = now;
		
		// update all cells first
		boolean forceAllUpdate = cellsModified;
		int cellCount = cells.size();
		for (int i = 0; i < cellCount; i++)
			forceAllUpdate |= cells.get(i).update(deltaTime);
		
		// resort after the update in case the update switched priority
		cells.sort(prioritySorter);

		// first process all BillboardTargets
		Iterator<Entity> it = system.iterator(BT_ID);
		while(it.hasNext()) {
			process(it.next());
		}
		
		// now process all remaining SceneElements
		it = system.iterator(SE_ID);
		while(it.hasNext()) {
			process(it.next());
		}
		
		// now compute world bounds and manage cells
		ElementData d;
		SceneElement s;
		Entity e;
		it = system.iterator(ED_ID);
		while(it.hasNext()) {
			e = it.next();
			d = (ElementData) e.get(ED_ID);
			s = (SceneElement) e.get(SE_ID);
			
			if (s == null) {
				// Entity is no longer a SceneElement, so clean it up
				if (d.cellOwner != null)
					d.cellOwner.remove(e, null, d.cellData);
				it.remove();
			} else {
				// process update if necessary
				if (d.updated) {
					// compute new world bounds
					BoundVolume world = null;
			 		if (d.oldLocalBounds != null)
						world = d.oldLocalBounds.transform(d.oldTransform, s.getWorldBounds());
					s.setWorldBounds(world);
				}
				
				if (d.updated || d.cellOwner == null || forceAllUpdate)
					placeElement(e, s, d);
				
				d.updated = false;
				d.processed = false;
			}
			
		}
		
		cellsModified = false;
		deltaTime = -1f; // invalidate delta for next frame
	}

	/**
	 * <p>
	 * Perform all necessary computations on the given Entity that this
	 * SceneController would perform on the entity during
	 * {@link #process(EntitySystem)}. If the given Entity is a SceneElement,
	 * the SceneElement will have its SceneElementUpdater run (if it has one),
	 * and any billboarding computations will be performed.
	 * </p>
	 * <p>
	 * If the Entity has already been processed since the last call to
	 * {@link #process(EntitySystem)}, this method will do nothing. If the
	 * Entity is not a SceneElement, it will do nothing. This can be invoked
	 * whenever, although the time deltas may be inaccurate if not invoked from
	 * within the stack of a {@link #process(EntitySystem)} call. It is intended
	 * primarily for SceneElementUpdaters to use to ensure that a dependent
	 * Entity is processed before computing a Transform.
	 * </p>
	 * <p>
	 * The world bounds of the SceneElement are not computed by the
	 * SceneController until after every SceneElement Entity has been processed,
	 * so this method does not compute any world bounds.
	 * </p>
	 * 
	 * @param e The Entity to be processed
	 */
	public void process(Entity e) {
		ElementData data = (ElementData) e.get(ED_ID);
		SceneElement element = (SceneElement) e.get(SE_ID);
		
		if (element != null) {
			if (data == null) {
				data = new ElementData();
				e.add(data);
				processEntity(e, element, data);
			} else if (!data.processed) {
				processEntity(e, element, data);
			}
		}
	}
	
	private void placeElement(Entity e, SceneElement s, ElementData d) {
		Object o;
		Cell c;
		int cellCount = cells.size();
		for (int i = 0; i < cellCount; i++) {
			c = cells.get(i);
			o = c.add(e, s, (d.cellOwner == c ? d.cellData : null));
			if (o != null) {
				// found a new owner, if needed remove it from old cell
				if (d.cellOwner != c && d.cellOwner != null)
					d.cellOwner.remove(e, s, d.cellData);
				
				// store reference to new data
				d.cellData = o;
				d.cellOwner = c;
				break;
			}
		}
	}
	
	private void processEntity(Entity e, SceneElement se, ElementData ed) {
		// set this to true now so we don't get stack overflow problems if
		// this entity is processed again by its updater
		ed.processed = true;

		// run the SceneElementUpdater if it has one
		if (se.getUpdater() != null)
			se.getUpdater().update(e, this, deltaTime);
		
		// now compute the billboarding, note that this assumes the billboard point
		// and constraint vector are already processed, or are static vectors
		Transform t = se.getTransform();
		if (se.getBillboardPoint() != null) {
			// X = 0, Y = 1, Z = 2
			int o = se.getBillboardDirectionAxis().ordinal();
			Matrix3f r = t.getRotation();

			Vector3f d = se.getBillboardPoint().sub(t.getTranslation(), null).normalize();
			Vector3f a = r.getCol((o + 1) % 3, null).ortho(d).normalize();
			
			r.setCol(o, d);
			r.setCol((o + 1) % 3, a);
			r.setCol((o + 2) % 3, d.cross(a, a));
		}
		if (se.getConstraintVector() != null) {
			// X = 0, Y = 1, Z = 2
			int o = se.getConstraintAxis().ordinal();
			Matrix3f r = t.getRotation();

			Vector3f d = se.getConstraintVector().normalize(null);
			Vector3f a = r.getCol((o + 1) % 3, null).ortho(d).normalize();
			
			r.setCol(o, d);
			r.setCol((o + 1) % 3, a);
			r.setCol((o + 2) % 3, d.cross(a, a));
		}
		
		// check if the transform has changed
		ed.updated = false;
		if (!ed.oldTransform.equals(t)) {
			ed.oldTransform.set(t);
			ed.updated = true;
		}
		
		// check if the local bounds have changed
		if (se.getLocalBounds() == null) {
			if (ed.oldLocalBounds != null) {
				ed.oldLocalBounds = null;
				ed.updated = true;
			}
		} else {
			if (ed.oldLocalBounds == null || !ed.oldLocalBounds.equals(se.getLocalBounds())) {
				ed.oldLocalBounds = se.getLocalBounds().clone(ed.oldLocalBounds);
				ed.updated = true;
			}
		}
	}
	
	private static class ElementData extends Component {
		private static final String DESCR = "Internal data used by SceneController to manage SceneElements";
		
		final Transform oldTransform;
		BoundVolume oldLocalBounds;
		boolean processed;
		boolean updated;
		
		Cell cellOwner;
		Object cellData;
		
		public ElementData() {
			super(DESCR);
			oldTransform = new Transform();
		}
	}
}
