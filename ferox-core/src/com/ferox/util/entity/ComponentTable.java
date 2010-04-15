package com.ferox.util.entity;

import java.util.Iterator;

import com.ferox.util.Bag;

/**
 * <p>
 * ComponentTable logically represents a collection of Entities that all contain
 * a Component of the same type. This allows for iteration over specific
 * Component types without having to traverse the entire EntitySystem.
 * </p>
 * <p>
 * Because of the complexities involved, such as when an index is requested on
 * an EntitySystem, or when a Component is added or removed from an Entity, the
 * links within the ComponentTable should not be modified directly. Instead, all
 * necessary logic is contained within
 * {@link EntityComponentLink#setComponent(Component)} and
 * {@link EntityComponentLink#setTable(ComponentTable)}.
 * </p>
 * 
 * @author Michael Ludwig
 */
final class ComponentTable implements Iterable<Entity> {
	/**
	 * Index stores the index of an EntityComponentLink within a ComponentTable
	 * to provide constant time removals.
	 */
	public static class Index {
		private final EntityComponentLink link;
		private final ComponentTable table;

		private int elementIndex;

		/*
		 * Create an Index between the given table and link. The created Index
		 * must still be assigned to link.
		 */
		private Index(ComponentTable table, EntityComponentLink link) {
			this.table = table;
			this.link = link;
			
			table.entities.add(this);
			elementIndex = table.entities.size() - 1;
		}
		
		/**
		 * Remove this Index's associated link from the Index's owning table.
		 * This manages 
		 */
		public void clearIndex() {
			table.entities.remove(elementIndex);
			if (table.entities.size() != elementIndex)
				table.entities.get(elementIndex).elementIndex = elementIndex;
			elementIndex = -1;
			link.setIndex(null);
		}
	}
	
	private final Bag<Index> entities;

	/**
	 * Create a new ComponentTable that is initially empty. The Component type
	 * of a ComponentTable is implicitly defined by the types of the links added
	 * to it. It is the Entity and EntitySystem's responsibility to ensure the
	 * the validity of the table.
	 */
	public ComponentTable() {
		entities = new Bag<Index>();
	}

	/**
	 * <p>
	 * Assign the given EntityComponentLink Index data that ties the link to
	 * this ComponentTable. Any previous index data is broken via
	 * {@link Index#clearIndex()} before assigning the new index data. This will
	 * invoke {@link EntityComponentLink#setIndex(Index)} on <tt>link</tt>
	 * </p>
	 * <p>
	 * After being assigned an index, the given link's Entity will appear within
	 * the results of this table's Iterators. It is assumed that the Component
	 * type of <tt>link</tt> matches the type of this table.
	 * </p>
	 * 
	 * @param link The EntityComponentLink that will be indexed into this table
	 */
	public void assignIndex(EntityComponentLink link) {
		if (link.getIndex() != null) {
			if (link.getIndex().table == this)
				return; // don't re-add the link
			link.getIndex().clearIndex();
		}
		
		// create and assign the new index
		link.setIndex(new Index(this, link));
	}

	/**
	 * Return an Iterator over all Entities within this table's owning
	 * EntitySystem that have a Component of a matching type. When the Entity is
	 * removed from the Iterator, it has the effect of removing the Component
	 * from the Entity (e.g. it matches the behavior expected for the Iterator
	 * returned from {@link EntitySystem#iterator(ComponentId)}.
	 * 
	 * @return An iterator over this tables Entities
	 */
	@Override
	public Iterator<Entity> iterator() {
		return new ComponentTableIterator();
	}

	/*
	 * Iterator over the ComponentTable's links, that will return Entities so
	 * that it is useful for the EntitySystem. When remove() is invoked, it
	 * removes the Component from the Entity that is associated with this
	 * ComponentTable type.
	 */
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
