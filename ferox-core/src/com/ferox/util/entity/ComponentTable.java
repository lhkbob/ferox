package com.ferox.util.entity;

import java.util.Iterator;

import com.ferox.util.Bag;

final class ComponentTable implements Iterable<Entity> {
	public static class Index {
		private final EntityComponentLink link;
		private final ComponentTable table;

		private int elementIndex;
		
		private Index(ComponentTable table, EntityComponentLink link) {
			this.table = table;
			this.link = link;
		}
		
		public void clearIndex() {
			table.entities.remove(elementIndex);
			if (table.entities.size() != elementIndex)
				table.entities.get(elementIndex).elementIndex = elementIndex;
			elementIndex = -1;
			link.setIndex(null);
		}
	}
	
	private final Bag<Index> entities;
	
	public ComponentTable() {
		entities = new Bag<Index>();
	}
	
	public void add(EntityComponentLink link) {
		if (link.getIndex() != null) {
			if (link.getIndex().table == this)
				return; // don't re-add the link
			link.getIndex().clearIndex();
		}
		
		// create the new index
		Index index = new Index(this, link);
		entities.add(index);
		index.elementIndex = entities.size() - 1;
		
		// assign the index
		link.setIndex(index);
	}

	@Override
	public Iterator<Entity> iterator() {
		return new ComponentTableIterator();
	}
	
	private class ComponentTableIterator implements Iterator<Entity> {
		private final Iterator<Index> iterator;
		private Index current;
		
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
			return current.link.getEntity();
		}

		@Override
		public void remove() {
			if (current == null)
				throw new IllegalStateException("next() must be called first");
			if (current.elementIndex < 0)
				throw new IllegalStateException("remove() has already been called");
			
			// clear the link's component, which will break the index
			// and remove current (and also takes care of the swapping)
			current.link.setComponent(null);
		}
	}
}
