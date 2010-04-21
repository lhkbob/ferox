package com.ferox.util.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Entity implements Iterable<Component> {
	EntitySystem owner;
	int systemIndex; // index into owner.allEntities
	final ReadWriteLock lock; // FIXME: remove this, I'd prefer a spin lock ... or no synchronization but that's risky
	
	private final List<EntityListener> listeners; // FIXME: actually invoke these appropriately
	private EntityComponentLink[] components;

	/**
	 * Create an Entity that has no attached Components and is not assigned to
	 * an {@link EntitySystem}.
	 */
	public Entity() {
		this((Component[]) null);
	}

	/**
	 * Create an Entity that has the given Components attached to it, but is not
	 * assigned to an {@link EntitySystem}. If there are multiple Components
	 * that report the same ComponentId, it is undefined which Component will be
	 * used.
	 * 
	 * @param comps Var-args containing a list of Components to add to the
	 *            Entity
	 * @throws NullPointerException if any element of <tt>comps</tt> is null
	 */
	public Entity(Component... comps) {
		owner = null;
		systemIndex = -1;
		components = new EntityComponentLink[4];
		listeners = new ArrayList<EntityListener>();
		lock = new ReentrantReadWriteLock();
		
		if (comps != null) {
			for (int i = 0; i < comps.length; i++)
				add(comps[i]);
		}
	}

	/**
	 * Add the given EntityListener to this Entity. Future changes to the state
	 * of this Entity will invoke the appropriate listener function on
	 * <tt>listener</tt>. If the listener has already been added to this Entity,
	 * this will do nothing.
	 * 
	 * @param listener The EntityListener to listen upon this Entity
	 * @throws NullPointerException if listener is null
	 */
	public void addListener(EntityListener listener) {
		if (listener == null)
			throw new NullPointerException("EntityListener cannot be null");
		
		try {
			lock.writeLock().lock();
			if (!listeners.contains(listener))
				listeners.remove(listener);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Remove the given EntityListener from this Entity. Future changes to the
	 * state of this Entity will not invoke the appropriate listener function on
	 * <tt>listener</tt>.  If the listener has not been added to this Entity, then
	 * this call do nothing.
	 * 
	 * @param listener The EntityListener to remove
	 * @throws NullPointerException if listener is null
	 */
	public void removeListener(EntityListener listener) {
		if (listener == null)
			throw new NullPointerException("EntityListener cannot be null");
		
		try {
			lock.writeLock().lock();
			listeners.remove(listener);
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	/**
	 * Get the EntitySystem owner of this Entity. If null is returned, the
	 * Entity is not a member of any EntitySystem. An Entity becomes owned by
	 * invoking {@link EntitySystem#add(Entity)} on the EntitySystem that will
	 * become the owner. After an Entity is added to a system, it will be
	 * included in the Entities returned by {@link EntitySystem#iterator()}.
	 * 
	 * @return The EntitySystem that owns this Entity, or null
	 */
	public EntitySystem getOwner() {
		return owner;
	}

	/**
	 * Attach the given Component, <tt>c</tt> to this Entity. This will replace
	 * any previously attached Component of the same type. However, any
	 * meta-components that were associated with the previous instance are
	 * always removed. The following will always be true:
	 * 
	 * <pre>
	 * entity.add(c).get(c.getComponentId()) == c
	 * </pre>
	 * 
	 * If the Entity has an owner, the Entity will be included in the iterators
	 * returned from {@link EntitySystem#iterator(ComponentId)} if <tt>c</tt>'s
	 * ComponentId is used.
	 * 
	 * @param c The Component to add
	 * @return This Entity to facilitate method-chaining.
	 * @throws NullPointerException if c is null
	 */
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

	/**
	 * <p>
	 * Attach the Component, <tt>meta</tt>, to this Entity as a meta-Component
	 * associated with <tt>attach</tt>. A meta-Component is not considered
	 * attached to the Entity when {@link #get(ComponentId)} or
	 * {@link EntitySystem#iterator(ComponentId)} is invoked. A Component in an
	 * Entity can have multiple meta-Components associated with it, but only one
	 * meta-Component for a given type. This functions identically to how an
	 * Entity can have multiple Components, but only one for each type.
	 * </p>
	 * <p>
	 * A meta-Component's lifetime is limited to the lifetime of the Component
	 * it's associated with. When <tt>attach</tt> is removed from the Entity,
	 * all meta-Components on <tt>attach</tt> are removed as well. Because of
	 * this, if <tt>attach</tt> is not attached to this Entity at the time
	 * {@link #addMeta(Component, Component)} is called, the meta-Component will
	 * not be assigned. True is returned when <tt>meta</tt> is correctly
	 * associated and false when <tt>attach</tt> is not attached to this Entity.
	 * </p>
	 * 
	 * @param <T> The Component type of the meta-Component
	 * @param attach The Component in this Entity that the meta-Component is
	 *            associated to
	 * @param meta The meta-Component that will be stored on <tt>attach</tt> for
	 *            this Entity
	 * @return True if the meta-Component is successfully added
	 * @throws NullPointerException if attach or meta are null
	 */
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

	/**
	 * Get the Component instance attached to this Entity with the given
	 * ComponentId type. If this Entity has no Component of the given type, then
	 * null is returned. When determining if an Entity has the given Component,
	 * only Components that are added via {@link #add(Component)} are
	 * considered. Meta-Components are not included, they can only be retrieved
	 * via {@link #getMeta(Component, ComponentId)}.
	 * 
	 * @param <T> The Component type to look-up
	 * @param id The ComponentId associated with T
	 * @return The Component of type T that's been attached to this Entity
	 * @throws NullPointerException if id is null
	 */
	@SuppressWarnings("unchecked")
	public <T extends Component> T get(ComponentId<T> id) {
		if (id == null)
			throw new NullPointerException("Id cannot be null");
		
		int index = id.getId();
		if (index < components.length && components[index] != null)
			return (T) components[index].getComponent();
		
		return null;
	}

	/**
	 * Get the meta-Component instance that's associated with <tt>attach</tt>
	 * and has the specified id (<tt>meta</tt>). If <tt>attach</tt> is not a
	 * member of this Entity anymore, or if it has not had any meta-Component of
	 * the given type associated with it, null is returned. A non-null Component
	 * is returned only if {@link #addMeta(Component, Component)} has been
	 * previously invoked with <tt>attach</tt> and a meta-Component with the
	 * correct id, <b>and</b> <tt>attach</tt> has not be removed from this
	 * Entity.
	 * 
	 * @param <T> The Component type of the meta-Component to look-up
	 * @param attach The Component which specifies the set of meta-Components to
	 *            search within
	 * @param meta The ComponentId of the type for the requested meta-Component
	 * @return The meta-Component of type T that's been associated with
	 *         <tt>attach</tt>
	 * @throws NullPointerException if attach or meta are null
	 */
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

	/**
	 * Remove and return the Component with the given id type. Null is returned
	 * if this Entity has no Component of the given type (i.e. get(id) returns
	 * null). After being removed, subsequent calls to get(id) will return null
	 * and the Entity will no longer be reported in iterators from
	 * {@link EntitySystem#iterator(ComponentId)} using <tt>id</tt>.
	 * Additionally, any meta-Components assigned to returned Component will be
	 * discarded. Even if the Component is re-added, the meta-Components will be
	 * lost.
	 * 
	 * @param <T> The Component type that is to be removed
	 * @param id The ComponentId corresponding to type T
	 * @return The Component of type T that was previously attached to this
	 *         Entity, or null if no such Component existed
	 * @throws NullPointerException if id is null
	 */
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

	/**
	 * Remove the specific Component from this Entity. This functions
	 * identically to {@link #remove(ComponentId)} using <tt>c</tt>'s
	 * ComponentId, except that it is only removed if <tt>c</tt> is currently
	 * attached to this Entity. This returns true when <tt>c</tt> and any
	 * meta-Components associated with it are removed, and false when <tt>c</tt>
	 * was not removed because it was not attached to this Entity.
	 * 
	 * @param c The Component to remove
	 * @return True if <tt>c</tt> was removed
	 * @throws NullPointerException if c is null
	 */
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

	/**
	 * Remove and return the meta-Component with id, <tt>meta</tt>, that's
	 * associated with <tt>attach</tt>. After a call to this method,
	 * {@link #getMeta(Component, ComponentId)} using <tt>attach</tt> and
	 * <tt>meta</tt> will return null. If <tt>attach</tt> is not currently
	 * attached to this Entity, or if <tt>attach</tt> has no meta-Component of
	 * the given id type.
	 * 
	 * @param <T> The Component type of the meta-Component to remove
	 * @param attach The Component whose meta-Component will be removed
	 * @param meta The ComponentId matching type T
	 * @return The meta-Component of type T that was previously associated with
	 *         <tt>attach</tt>, or null if <tt>attach</tt> was not attached to
	 *         this Entity or had no meta-Component of type T
	 * @throws NullPointerException if attach or meta are null
	 */
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
	
	/**
	 * @return An Iterator over the Components currently attached to this Entity.
	 */
	@Override
	public Iterator<Component> iterator() {
		return new EntityIterator();
	}

	/**
	 * Properly invoke {@link EntityComponentLink#setTable(ComponentTable)} on
	 * the Components attached to this Entity based on the ComponentTables of
	 * the Entity's currently assigned owner.
	 */
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

	/**
	 * Invoke {@link EntityComponentLink#setTable(ComponentTable)} on the link
	 * with the given type id, if this Entity has that Component type attached.
	 * <tt>typeId</tt> refers to an integer returned from
	 * {@link ComponentId#getId()}. <tt>table</tt> is the table instance
	 * assigned to the link, it is assumed that the ComponentTable is of the
	 * appropriate Component type.
	 * 
	 * @param typeId The type id of the Component and corresponding table
	 * @param table The ComponentTable that is assigned to the link, if a link
	 *            exists
	 */
	void updateIndex(int typeId, ComponentTable table) {
		if (typeId < components.length && components[typeId] != null)
			components[typeId].setTable(table);
	}
	
	/*
	 * Internal Iterator over the Components of the Entity.
	 */
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
