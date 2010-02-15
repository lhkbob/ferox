package com.ferox.util.entity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Entity implements Iterable<Component> {
	private EntitySystem owner;
		
	private Component[] components;
	
	public Entity() {
		this((Component[]) null);
	}
	
	public Entity(Component... components) {
		owner = null;
		this.components = new Component[(components == null ? 8 : components.length)];
		if (components != null) {
			for (int i = 0; i < components.length; i++)
				add(components[i]);
		}
	}
	
	public Entity add(Component c) {
		if (c == null)
			throw new NullPointerException("Component cannot be null");
		
		int type = c.getComponentId().getId();
		if (type >= components.length)
			components = Arrays.copyOf(components, type + 1);
		components[type] = c;
		
		if (owner != null)
			owner.attach(this, c);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T remove(ComponentId<T> id) {
		int type = id.getId();
		if (type >= 0 && type < components.length) {
			Component old = components[type];
			if (old != null) {
				components[type] = null;
				owner.detach(this, old);
			}
			
			return (T) old;
		} else
			return null;
	}
	
	public <T extends Component> T remove(Class<T> type) {
		return remove(Component.getComponentId(type));
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T get(ComponentId<T> id) {
		int type = id.getId();
		if (type >= 0 && type < components.length)
			return (T) components[type];
		else
			return null;
	}
	
	public <T extends Component> T get(Class<T> type) {
		return get(Component.getComponentId(type));
	}
	
	public EntitySystem getOwner() {
		return owner;
	}
	
	@Override
	public Iterator<Component> iterator() {
		return new ComponentIterator();
	}
	
	void detach(int componentType) {
		if (componentType >= 0 && componentType < components.length)
			components[componentType] = null;
	}
	
	void setOwner(EntitySystem owner) {
		// assign new owner
		this.owner = owner;
	}
	
	private class ComponentIterator implements Iterator<Component> {
		int index;
		final int maxComponentId;
		
		public ComponentIterator() {
			maxComponentId = components.length - 1;
			index = -1;
		}
		
		@Override
		public boolean hasNext() {
			for (int i = index + 1; i <= maxComponentId; i++) {
				if (components[i] != null)
					return true;
			}
			return false;
		}

		@Override
		public Component next() {
			for (int i = index + 1; i <= maxComponentId; i++) {
				if (components[i] != null) {
					index = i;
					return components[i];
				}
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			if (index < 0)
				throw new IllegalStateException("Must call next() before first calling remove()");
			if (components[index] == null)
				throw new IllegalStateException("Already called remove()");
			
			if (owner != null)
				owner.detach(Entity.this, components[index]);
			components[index] = null;
		}
	}
}
