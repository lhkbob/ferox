package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.Transform;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.scene.SceneElement;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntityListener;
import com.ferox.util.entity.EntitySystem;

public class SceneController implements Controller {
	private static final ComponentId<ElementData> ED_ID = Component.getComponentId(ElementData.class);
	private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);

	private final SpatialHierarchy<Entity> hierarchy;
	private final SceneElementListener listener;
	
	public SceneController(SpatialHierarchy<Entity> hierarchy) {
		if (hierarchy == null)
			throw new NullPointerException("SpatialHierarchy cannot be null");
		this.hierarchy = hierarchy;
		listener = new SceneElementListener();
	}
	
	@Override
	public void process(EntitySystem system) {
		// make sure we have an index over SceneElements
		system.addIndex(SE_ID);
		
		// process all SceneElements
		Entity e;
		SceneElement element;
		Iterator<Entity> it = system.iterator(SE_ID);
		while(it.hasNext()) {
			e = it.next();
			element = e.get(SE_ID);
			if (element != null) {
				ElementData data = e.getMeta(element, ED_ID);

				if (data == null) {
					// new scene element
					data = new ElementData();
					e.addMeta(element, data);
					e.addListener(listener);
				}
				processEntity(e, element, data);
			}
		}
	}
	
	private void processEntity(Entity e, SceneElement se, ElementData ed) {
		boolean updated = false;
		
		Transform t = se.getTransform();
		// check if the transform has changed
		if (!ed.oldTransform.equals(t)) {
			ed.oldTransform.set(t);
			updated = true;
		}
		
		// check if the local bounds have changed
		if (se.getLocalBounds() == null) {
			if (ed.oldLocalBounds != null) {
				ed.oldLocalBounds = null;
				updated = true;
			}
		} else {
			if (ed.oldLocalBounds == null || !ed.oldLocalBounds.equals(se.getLocalBounds())) {
				if (ed.oldLocalBounds == null)
					ed.oldLocalBounds = new AxisAlignedBox(se.getLocalBounds());
				else
					ed.oldLocalBounds.set(se.getLocalBounds());
				updated = true;
			}
		}
		
		if (updated) {
			// compute new world bounds
			AxisAlignedBox world = null;
	 		if (ed.oldLocalBounds != null)
				world = ed.oldLocalBounds.transform(ed.oldTransform, se.getWorldBounds());
			se.setWorldBounds(world);
		}
		
		if (updated || ed.hierarchyKey == null)
			placeElement(e, se, ed);
		
		// reset visibility
		se.resetVisibility();
	}
	
	private void placeElement(Entity e, SceneElement s, ElementData d) {
		Object newKey = hierarchy.insert(e, s.getWorldBounds(), d.hierarchyKey);
		d.hierarchyKey = newKey;

	}
	
	private class SceneElementListener implements EntityListener {
		@Override
		public void onComponentAdd(Entity e, Component c) {
			// cleanup entity data if c is a scene element and
			// the entity previously had another scene element
			if (c instanceof SceneElement) {
				SceneElement old = e.get(SE_ID);
				if (old != null)
					cleanupEntityData(e, old);
			}
		}

		@Override
		public void onComponentRemove(Entity e, Component c) {
			if (c instanceof SceneElement) {
				// this is called before the meta-components are actually removed
				cleanupEntityData(e, (SceneElement) c);
				// remove this listener from the entity
				e.removeListener(this);
			}
		}

		@Override
		public void onSystemAdd(Entity e, EntitySystem system) {
			// do nothing
		}

		@Override
		public void onSystemRemove(Entity e, EntitySystem system) {
			// note that we don't need to check the system because the listener
			// is only added to scene elements already processed by this controller.
			// any system change by an entity must always first have a removal.
			
			SceneElement se = e.get(SE_ID);
			if (se != null) {
				cleanupEntityData(e, se);
				// remove this listener from the entity
				e.removeListener(this);
			}
		}
		
		private void cleanupEntityData(Entity e, SceneElement se) {
			ElementData data = e.getMeta(se, ED_ID);
			if (data != null && data.hierarchyKey != null) {
				// remove the entity from the hierarchy so it can possibly be garbage collected
				hierarchy.remove(e, data.hierarchyKey);
			}
		}
	}

	/*
	 * Meta-Component that tracks the SceneElement changes to reduce
	 * inserts and changes to the spatial-hierarchies
	 */
	private static class ElementData extends Component {
		final Transform oldTransform;
		AxisAlignedBox oldLocalBounds;
		
		Object hierarchyKey;
		
		public ElementData() {
			oldTransform = new Transform();
			oldLocalBounds = new AxisAlignedBox();
		}
	}
}
