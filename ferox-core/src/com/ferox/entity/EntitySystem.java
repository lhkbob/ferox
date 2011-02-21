package com.ferox.entity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
 * parallel. However, to prevent conflicts betwen multiple controllers (such as
 * one attempting to remove an Entity while another is processing it), it makes
 * more sense to execute multiple controllers in a well defined order.
 * </p>
 * 
 * @see Entity
 * @see Component
 * @see Controller
 * @author Michael Ludwig
 */
public class EntitySystem implements Iterable<Entity> {
    @SuppressWarnings("rawtypes")
    private volatile KeyedLinkedList[] componentIndices;
    private final Object componentLock;

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
    
    // FIXME: what about adding controllers now, NO we need to add/remove EntityListeners
    // since not all Controllers need to be notified of changes to the system.

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
