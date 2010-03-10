package com.ferox.util.entity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Entity implements Iterable<Component> {
	EntitySystem owner;
	int systemIndex; // index into owner.allEntities
	
	private EntityComponentLink[] components;
	
	public Entity() {
		this((Component[]) null);
	}
	
	public Entity(Component... comps) {
		owner = null;
		systemIndex = -1;
		components = new EntityComponentLink[4];
		
		if (comps != null) {
			for (int i = 0; i < comps.length; i++)
				add(comps[i]);
		}
	}
	
	public EntitySystem getOwner() {
		return owner;
	}
	
	public Entity add(Component c) {
		if (c == null)
			throw new NullPointerException("Component cannot be null");
		
		int id = c.getComponentId().getId();
		if (id >= components.length)
			components = Arrays.copyOf(components, id + 1);
		
		EntityComponentLink link = components[id];
		if (link == null) {
			link = new EntityComponentLink(this);
			components[id] = link;
		}
		link.setComponent(c);
		
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> boolean addMeta(Component attach, T meta) {
		if (attach == null || meta == null)
			throw new NullPointerException("Arguments cannot be null");
		
		int id = attach.getComponentId().getId();
		if (id < components.length && components[id] != null) {
			EntityComponentLink link = components[id];
			if (link.getComponent() == attach) {
				link.setMetaComponent((ComponentId<T>) meta.getComponentId(), meta);
				return true;
			}
		}
		
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T get(ComponentId<T> id) {
		if (id == null)
			throw new NullPointerException("Id cannot be null");
		
		int index = id.getId();
		if (index < components.length && components[index] != null)
			return (T) components[index].getComponent();
		
		return null;
	}
	
	public <T extends Component> T getMeta(Component attach, ComponentId<T> meta) {
		if (attach == null || meta == null)
			throw new NullPointerException("Arguments cannot be null");
		
		int id = attach.getComponentId().getId();
		if (id < components.length && components[id] != null) {
			EntityComponentLink link = components[id];
			if (link.getComponent() == attach)
				return link.getMetaComponent(meta);
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T remove(ComponentId<T> id) {
		if (id == null)
			throw new NullPointerException("Id cannot be null");
		
		int index = id.getId();
		if (index < components.length && components[index] != null) {
			EntityComponentLink link = components[index];
			T component = (T) link.getComponent();
			
			// clear the link
			link.setComponent(null);
			return component;
		}
		
		return null;
	}
	
	public boolean remove(Component c) {
		if (c == null)
			throw new NullPointerException("Component cannot be null");
		
		int index = c.getComponentId().getId();
		if (index < components.length && components[index] != null) {
			EntityComponentLink link = components[index];
			if (link.getComponent() == c) {
				// c is still linked, so we can remove it
				link.setComponent(null);
				return true;
			}
		}
		
		// if we got here, we had no link or a link with a different component
		return false;
	}
	
	public <T extends Component> T removeMeta(Component attach, ComponentId<T> meta) {
		if (attach == null || meta == null)
			throw new NullPointerException("Arguments cannot be null");
		
		int index = attach.getComponentId().getId();
		if (index < components.length && components[index] != null) {
			EntityComponentLink link = components[index];
			if (link.getComponent() == attach) {
				// components match so we can remove the meta component
				T old = link.getMetaComponent(meta);
				link.setMetaComponent(meta, null);
				return old;
			}
		}
		
		return null;
	}
	
	@Override
	public Iterator<Component> iterator() {
		return new EntityIterator();
	}
	
	void updateIndices() {
		if (owner != null) {
			for (int i = 0; i < components.length; i++) {
				if (components[i] != null)
					components[i].setTable(owner.lookupTable(i));
			}
		} else {
			for (int i = 0; i < components.length; i++) {
				if (components[i] != null)
					components[i].setTable(null);
			}
		}
	}
	
	void updateIndex(int typeId, ComponentTable table) {
		if (typeId < components.length && components[typeId] != null)
			components[typeId].setTable(table);
	}
	
	private class EntityIterator implements Iterator<Component> {
		int index;
		final int maxComponentId;
		
		public EntityIterator() {
			maxComponentId = components.length - 1;
			index = -1;
		}
		
		@Override
		public boolean hasNext() {
			for (int i = index + 1; i <= maxComponentId; i++) {
				if (components[i] != null && components[i].getComponent() != null)
					return true;
			}
			return false;
		}

		@Override
		public Component next() {
			for (int i = index + 1; i <= maxComponentId; i++) {
				if (components[i] != null && components[i].getComponent() != null) {
					index = i;
					return components[i].getComponent();
				}
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			if (index < 0)
				throw new IllegalStateException("Must call next() before first calling remove()");
			if (components[index] == null || components[index].getComponent() == null)
				throw new IllegalStateException("Already called remove()");
			
			components[index].setComponent(null);
		}
	}
}
