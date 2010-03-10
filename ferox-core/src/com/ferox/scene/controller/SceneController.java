package com.ferox.scene.controller;

import java.util.Comparator;
import java.util.Iterator;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.scene.Cell;
import com.ferox.scene.SceneElement;
import com.ferox.util.Bag;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Description;
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
 * The SceneController resets every SceneElement's
 * {@link SceneElement#isPotentiallyVisible() potentially visible status} to
 * false. It is assumed that additional Controllers are used to set the
 * appropriate SceneElement's status back to true, potentially with the aid of
 * the SceneControllers {@link #query(BoundVolume, Bag)} and
 * {@link #query(Frustum, Bag)}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class SceneController implements Controller {
	private static final ComponentId<ElementData> ED_ID = Component.getComponentId(ElementData.class);
	private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);

	/**
	 * <p>
	 * Create a new SceneController that initially has a single UnboundedCell
	 * with minimum priority. This Cell cannot be removed and is used as a catch
	 * all for SceneElements that aren't assigned to any configured Cells.
	 * </p>
	 */
	public SceneController() {
		
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
	 * @throws NullPointerException if system is null
	 */
	@Override
	public void process(EntitySystem system) {
		// now process all remaining SceneElements
		Iterator<Entity> it = system.iterator(SE_ID);
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
			d = e.get(ED_ID);
			s = e.get(SE_ID);
			
			if (s == null) {
				// Entity is no longer a SceneElement, so clean it up
				if (d.owner != null)
					d.owner.remove(e, d.data);
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
				
				// FIXME: we should always place this... how can we track when new
				// spatial hierarchies might get added, in which case we have to reassign
				// everything since the priorities could have changed??
				if (d.updated || d.cellOwner == null)
					placeElement(e, s, d);
				
				d.updated = false;
			}
			
		}
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
	private void process(Entity e) {
		ElementData data = e.get(ED_ID);
		SceneElement element = e.get(SE_ID);
		
		if (element != null) {
			if (data == null) {
				// new scene element
				data = new ElementData();
				e.add(data);
			}
			processEntity(e, element, data);

			// reset visibility
			element.setPotentiallyVisible(false);
		}
	}
	
	// FIXME: can we make this part of SpatialHierarchyUtil??
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
		Transform t = se.getTransform();
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
	
	@Description("Internal data used by SceneController to manage SceneElements")
	private static class ElementData extends Component {
		final Transform oldTransform;
		BoundVolume oldLocalBounds;
		boolean updated;
		
		SpatialHierarchy<Entity> owner;
		Object data;
		
		public ElementData() {
			oldTransform = new Transform();
		}
	}
}
