package com.ferox.entity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Entity implements Iterable<Component> {
	private final int id;
	private final EntitySystem owner;
	
	private boolean valid;
	
	private Component[] components;
	
	Entity(EntitySystem owner, int id) {
		this.owner = owner;
		this.id = id;
		
		valid = true;
		components = new Component[8];
	}
	
	public void add(Component c) {
		if (c == null)
			throw new NullPointerException("Component cannot be null");
		
		int type = c.getTypeId();
		if (type >= components.length)
			components = Arrays.copyOf(components, type + 1);
		components[type] = c;
		
		owner.attach(this, c);
	}
	
	public Component remove(int type) {
		if (type >= 0 && type < components.length) {
			Component old = components[type];
			if (old != null) {
				components[type] = null;
				owner.detach(this, old);
			}
			return old;
		} else
			return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T remove(Class<T> type) {
		int typeId = Component.getTypeId(type);
		return (T) remove(typeId);
	}
	
	public Component get(int type) {
		if (type >= 0 && type < components.length)
			return components[type];
		else
			return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T get(Class<T> type) {
		int typeId = Component.getTypeId(type);
		return (T) get(typeId);
	}
	
	public EntitySystem getOwner() {
		return owner;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isValid() {
		return valid;
	}
	
	@Override
	public Iterator<Component> iterator() {
		return new ComponentIterator();
	}
	
	void detach(int componentType) {
		if (componentType >= 0 && componentType < components.length)
			components[componentType] = null;
	}
	
	boolean setValid(boolean valid) {
		if (this.valid != valid) {
			this.valid = valid;
			return true;
		} else
			return false;
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
			
			owner.detach(Entity.this, components[index]);
			components[index] = null;
		}
	}
}
