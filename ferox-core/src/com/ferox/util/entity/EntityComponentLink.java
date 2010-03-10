package com.ferox.util.entity;

import java.util.HashMap;
import java.util.Map;

import com.ferox.util.entity.ComponentTable.Index;

final class EntityComponentLink {
	private final Entity entity;
	
	private Index indexData;
	
	private Component primaryComponent;
	private Map<ComponentId<?>, Component> attachedComponents;
	
	public EntityComponentLink(Entity e) {
		entity = e;
	}
	
	public Index getIndex() {
		return indexData;
	}
	
	public void setIndex(Index index) {
		indexData = index;
	}
	
	public Entity getEntity() {
		return entity;
	}
	
	public Component getComponent() {
		return primaryComponent;
	}
	
	public void setComponent(Component component) {
		if (primaryComponent != component) {
			// any change, we clear the meta-components
			attachedComponents = null;
			
			if (component == null) {
				// possibly remove the old index since we're clearing the link
				if (indexData != null)
					indexData.clearIndex();
				
				if (entity.owner != null)
					entity.owner.decrementCount(primaryComponent.getComponentId());
			} else if (primaryComponent == null || indexData == null) {
				// add a link if this a new link, or if we didn't have any index data before
				if (entity.owner != null) {
					ComponentTable table = entity.owner.lookupTable(component.getComponentId().getId());
					if (table != null)
						table.add(this);
					
					if (primaryComponent == null) // only increase count when its a new link
						entity.owner.incrementCount(component.getComponentId());
				}
			}
			primaryComponent = component;
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T getMetaComponent(ComponentId<T> id) {
		return (attachedComponents == null ? null : (T) attachedComponents.get(id));
	}
	
	public <T extends Component> void setMetaComponent(ComponentId<T> id, T component) {
		if (attachedComponents == null)
			attachedComponents = new HashMap<ComponentId<?>, Component>();
		
		if (component == null)
			attachedComponents.put(id, component);
		else
			attachedComponents.remove(id);
	}
	
	public void setTable(ComponentTable newTable) {
		if (primaryComponent != null) {
			if (indexData != null)
				indexData.clearIndex(); // invokes #setIndex(null)
			if (newTable != null)
				newTable.add(this); // add new index data
		}
	}
}
