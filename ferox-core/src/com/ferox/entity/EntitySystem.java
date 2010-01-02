package com.ferox.entity;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class EntitySystem {
	private final Object entityLock = new Object();
	private int entityIdSeq;
	
	private ComponentTable[] componentTables;
	private final EntityList allList;
	
	public EntitySystem() {
		entityIdSeq = 0;
		
		componentTables = new ComponentTable[8];
		allList = new EntityList();
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
			return new ComponentTableIterator(componentTables[componentId]);
	}
	
	public Entity newEntity() {
		Entity e;
		synchronized(entityLock) {
			int id = entityIdSeq++;
			e = new Entity(this, id);
		}
		
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

		synchronized(entityLock) {
			int type = c.getTypeId();
			if (type >= componentTables.length) {
				// need a new component table
				table = new ComponentTable(type);
				componentTables = Arrays.copyOf(componentTables, type + 1);
				componentTables[type] = table;
			} else 
				table = componentTables[type];
		}
		
		table.list.add(entity);
	}
	
	void detach(Entity entity, Component c) {
		int type = c.getTypeId();
		if (type > 0 && type < componentTables.length && componentTables[type] != null)
			componentTables[type].list.remove(entity);
	}
	
	/* Iterator that iterates over all entities in the system */
	private class EntitySystemIterator implements Iterator<Entity> {
		private final EntityListIterator iterator;
		private Entity current;
		
		public EntitySystemIterator() {
			iterator = new EntityListIterator(allList);
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
	
	/* Iterator that iterates over the entities within a ComponentTable */
	private static class ComponentTableIterator implements Iterator<Entity> {
		private final int typeId;
		private final EntityListIterator iterator;
		private Entity current;
		
		public ComponentTableIterator(ComponentTable table) {
			typeId = table.typeId;
			iterator = new EntityListIterator(table.list);
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
	
	/* Iterator that has no elements */
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
	
	/* Iterator that iterates over the contents of an EntityList */
	private static class EntityListIterator implements Iterator<Entity> {
		final EntityList list;
		int modCount;
		
		EntityNode currentNode;
		boolean removed;
		
		public EntityListIterator(EntityList list) {
			this.list = list;
			
			modCount = list.modCount;
			currentNode = null;
			
			removed = false;
		}
		
		@Override
		public boolean hasNext() {
			if (modCount != list.modCount)
				throw new ConcurrentModificationException();
			
			return (currentNode == null ? list.head != null : currentNode.next != null);
		}

		@Override
		public Entity next() {
			if (!hasNext())
				throw new NoSuchElementException();
			
			EntityNode n = (currentNode == null ? list.head : currentNode.next);
			
			if (n == null) {
				// must have had some modification that screwed us up
				throw new ConcurrentModificationException();
			} else {
				// advance to the node, n
				currentNode = n;
				removed = false;
				return currentNode.entity;
			}
		}

		@Override
		public void remove() {
			if (currentNode == null)
				throw new IllegalStateException("next() must be called first");
			if (removed)
				throw new IllegalStateException("remove() already called for this element");
			if (modCount != list.modCount)
				throw new ConcurrentModificationException();
			
			modCount = list.remove(currentNode);
			removed = true;
		}
	}
	
	private static class ComponentTable {
		final int typeId;
		final EntityList list;
		
		public ComponentTable(int typeId) {
			this.typeId = typeId;
			list = new EntityList();
		}
	}
	
	private static class EntityList {
		EntityNode head;
		EntityNode tail;
		
		volatile int modCount;
		
		public EntityList() {
			head = null;
			tail = null;
			
			modCount = 0;
		}
		
		public synchronized void add(Entity entity) {
			modCount++;

			if (head == null) {
				head = new EntityNode(entity, null, null);
				tail = head;
			} else {
				// iterate from the end to the beginning
				EntityNode c = tail;
				while(c != null && c.entity.getId() > entity.getId()) {
					c = c.prev;
				}
				
				if (c == null) {
					// new head
					head = new EntityNode(entity, null, head);
				} else {
					// insert new node
					new EntityNode(entity, c, c.next);
				}
			}
		}
		
		public synchronized void remove(Entity e) {
			int id = e.getId();
			EntityNode c = head;
			while(c != null && c.entity.getId() < id) {
				c = c.next;
			}
			
			if (c != null && c.entity.getId() == id)
				remove(c);
		}			
		
		public synchronized int remove(EntityNode node) {
			modCount++;
			
			if (node.prev != null)
				node.prev.next = node.next;
			else
				head = node.next;
			
			if (node.next != null)
				node.next.prev = node.prev;
			else
				tail = node.prev;
			return modCount;
		}
	}
	
	private static class EntityNode {
		EntityNode prev;
		EntityNode next;
		
		Entity entity;
		
		// modifies p and n to point to this node, too
		public EntityNode(Entity entity, EntityNode p, EntityNode n) {
			this.entity = entity;
			
			prev = p;
			next = n;
			
			if (prev != null)
				prev.next = this;
			if (next != null)
				next.prev = this;
		}
	}
}
