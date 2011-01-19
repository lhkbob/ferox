package com.ferox.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
 * parallel.
 * </p>
 * 
 * @see Entity
 * @see Component
 * @see Controller
 * @author Michael Ludwig
 */
public class EntitySystem implements Iterable<Entity> {
    private final Object systemLock;
    private ComponentIndex[] indices;
    
    private final List<SystemListener> listeners;
    
    private EntityNode head;
    private EntityNode tail;
    
    /**
     * Create an initially empty EntitySystem.
     */
    public EntitySystem() {
        listeners = new ArrayList<SystemListener>();
        
        systemLock = new Object();
        indices = new ComponentIndex[0];
        
        head = null;
        tail = null;
    }

    /**
     * <p>
     * Add the given Entity to this EntitySystem. Iterators returned by
     * {@link #iterator()} and {@link #iterator(ComponentId)} will now include
     * <tt>e</tt> in the Entities that can be returned. For the
     * Component-specific iterator, this is dependent on the Components attached
     * to e. An Entity can only be added if it does not have an owning
     * EntitySystem already. If the Entity is owned by another EntitySystem an
     * exception is thrown (nothing occurs if the Entity is re-added to its
     * owner).
     * </p>
     * <p>
     * Any SystemListeners on this system will be notified. Any EntityListeners
     * on the Entity will also be notified.
     * </p>
     * 
     * @param e The Entity to add to this EntitySystem
     * @throws NullPointerException if e is null
     * @throws IllegalArgumentException if e is owned by an EntitySystem that is
     *             not this system
     */
    public void add(Entity e) {
        if (e == null)
            throw new NullPointerException("Entity cannot be null");
        EntitySystem owner = e.getSystem();
        if (owner != null && owner != this)
            throw new IllegalArgumentException("Entity owned by a different EntitySystem");
        
        if (owner == this)
            return; // don't re-add the entity to its current owner
        
        synchronized(systemLock) {
            for (int i = listeners.size() - 1; i >= 0; i--)
                listeners.get(i).onEntityAdd(this, e);
            
            EntityNode node = new EntityNode();
            node.entity = e;
            
            // we notify now in case any of e's listeners throw
            // an exception so that the entire add fails
            e.notifySystemAdd(this, node);
            
            node.previous = tail;
            if (tail != null)
                tail.next = node;
            else
                head = node;
            tail = node;
        }
    }

    /**
     * <p>
     * Remove the given Entity from this EntitySystem so that it will not be
     * included in the results of this system's various Iterators. An exception
     * is thrown if the given Entity is not currently owned by this system.
     * </p>
     * <p>
     * Any SystemListeners on this system will be notified. Any EntityListeners
     * on the Entity will also be notified.
     * </p>
     * 
     * @param e The Entity that's to be removed
     * @throws NullPointerException if e is null
     * @throws IllegalArgumentException if e is not owned by this EntitySystem
     */
    public void remove(Entity e) {
        if (e == null)
            throw new NullPointerException("Entity cannot be null");
        if (e.getSystem() != this)
            throw new IllegalArgumentException("Entity is not owned by this EntitySystem");
        
        synchronized(systemLock) {
            for (int i = listeners.size() - 1; i >= 0; i--)
                listeners.get(i).onEntityRemove(this, e);

            EntityNode node = e.getNode();
            // we notify here before actually removing it in
            // case e's listeners throw an exception
            e.notifySystemRemove();

            node.entity = null;
            
            if (node.previous != null)
                node.previous.next = node.next;
            else
                head = node.next;
            
            if (node.next != null)
                node.next.previous = node.previous;
            else
                tail = node.previous;
        }
    }

    /**
     * Add the given SystemListener to this EntitySystem. Future changes to the
     * state of this EntitySystem will invoke the appropriate listener function
     * on <tt>listener</tt>. If the listener has already been added to this
     * EntitySystem, this will do nothing.
     * 
     * @param listener The SystemListener to listen upon this EntitySystem
     * @throws NullPointerException if listener is null
     */
    public void addListener(SystemListener listener) {
        if (listener == null)
            throw new NullPointerException("Listener cannot be null");
        synchronized(systemLock) {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    /**
     * Remove the given SystemListener from this EntitySystem. Future changes to
     * the state of this EntitySystem will not invoke the appropriate listener
     * function on <tt>listener</tt>. If the listener has not been added to this
     * system, then this call will do nothing.
     * 
     * @param listener The SystemListener to remove
     * @throws NullPointerException if listener is null
     */
    public void removeListener(SystemListener listener) {
        if (listener == null)
            throw new NullPointerException("Listener cannot be null");
        synchronized(systemLock) {
            listeners.remove(listener);
        }
    }

    /**
     * Return an Iterator over the Entities of this EntitySystem. The returned
     * Iterator supports concurrent modifications, although multi-threaded
     * changes may not be visible right away. The returned Iterator supports the
     * {@link Iterator#remove()} method, and invoking it performs the same
     * operations as {@link #remove(Entity)}.
     * 
     * @return An Iterator over all Entities that have been added to this
     *         EntitySystem
     */
    @Override
    public Iterator<Entity> iterator() {
        return new EntityIterator();
    }

    /**
     * <p>
     * Return an Iterator over all Entities of this EntitySystem that have a
     * Component attached with the given <tt>id</tt>. The returned Iterator
     * supports concurrent modifications, although modifications here refers to
     * adding or removing relevant Components from the system's Entities. Like
     * the Iterator returned from {@link #iterator()}, multi-threaded changes
     * may not be visible immediately. Because of this, it's possible for an
     * Entity to be returned from {@link Iterator#next()} that had the desired
     * Component type, but had the Component removed before the iteration
     * continued to process it.
     * </p>
     * <p>
     * The returned Iterator supports {@link Iterator#remove()} and invoking it
     * performs the same operations as {@link #remove(Entity)}. This is to
     * remain consistent with the specification of Iterator. It is perfectly
     * allowable to remove any iterated Entity's Component during the iteration
     * without corrupting the iteration results.
     * </p>
     * 
     * @param id The ComponentId of the Component type that all returned
     *            Entities are required to have.
     * @return An Iterator over the Entities that have Components of the desired
     *         type
     */
    public Iterator<Entity> iterator(TypedId<? extends Component> id) {
        if (id == null)
            throw new NullPointerException("ComponentId cannot be null");
        
        int index = id.getId();
        if (index < indices.length && indices[index] != null)
            return indices[index].iterator();
        else
            return new NullIterator();
    }

    /**
     * @param c The Component whose relevant ComponentIndex is returned
     * @return The appropriate ComponentIndex of this system capable of storing
     *         ComponentAttachments for the given Component (and its associated
     *         type)
     */
    ComponentIndex getIndex(Component c) {
        int index = c.getTypedId().getId();
        
        synchronized(systemLock) {
            if (index >= indices.length)
                indices = Arrays.copyOf(indices, index + 1);
            if (indices[index] == null)
                indices[index] = new ComponentIndex();
            
            return indices[index];
        }
    }
    
    private class EntityIterator implements Iterator<Entity> {
        private EntityNode current;
        private EntityNode next;
        
        public EntityIterator() {
            current = null;
            next = null;
        }

        @Override
        public boolean hasNext() {
            return next != null || advance();
        }

        @Override
        public Entity next() {
            if (next == null && !advance())
                throw new NoSuchElementException();
            
            current = next;
            next = null;
            return current.entity;
        }

        @Override
        public void remove() {
            if (current == null)
                throw new IllegalStateException("Must call next() first");
            if (current.entity == null)
                throw new IllegalStateException("Entity already removed");
            
            EntitySystem.this.remove(current.entity);
        }
        
        private boolean advance() {
            if (current == null)
                next = head;
            else
                next = current.next;
            
            while(next != null && next.entity == null)
                next = next.next;
            return next != null;
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
