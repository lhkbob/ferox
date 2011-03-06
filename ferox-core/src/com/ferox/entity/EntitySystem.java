package com.ferox.entity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <p>
 * EntitySystem represents a collection of Entities and their Components that
 * have some logical relation to one another. Of the concepts required for the
 * entity pattern, the system is relatively simple compared with Entity and
 * Component. Some examples of an EntitySystem may be to represent a all of the
 * pieces of a 3D scene.
 * </p>
 * <p>
 * Like the rest of the entity package, EntitySystem is intended to be
 * thread-safe. Controllers are capable of processing a single EntitySystem in
 * parallel. However, to prevent conflicts between multiple controllers (such as
 * one attempting to remove an Entity while another is processing it), it makes
 * more sense to execute multiple controllers in a well defined order.
 * </p>
 * 
 * @see Entity
 * @see Component
 * @see Controller
 * @author Michael Ludwig
 */
public final class EntitySystem implements Iterable<Entity> {
    @SuppressWarnings("rawtypes")
    private volatile KeyedLinkedList[] componentIndices;
    private final Object componentLock;
    
    private final CopyOnWriteArraySet<EntityListener> listeners;

    /**
     * The list of entities within the system. This is exposed only so that
     * Entity can add and remove objects since Entities implements the actions
     * necessary to add or remove themselves from a system.
     */
    final KeyedLinkedList<Entity> entityList;
    
    /**
     * Create a new EntitySystem that initially has no attached Entities or
     * EntityListeners.
     */
    public EntitySystem() {
        componentLock = new Object();
        componentIndices = new KeyedLinkedList[0];
        entityList = new KeyedLinkedList<Entity>();
        
        listeners = new CopyOnWriteArraySet<EntityListener>();
    }

    /**
     * <p>
     * Add an EntityListener to this EntitySystem, so that it will be notified
     * of all added or removed Entities, or changes to the Components on the
     * Entities in the system. This does nothing if the listener is already
     * listening on this system.
     * </p>
     * <p>
     * The added listener will not be notified of any changes that started being
     * reported before this method was called.
     * </p>
     * 
     * @param listener The EntityListener to add
     * @throws NullPointerException if listener is null
     */
    public void addEntityListener(EntityListener listener) {
        if (listener == null)
            throw new NullPointerException("Null EntityListener is not allowed");
        // Don't need any synchronization since listeners is thread safe
        listeners.add(listener);
    }

    /**
     * <p>
     * Remove an EntityListener from this EntitySystem, so that it will be no
     * longer be notified of added or removed Entities, or changes to the
     * Components on the Entities in the system. This does nothing if the
     * listener is not listening on the system.
     * </p>
     * <p>
     * The removed listener may be notified of any changes that occurred during
     * this method call, but will not be notified of changes that occur
     * afterwards.
     * </p>
     * 
     * @param listener The EntityListener to remove
     * @throws NullPointerException if listener is null
     */
    public void removeEntityListener(EntityListener listener) {
        if (listener == null)
            throw new NullPointerException("Null EntityListener is not allowed");
        // Don't need any synchronization since listeners is thread safe
        listeners.remove(listener);
    }
    
    /**
     * <p>
     * Add the given Entity to the EntitySystem. An Entity can only be added if
     * it is not owned by another EntitySystem. Because the entity framework is
     * meant to be thread safe, it is possible that add() may be called on the
     * same Entity with two different systems. In this case, only one system
     * will become the owner.
     * </p>
     * <p>
     * True is returned if the Entity was successfully added to the system. Keep
     * in mind that another Thread could subsequently remove it, although this
     * is up to the application using the system. If an Entity is added to its
     * current owner, true is returned although it is a no-op.
     * </p>
     * 
     * @param e The Entity to add to the system
     * @return True if the Entity was successfully added, or false if the Entity
     *         could not be added because another system owns the entity
     * @throws NullPointerException if e is null
     */
    public boolean add(Entity e) {
        if (e == null)
            throw new NullPointerException("Entity cannot be null");
        // addToEntitySystem() performs all necessary actions, including
        // throwing the proper exceptions
        return e.addToEntitySystem(this);
    }

    /**
     * <p>
     * Remove the given Entity from this EntitySystem. An Entity can only be
     * removed if it is currently owned by the system. Because the entity
     * framework is meant to be thread safe, multiple threads could attempt to
     * remove the same entity at the same time. In this case, only one thread
     * will perform the actual remove operation.
     * </p>
     * <p>
     * True is returned if the Entity is successfully removed from the system.
     * False is returned if the Entity has no owner or is owned by another
     * system. In either case, the Entity is guaranteed to not be in the calling
     * EntitySystem, unless another thread re-adds it after the remove
     * completes.
     * </p>
     * 
     * @param e The Entity to remove from the system
     * @return True if the Entity was in the system and is now removed, or false
     *         if the Entity was not in this EntitySystem
     * @throws NullPointerException if e is null
     */
    public boolean remove(Entity e) {
        if (e == null)
            throw new NullPointerException("Entity cannot be null");
        // removeFromEntitySystem() performs all necessary actions,
        // including throwing the proper exceptions
        return e.removeFromEntitySystem(this);
    }
    
    /**
     * <p>
     * Return an Iterator over all Entities within the EntitySystem. The
     * returned Iterator is thread safe in that it correctly handles concurrent
     * modifications. Entities added after the Iterator is created will not be
     * returned by the Iterator. Entities removed from the system before the
     * Iterator reaches them will not be returned by the Iterator.
     * </p>
     * <p>
     * All operations are supported by the returned Iterator.
     * </p>
     * 
     * @return An iterator over all entities in the system
     */
    @Override
    public Iterator<Entity> iterator() {
        return new EntityIterator();
    }

    /**
     * <p>
     * Return an Iterator over all Components of the given type within the
     * EntitySystem. Every Component provided by the Iterator will be owned by
     * an Entity within this EntitySystem at the time {@link Iterator#next()}
     * was called. The returned Iterator is thread safe in that it correctly
     * handles concurrent modifications.
     * </p>
     * <p>
     * Components of the given type that are added to the system (by being added
     * to an Entity already in the system, or being attached to an Entity added
     * to the system) will not be returned by the iterator. Components of the
     * given type removed from the system (in similar ways to being added)
     * before the iterator reaches them will not be returned by the iterator.
     * </p>
     * <p>
     * All operations are supported by the returned Iterator. However, the
     * remove() of the Iterator removes the Component from its owning Entity. It
     * does not remove the Component's owner from this EntitySystem.
     * </p>
     * 
     * @param <T> The Component type of the returned iterator
     * @param id The TypedId associated with the given Component type
     * @return An Iterator over all Components of type T currently within the
     *         system
     * @throws NullPointerException if id is null
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends Component> Iterator<T> iterator(TypedId<T> id) {
        KeyedLinkedList[] indices = componentIndices;
        if (indices.length <= id.getId() || indices[id.getId()] == null)
            return new ComponentIterator<T>(null);
        else
            return new ComponentIterator<T>(indices[id.getId()]);
    }

    /**
     * Return the KeyedLinkedList holding all Components of the given id type
     * for the EntitySystem. Although the generic type is Component, users of
     * this method must ensure that only Components that match the id type are
     * added to the list. This will never return null.
     * 
     * @param id The Component type
     * @return The list used to track all Components of a certain type currently
     *         within the system
     * @throws NullPointerException if id is null
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    KeyedLinkedList<Component> getComponentIndex(TypedId<? extends Component> id) {
        int index = id.getId();
        
        KeyedLinkedList[] indices = componentIndices;
        if (indices.length <= index || indices[index] == null) {
            synchronized(componentLock) {
                // double check
                if (componentIndices.length <= index)
                    indices = Arrays.copyOf(componentIndices, index + 1);
                if (indices[index] == null)
                    indices[index] = new KeyedLinkedList<Component>();
                
                componentIndices = indices;
                return (KeyedLinkedList<Component>) indices[index];
            }
        } else {
            // component indices is monotonically increasing in length
            // and the volatile read above should make everything valid
            return (KeyedLinkedList<Component>) indices[index];
        }
    }

    /**
     * Run all current EntityListeners' onComponentAdd() methods with the given
     * ComponentEvent. To preserve ordering of events for a single Entity, this
     * should be called within the Entity's lock. Ordering across multiple
     * Entities is not guaranteed.
     * 
     * @param event The ComponentEvent, must not be null
     */
    void notifyComponentAdd(ComponentEvent event) {
        for (EntityListener l: listeners)
            l.onComponentAdd(event);
    }

    /**
     * Run all current EntityListeners' onComponentRemove() methods with the
     * given ComponentEvent. To preserve ordering of events for a single Entity,
     * this should be called within the Entity's lock. Ordering across multiple
     * Entities is not guaranteed.
     * 
     * @param event The ComponentEvent, must not be null
     */
    void notifyComponentRemove(ComponentEvent event) {
        for (EntityListener l: listeners)
            l.onComponentRemove(event);
    }

    /**
     * Run all current EntityListeners' onEntityAdd() methods with the given
     * EntityEvent. To preserve ordering of events for a single Entity, this
     * should be called within the Entity's lock. Ordering across multiple
     * Entities is not guaranteed.
     * 
     * @param event The ComponentEvent, must not be null
     */
    void notifyEntityAdd(EntityEvent event) {
        for (EntityListener l: listeners)
            l.onEntityAdd(event);
    }

    /**
     * Run all current EntityListeners' onEntityAdd() methods with the given
     * EntityEvent. To preserve ordering of events for a single Entity, this
     * should be called within the Entity's lock. Ordering across multiple
     * Entities is not guaranteed.
     * 
     * @param event The ComponentEvent, must not be null
     */
    void notifyEntityRemove(EntityEvent event) {
        for (EntityListener l: listeners)
            l.onEntityRemove(event);
    }

    /**
     * An Iterator over all the entities currently within the system. This wraps
     * a KeyedLInkedList iterator to correctly remove an Entity from the system.
     */
    private class EntityIterator implements Iterator<Entity> {
        private final KeyedLinkedList<Entity>.KeyIterator iterator;
        
        public EntityIterator() {
            iterator = entityList.iterator();
        }
        
        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Entity next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            Entity removed = iterator.attemptRemove();
            if (removed != null) {
                // This operation has a double-remove for the index,
                // but that's okay.  We need to do this anyway to clean up
                // the owner and component indices.
                // - If another thread is also removing the entity, this could
                //   return false but that won't corrupt the system
                removed.removeFromEntitySystem(EntitySystem.this);
            }
        }
    }

    /**
     * An Iterator over all the components of a specific type currently within
     * the system. This wraps a KeyedLinkedList's iterator to correctly remove a
     * Component from its owning Entity.
     */
    private class ComponentIterator<T extends Component> implements Iterator<T> {
        private final KeyedLinkedList<T>.KeyIterator iterator;
        
        public ComponentIterator(KeyedLinkedList<T> list) {
            if (list == null)
                iterator = null;
            else
                iterator = list.iterator();
        }
        
        @Override
        public boolean hasNext() {
            return (iterator == null ? false : iterator.hasNext());
        }

        @Override
        public T next() {
            if (iterator == null)
                throw new NoSuchElementException();
            return iterator.next();
        }

        @Override
        public void remove() {
            T removed = iterator.attemptRemove();
            if (removed != null) {
                // By using removeIfOwned(), we are guaranteed to only remove the
                // Component if the Entity is still in the system, so we won't remove()
                // something outside of the system's control.
                // - If the entity is removed at this time, there will be a double-remove
                //   of the index key of the Component (either by the iterator or when
                //   the entity is detached), but this won't corrupt anything because
                //   double-removes are permissible and the iterator won't remove any new key
                ComponentContainer owner = removed.getOwner();
                if (owner instanceof Entity)
                    ((Entity) owner).removeIfOwned(removed, EntitySystem.this);
            }
        }
    }
}
