package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.Transform;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.scene.SceneElement;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;

public class SceneController implements Controller {
	private static final ComponentId<ElementData> ED_ID = Component.getComponentId(ElementData.class);
	private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);

	private final SpatialHierarchy<Entity> hierarchy;
	
	public SceneController(SpatialHierarchy<Entity> hierarchy) {
		if (hierarchy == null)
			throw new NullPointerException("SpatialHierarchy cannot be null");
		this.hierarchy = hierarchy;
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
				ed.oldLocalBounds = se.getLocalBounds().clone(ed.oldLocalBounds);
				updated = true;
			}
		}
		
		if (updated) {
			// compute new world bounds
			BoundVolume world = null;
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
		// FIXME: what happens when an entity is removed? we need listeners
		// to properly cleanup the hierarchy
		
		// other option is to have a clear() option in hierarchy that will
		// remove all entities not updated since last clear() -> this is slower
		// or requires a lot of extra effort on the part of the hierarchy
		Object newKey = hierarchy.insert(e, s.getWorldBounds(), d.hierarchyKey);
		d.hierarchyKey = newKey;

	}

	/*
	 * Meta-Component that tracks the SceneElement changes to reduce
	 * inserts and changes to the spatial-hierarchies
	 */
	private static class ElementData extends Component {
		final Transform oldTransform;
		BoundVolume oldLocalBounds;
		
		Object hierarchyKey;
		
		public ElementData() {
			oldTransform = new Transform();
		}
	}
}
