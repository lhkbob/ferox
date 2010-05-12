package com.ferox.entity;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ComponentIndex is a utility class that maintains a linked-list of
 * ComponentAttachments (and manages said attachment's previous and next fields
 * as needed) that allows the EntitySystem to return Iterators over only
 * Entities that have specific Component types. The Iterator returned by
 * {@link #iterator()} will remove the Component corresponding to the component
 * type of the index when {@link Iterator#remove()} is invoked. The returned
 * Iterator implementation is also thread safe as required for use with
 * {@link EntitySystem#iterator(ComponentId)}.
 * 
 * @author Michael Ludwig
 */
class ComponentIndex implements Iterable<Entity> {
    private final ReadWriteLock lock;
    
    private ComponentAttachment head;
    private ComponentAttachment tail;
    
    /**
     * Create a new ComponentIndex that initially contains no
     * ComponentAttachments.
     */
    public ComponentIndex() {
        lock = new ReentrantReadWriteLock();
        head = null;
        tail = null;
    }
    
    @Override
    public Iterator<Entity> iterator() {
        return new IndexIterator();
    }

    /**
     * <p>
     * Notify the ComponentIndex that the given ComponentAttachment has either
     * been an added to Entity that is already owned by this index's
     * EntitySystem, or an Entity with the attachment has just been added to the
     * index's system. This will update <tt>added</tt>'s previous and next
     * fields to correctly position the attachment within the index's
     * linked-list like index structure.
     * </p>
     * <p>
     * It is the responsibility of ComponentAttachment to call this when
     * appropriate. Entity and EntitySystem should not need to invoke these
     * directly.
     * </p>
     * 
     * @param added The ComponentAttachment that must be added to the index
     */
    public void notifyComponentAdd(ComponentAttachment added) {
        synchronized(lock) {
            added.previous = tail;
            if (tail != null)
                tail.next = added;
            else
                head = added;
            tail = added;
        }
    }

    /**
     * <p>
     * Notify the ComponentIndex that the given ComponentAttachment has either
     * been removed from an Entity that is already owned by this index's
     * EntitySystem, or an Entity with the attachment has been removed from the
     * index's system. This will update <tt>removed</tt>'s previous and next
     * linked attachments to exclude <tt>removed</tt> from the index list.
     * </p>
     * <p>
     * It is the responsibility of ComponentAttachment to call this when
     * appropriate. Entity and EntitySystem should not need to invoke these
     * directly.
     * </p>
     * 
     * @param added The ComponentAttachment that must be removed from the index
     */
    public void notifyComponentRemove(ComponentAttachment removed) {
        synchronized(lock) {
            if (removed.previous != null)
                removed.previous.next = removed.next;
            else
                head = removed.next;
            
            if (removed.next != null)
                removed.next.previous = removed.previous;
            else
                tail = removed.previous;
        }
    }
    
    private class IndexIterator implements Iterator<Entity> {
        private ComponentAttachment current;
        private ComponentAttachment next;
        
        public IndexIterator() {
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
            return current.getOwner();
        }

        @Override
        public void remove() {
            if (current == null)
                throw new IllegalStateException("Must call next() first");
            if (current.getOwner() == null)
                throw new IllegalStateException("Entity already removed");
            
            current.getOwner().remove(current.getComponent());
        }
        
        private boolean advance() {
            if (current == null)
                next = head;
            else
                next = current.next;
            
            while(next != null && next.getOwner() == null)
                next = next.next;
            return next != null;
        }
    }
}
