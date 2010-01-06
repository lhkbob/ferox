package com.ferox.entity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ferox.util.Bag;

public final class EntitySystem implements Iterable<Entity> {
	private int entityIdSeq;
	
	private ComponentTable[] componentTables;
	private final Bag<Entity> allList;
	
	public EntitySystem() {
		entityIdSeq = 0;
		
		componentTables = new ComponentTable[8];
		allList = new Bag<Entity>();
	}
	
	public Iterator<Entity> iterator() {
		return new EntitySystemIterator();
	}
	
	public Iterator<Entity> iterator(Class<? extends Component> componentType) {
		return iterator(Component.getTypeId(componentType));
	}
	
	public Iterator<Entity> iterator(int componentId) {
		if (componentId < 0 || componentId >= componentTables.length || componentTables[componentId] == null)
			return new NullIterator();
		else
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
		ComponentTable table;

		int type = c.getTypeId();
		if (type >= componentTables.length) {
			// need a new component table
			table = new ComponentTable(type);
			componentTables = Arrays.copyOf(componentTables, type + 1);
			componentTables[type] = table;
		} else 
			table = componentTables[type];
		
		table.entities.add(entity);
	}
	
	void detach(Entity entity, Component c) {
		int type = c.getTypeId();
		if (type > 0 && type < componentTables.length && componentTables[type] != null)
			componentTables[type].entities.remove(entity);
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
}
