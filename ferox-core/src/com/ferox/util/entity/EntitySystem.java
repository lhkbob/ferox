package com.ferox.util.entity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ferox.util.Bag;

public class EntitySystem implements Iterable<Entity> {
	private ComponentTable[] tables;
	private int[] tableCounts;
	private final Bag<Entity> allEntities;
	
	public EntitySystem() {
		allEntities = new Bag<Entity>();
		tables = new ComponentTable[0];
		tableCounts = new int[0];
	}
	
	@Override
	public Iterator<Entity> iterator() {
		return new EntitySystemIterator();
	}
	
	public Iterator<Entity> iterator(ComponentId<?> id) {
		if (id == null)
			throw new NullPointerException("ComponentId cannot be null");
		
		int index = id.getId();
		if (index >= tableCounts.length || tableCounts[index] == 0)
			return new NullIterator(); // no table or entities available
		if (index < tables.length && tables[index] != null)
			return tables[index].iterator();
		else
			return new ComponentViewIterator(id);
	}
	
	public void addIndex(ComponentId<?> id) {
		if (id == null)
			throw new NullPointerException("Cannot add an index for a null ComponentId");
		
		int index = id.getId();
		if (index >= tables.length)
			tables = Arrays.copyOf(tables, index + 1);
		if (tables[index] == null) {
			tables[index] = new ComponentTable();
			for (Entity e: allEntities)
				e.updateIndex(index, tables[index]);
		} // else don't need anything
	}
	
	public void removeIndex(ComponentId<?> id) {
		if (id == null)
			throw new NullPointerException("Cannot remove an index for a null ComponentId");
		
		int index = id.getId();
		if (index < tables.length && tables[index] != null) {
			for (Entity e: tables[index])
				e.updateIndex(index, null);
			tables[index] = null;
		}
	}
	
	public boolean hasIndex(ComponentId<?> id) {
		if (id == null)
			throw new NullPointerException("ComponentId cannot be null");
		
		int index = id.getId();
		return index < tables.length && tables[index] != null;
	}
	
	public void add(Entity e) {
		if (e == null)
			throw new NullPointerException("Entity cannot be null");
		if (e.owner != null && e.owner != this)
			throw new IllegalArgumentException("Entity is already owned by another EntitySystem");
		
		if (e.owner == this)
			return; // don't need to do any extra work
		
		e.owner = this;
		allEntities.add(e);
		e.systemIndex = allEntities.size() - 1;
		
		e.updateIndices();
		
		for (Component c: e)
			incrementCount(c.getComponentId());
	}
	
	public void remove(Entity e) {
		if (e == null)
			throw new NullPointerException("Entity cannot be null");
		if (e.owner != this)
			throw new IllegalArgumentException("Entity must be owned by this EntitySystem");
		
		e.owner = null;
		allEntities.remove(e.systemIndex);
		if (allEntities.size() != e.systemIndex)
			allEntities.get(e.systemIndex).systemIndex = e.systemIndex;
		e.systemIndex = -1;
		
		e.updateIndices();
		
		for (Component c: e)
			decrementCount(c.getComponentId());
	}
	
	public void reset() {
		// remove all of the entities from this system
		Iterator<Entity> it = allEntities.iterator();
		while(it.hasNext()) {
			Entity e = it.next();
			e.owner = null;
			e.systemIndex = -1;
			e.updateIndices(); // will handle removing itself from component tables
			
			it.remove();
		}
		
		// reset tables and counts
		tableCounts = new int[0];
		tables = new ComponentTable[0];
	}
	
	public int size() {
		return allEntities.size();
	}
	
	void incrementCount(ComponentId<?> id) {
		int index = id.getId();
		if (index < tableCounts.length) {
			// just update counter
			tableCounts[index]++;
		} else {
			// must grow table counts
			tableCounts = Arrays.copyOf(tableCounts, index + 1);
			tableCounts[index] = 1;
		}
	}
	
	void decrementCount(ComponentId<?> id) {
		int index = id.getId();
		if (index < tableCounts.length)
			tableCounts[index]--;
		// else it should be 0 anyway
	}
	
	ComponentTable lookupTable(int typeId) {
		if (typeId >=0 && typeId < tables.length)
			return tables[typeId];
		return null;
	}
	
	/* Iterator that iterates over all entities in the system */
	private class EntitySystemIterator implements Iterator<Entity> {
		private final Iterator<Entity> iterator;
		private Entity current;
		
		public EntitySystemIterator() {
			iterator = allEntities.iterator();
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
			if (current == null)
				throw new IllegalStateException("Must call next() first");
			
			// this will remove current from allEntities, which a Bag iterator
			// can handle
			EntitySystem.this.remove(current);
			current = null;
		}
	}
	
	/* Iterator that iterates over all entities, but only returns those that
	 * match a certain type. */
	private class ComponentViewIterator implements Iterator<Entity> {
		private final Iterator<Entity> iterator;
		private final ComponentId<?> componentType;
		
		private Entity toReturn;
		private Entity current;
		
		public ComponentViewIterator(ComponentId<?> type) {
			iterator = allEntities.iterator();
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
			if (current == null)
				throw new IllegalStateException("Must call next() first");
			current.remove(componentType);
			current = null;
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
}
