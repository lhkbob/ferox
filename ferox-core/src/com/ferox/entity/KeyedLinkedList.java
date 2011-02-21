package com.ferox.entity;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * KeyedLinkedList is a specialized collection that is a linked list structure.
 * However, when items are added, special keys are returned that must be used to
 * remove their associated item. This alleviates linked lists' need for scanning
 * through the list in order to remove it.
 * </p>
 * <p>
 * The KeyedLinkedList's primary purpose is for storage and iteration so it does
 * not provide random access. This allows its storage to be extremely light
 * weight and to be thread safe without requiring any synchronization. Its
 * iterators support concurrent modifications to the underlying list, including
 * removal. The iterators will not show items added after the iterator was
 * created, although they will respect removals.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The item type
 */
class KeyedLinkedList<T> implements Iterable<T> {
    /**
     * Key represents a tie from a value back to its node or link within the
     * linked list. This is used to perform very fast removals of elements.
     */
    public static class Key<T> {
        private final AtomicReference<T> value;
        private volatile Key<T> next;
        
        public Key(T value) {
            this.value = new AtomicReference<T>(value);
            next = null;
        }
    }
    
    private static final float PURGE_FRACTION = .3f;
    
    private final AtomicReference<Key<T>> head;
    
    private final AtomicBoolean purgeLock;
    private final AtomicInteger size;
    private final AtomicInteger invalidKeys;
    
    /**
     * Create an empty KeyedLinkedList.
     */
    public KeyedLinkedList() {
        head = new AtomicReference<Key<T>>();
        size = new AtomicInteger(0);
        invalidKeys = new AtomicInteger(0);
        purgeLock = new AtomicBoolean();
    }
    
    public int size() {
        return size.get();
    }

    /**
     * Append the given value to the front of the list. A Key is returned, which
     * must be used to remove the value from the list. This is an O(1)
     * operation, although it may block for an unknown amount of time when the
     * list is in high contention (this should never be the case though).
     * 
     * @param value The value to add
     * @return The Key associated with the value's place in the list
     * @throws NullPointerException if value is null
     */
    public Key<T> add(T value) {
        if (value == null)
            throw new NullPointerException("Value cannot be null");
        Key<T> newKey = new Key<T>(value);
        
        while(true) {
            // Grab the old head reference so that we can update the next pointer
            // of the new head, and make sure that there aren't two adds going
            // on at the same time
            Key<T> oldHead = head.get();
            
            // Set the next pointer before trying to update the head, so that if
            // it is successful the new head is fully formed.  If we waited until
            // after the set went through, there would be a brief moment where
            // a thread might just see the null reference.
            // - And if the set fails, the next iteration will update this key
            //   to correctly fit the list
            newKey.next = oldHead;

            // Only assign to the head if nothing has changed since we started
            if (head.compareAndSet(oldHead, newKey)) {
                size.incrementAndGet();
                return newKey;
            }
        }
    }

    /**
     * Remove the given Key from the list. This invalidates the Key so that it
     * cannot be used anymore. Removing an invalid key does nothing. Keys are
     * not immediately removed from the list structure but after enough removes
     * occur, the list is scanned and purged of as many invalid keys as
     * possible. This means that many removes are very fast, even in a contested
     * environment because it simply sets the value reference. However, some
     * removes() will require an O(n) scan on the list. It is expected that the
     * amortized cost of removal is O(1).
     * 
     * @param key The key to remove
     * @return The value stored in the key, or null if the key was previously
     *         invalidated
     */
    public T remove(Key<T> key) {
        // The remove method is only responsible for invalidating the key.
        // Invalid keys are not returned by the iterator so for all intents
        // and purposes, it appears to be removed.
        T oldValue = key.value.getAndSet(null);
        
        if (oldValue != null) {
            // This is not an accidental double remove, so update counters
            // There is a chance that the state of the two counters combined is
            // inconsistent since the reads aren't atomic, but it won't drastically
            // affect the purge actions (it might mean one is skipped or done twice)
            int currentSize = size.decrementAndGet();
            int currentInvalidKeys = invalidKeys.incrementAndGet();
            
            if (currentInvalidKeys >= PURGE_FRACTION * currentSize) {
                // There are enough invalid keys that a purge should occur
                purge();
            }
        }
        
        return oldValue;
    }

    /**
     * Clean up some number of invalid keys from the list. Because keys can be
     * invalidated as the purge happens, this does not guarantee that the list
     * is devoid of empty keys.
     */
    private void purge() {
        // Set the purgeLock to true. If it was already true then skip the purge
        // because another thread is already cleaning the list.  This is a cheap
        // way to make sure that purge() is only running on a single thread at 
        // a time.
        // - Because it's run on a single thread at a time, some logic below is
        //   valid even though it would fail if purge() is run multiple times 
        //   concurrently.
        if (!purgeLock.compareAndSet(false, true))
            return;
        
        
        // Start at the current head of the list. Any new keys will be added
        // to the front so they won't be considered in the purge. The head
        // reference is not modified so there's no chance that we'd accidentally
        // clear any new keys from the list by overwriting the head.
        // - This does mean that an "empty" list might still have a number of
        //   invalid keys in the list because an invalid head isn't cleaned up
        Key<T> current = head.get();
        if (current == null)
            return;
        
        // We set lastValid to the current just in case there is a large chain
        // of invalid keys at the beginning. This allows all but the head
        // invalid key to be removed.
        Key<T> lastValid = current;
        current = current.next;
        
        while(current != null) {
            // Most of this loop would be safe even in multiple threads, but
            // it's even more valid now because it's running single threaded.
            // Decrementing invalidKeys is only valid in a single-threaded context.
            
            if (lastValid != null)
                lastValid.next = current;
            if (current.value.get() != null)
                lastValid = current;
            else
                invalidKeys.decrementAndGet();
            current = current.next;
        }
        
        if (lastValid != null) {
            Key<T> lastValidNext = lastValid.next;
            if (lastValidNext != null && lastValidNext.value.get() == null) {
                // The only way the lastValid's next pointer could be not null
                // is if the last keys in the list all were invalid. Because
                // new items are added to the front, and keys can't be revalidated,
                // it is safe to just set lastValid's next pointer to null.
                // - And since the full list has already been walked through, 
                //   the invalidKeys counter doesn't need to be decremented anymore.
                lastValid.next = null;
            }
        }
        
        // Unlock the purge()
        purgeLock.set(false);
    }
    
    @Override
    public KeyIterator iterator() {
        return new KeyIterator();
    }

    /**
     * An internal iterator over the keys in a list. It starts at the current
     * head of the list when it was created, so it will not see any keys added
     * after the iterator was made (because new values are prepended onto the
     * list).
     */
    public class KeyIterator implements Iterator<T> {
        private T nextValue;
        private Key<T> next;
        
        private Key<T> currentKey; // For implementing remove()
        
        public KeyIterator() {
            next = head.get();
            nextValue = (next == null ? null : next.value.get());
            currentKey = null;
            
            if (nextValue == null) {
                // Advance to the first real value, which might not be the head
                advance();
            }
        }

        @Override
        public boolean hasNext() {
            return nextValue != null;
        }

        @Override
        public T next() {
            if (nextValue == null)
                throw new NoSuchElementException();

            T current = nextValue;
            
            // Set the current key to the next key. We treat current/nextValue separately
            // because that won't change and can't be null, but there's a chance
            // that next.value.get() will be set to null before this returns.
            currentKey = next;
            
            advance();
            return current;
        }

        @Override
        public void remove() {
            attemptRemove();
        }
        
        public T attemptRemove() {
            if (currentKey == null)
                throw new IllegalStateException("Must call next() first");
            
            // Don't fail if the key was already invalid, because that means that an iterator
            // could fail due to another thread's actions. Instead we let remove(key) properly
            // handle double-removes, which turns into a no-op.
            T removed = KeyedLinkedList.this.remove(currentKey);
            currentKey = null;
            return removed;
        }
        
        private void advance() {
            if (next == null)
                return; // There are no nodes left
            
            do {
                // Loop through the chain, until we walk off the end of the chain,
                // or find a key that is valid.
                next = next.next;
                nextValue = (next == null ? null : next.value.get());
            } while(nextValue == null && next != null);
        }
    }
}
