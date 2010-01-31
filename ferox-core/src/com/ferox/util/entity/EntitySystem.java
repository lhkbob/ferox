package com.ferox.util.entity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.ferox.util.Bag;

public final class EntitySystem implements Iterable<Entity> {
	private int entityIdSeq;
	
	private int[] componentCounts;
	private ComponentTable[] componentTables;
	private final Bag<Entity> allList;
	
	private final Map<Class<? extends Controller>, Controller> controllers;
	
	public EntitySystem() {
		entityIdSeq = 0;
		
		componentCounts = new int[8];
		componentTables = new ComponentTable[8];
		allList = new Bag<Entity>();
		
		controllers = new HashMap<Class<? extends Controller>, Controller>();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Controller> T getController(Class<T> type) {
		if (type == null)
			throw new NullPointerException("Type cannot be null");
		
		return (T) controllers.get(type);
	}
	
	@SuppressWarnings("unchecked")
	<T extends Controller> void registerController(T controller) {
		if (controller == null)
			throw new NullPointerException("Controller cannot be null");
		
		Class<? extends Controller> type = controller.getClass();
		T old = (T) controllers.get(type);
		if (old != null)
			throw new IllegalArgumentException("There already exists a " + type.getSimpleName() + " within the EntitySystem");
		else
			controllers.put(type, controller);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Controller> T removeController(Class<T> type) {
		if (type == null)
			throw new NullPointerException("Type cannot be null");
		
		T old = (T) controllers.remove(type);
		if (old != null)
			old.invalid = true;
		return old;
	}
	
	@Override
	public Iterator<Entity> iterator() {
		return new EntitySystemIterator();
	}
	
	public Iterator<Entity> iterator(Class<? extends Component> componentType) {
		return iterator(Component.getTypeId(componentType));
	}
	
	public Iterator<Entity> iterator(int componentId) {
		if (componentId < 0 || componentId >= componentTables.length || componentTables[componentId] == null) {
			if (componentId >= 0 && componentId < componentCounts.length && componentCounts[componentId] > 0)
				return new ComponentViewIterator(componentId);
			else
				return new NullIterator();
		} else
			return componentTables[componentId].iterator();
	}
	
	public void removeAll() {
		for(Entity e: this)
			e.setValid(false);
			
		for (int i = 0; i < componentTables.length; i++) {
			if (componentTables[i] != null)
				componentTables[i].entities.clear();
		}
		allList.clear();
	}
	
	public int getEntityCount() {
		return allList.size();
	}
	
	public Entity newEntity() {
		Entity e = new Entity(this, entityIdSeq++);
		allList.add(e);
		
		return e;
	}
	
	public void removeEntity(Entity e) {
		if (e == null)
			throw new NullPointerException("Cannot remove a null Entity");
		if (e.getOwner() != this)
			throw new IllegalArgumentException("Entity must be owned by this EntitySystem");
		
		if (e.setValid(false)) {
			// remove the entity from all tables that reference it
			for (Component c: e)
				detach(e, c);
			allList.remove(e);
		}
	}
	
	public void addEntity(Entity e) {
		if (e == null)
			throw new NullPointerException("Cannot add a null Entity");
		if (e.getOwner() != this)
			throw new IllegalArgumentException("Entity must be owned by this EntitySystem");
		
		if (e.setValid(true)) {
			// add back entity to any component tables it has
			for (Component c: e)
				attach(e, c);
			allList.add(e);
		}
	}
	
	void attach(Entity entity, Component c) {
		// update component counts
		int type = c.getTypeId();
		if (type >= componentCounts.length)
			componentCounts = Arrays.copyOf(componentCounts, type + 1);
		componentCounts[type]++;
		
		if (c.isIndexable()) {
			// add to the component table, too
			ComponentTable table;
			if (type >= componentTables.length || componentTables[type] == null) {
				// need a new component table
				table = new ComponentTable(type);
				if (type >= componentTables.length)
					componentTables = Arrays.copyOf(componentTables, type + 1);
				componentTables[type] = table;
			} else 
				table = componentTables[type];

			table.entities.add(entity);
		}
	}
	
	void detach(Entity entity, Component c) {
		int type = c.getTypeId();
		if (type < componentCounts.length)
			componentCounts[type] = Math.max(0, componentCounts[type] - 1);
		
		if (c.isIndexable()) {
			if (type < componentTables.length && componentTables[type] != null)
				componentTables[type].entities.remove(entity);
		}
	}
	
	/* Iterator that iterates over all entities in the system */
	private class EntitySystemIterator implements Iterator<Entity> {
		private final Iterator<Entity> iterator;
		private Entity current;
		
		public EntitySystemIterator() {
			iterator = allList.iterator();
			current = null;
		}
		
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Entity next() {
			current = iterator.next();
			return current;
		}

		@Override
		public void remove() {
			iterator.remove();
			if (current.setValid(false)) {
				for (Component c: current)
					detach(current, c);
			}
		}
	}
	
	/* Iterator that iterates over all entities, but only returns those that
	 * match a certain type. */
	private class ComponentViewIterator implements Iterator<Entity> {
		private final Iterator<Entity> iterator;
		private final int componentType;
		
		private Entity toReturn;
		private Entity current;
		
		public ComponentViewIterator(int type) {
			iterator = allList.iterator();
			componentType = type;
		}
		
		@Override
		public boolean hasNext() {
			if (toReturn == null)
				advance();
			return toReturn != null;
		}

		@Override
		public Entity next() {
			if (toReturn == null)
				advance();
			current = toReturn;
			toReturn = null;
			
			if (current == null)
				throw new NoSuchElementException();
			else
				return current;
		}

		@Override
		public void remove() {
			// let the iterator handle everything
			iterator.remove();
		}
		
		private void advance() {
			toReturn = null;
			while(iterator.hasNext()) {
				toReturn = iterator.next();
				if (toReturn.get(componentType) != null)
					break;
				else
					toReturn = null; // clear it
			}
		}
	}
	
	private static class NullIterator implements Iterator<Entity> {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Entity next() {
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new IllegalStateException();
		}
	}
	
	private static class ComponentTable implements Iterable<Entity> {
		final int typeId;
		final Bag<Entity> entities;
		
		public ComponentTable(int typeId) {
			this.typeId = typeId;
			entities = new Bag<Entity>();
		}

		@Override
		public Iterator<Entity> iterator() {
			return new ComponentTableIterator();
		}
		
		private class ComponentTableIterator implements Iterator<Entity> {
			private final Iterator<Entity> iterator;
			private Entity current;
			
			public ComponentTableIterator() {
				iterator = entities.iterator();
				current = null;
			}
			
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Entity next() {
				current = iterator.next();
				return current;
			}

			@Override
			public void remove() {
				iterator.remove();
				current.detach(typeId);
			}
		}
	}
}
