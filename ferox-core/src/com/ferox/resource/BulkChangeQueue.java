package com.ferox.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * BulkChangeQueue is an implementation of a fixed-length FIFO queue,
 * representing a large queue of changes to a {@link Resource}. Frameworks are
 * required to identify changes to a Resource when they are updated. For simple
 * fields and properties, this can be done easily with comparisons, but this
 * does not work when a Resource depends on bulk data such as {@link BufferData}
 * .
 * </p>
 * <p>
 * When the queue reaches its {@link #MAX_CHANGE_QUEUE_SIZE capacity}, items at
 * the front of the queue (i.e. older ones) are discarded, causing the window to
 * slide forward in "time".
 * </p>
 * <p>
 * The queue stores a version that increases each time a "bulk change" is
 * queued. This can be used to quickly determine if the Resource has been
 * changed since the last time it was processed. Additionally, it can be used to
 * check if the queue has changed so much that changes have been discarded from
 * the queue before it has been processed because it was at max capacity.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The type representing the "bulk change"
 */
public class BulkChangeQueue<T> {
    public static final int MAX_CHANGE_QUEUE_SIZE = 20;
    
    private int version;
    private LinkedList<VersionedChange<T>> latestChanges;
    
    /**
     * Create a new BulkChangeQueue.
     */
    public BulkChangeQueue() {
        version = 0;
        latestChanges = new LinkedList<VersionedChange<T>>();
    }

    /**
     * Push the given change to the end of this queue. If the queue is at
     * capacity, the change at the head of the queue will be discarded.
     * 
     * @param change The new change to record
     * @return The new version of queue
     * @throws NullPointerException if change is null
     */
    public int push(T change) {
        if (change == null)
            throw new NullPointerException("Change cannot be null");
        
        latestChanges.addLast(new VersionedChange<T>(change, ++version));
        if (latestChanges.size() > MAX_CHANGE_QUEUE_SIZE)
            latestChanges.removeFirst();
        return version;
    }
    
    /**
     * @return The current version of the queue
     */
    public int getVersion() {
        return version;
    }

    /**
     * Return the last queued change, representing the newest or most recent
     * change to the Resource implicitly tied to this queue.
     * 
     * @return The latest change or null if there are no queued changes
     */
    public T getLatestChange() {
        return (latestChanges.isEmpty() ? null : latestChanges.getLast().change);
    }

    /**
     * <p>
     * Return a list of changes that were pushed after the given version,
     * <tt>lastKnownVersion</tt>. This may not return every pushed change if so
     * many changes have been pushed that older changes were pushed off the
     * queue, even if those discarded changes were pushed after the last known
     * version. Use {@link #hasLostChanges(int)} to determine if this is the
     * case.
     * </p>
     * <p>
     * If the queue has been cleared, this may return an empty list even if
     * {@link #isVersionStale(int)} returns true.
     * </p>
     * 
     * @param lastKnownVersion The last known version of the queue, use -1 for
     *            the first time processing
     * @return A list of changes, from oldest to newest that came after the
     *         given version
     */
    public List<T> getChangesSince(int lastKnownVersion) {
        // If we can easily tell that there are no new changes, just use emptyList
        // to avoid object creation overhead
        if (!isVersionStale(lastKnownVersion))
            return Collections.emptyList();
        
        // At this point there is a very good chance that we will have at least
        // one object to put into the returned array, so the object creation is 
        // acceptable.
        List<T> changes = new ArrayList<T>();
        Iterator<VersionedChange<T>> it = latestChanges.iterator();
        
        VersionedChange<T> v;
        while(it.hasNext()) {
            v = it.next();
            if (v.version > lastKnownVersion || (lastKnownVersion > 0 && version < 0))
                changes.add(v.change);
        }
        
        return changes;
    }

    /**
     * Return true if the provided version is older than the current version of
     * the queue.
     * 
     * @param lastKnownVersion The last known or processed version, use -1 if
     *            the queue has not been processed
     * @return True if there could be changes returned by
     *         {@link #getChangesSince(int)}
     */
    public boolean isVersionStale(int lastKnownVersion) {
        return version > lastKnownVersion || (lastKnownVersion > 0 && version < 0);
    }

    /**
     * Return true if so many changes have been pushed since
     * <tt>lastKnownVersion</tt> that some of the unprocessed changes have been
     * discarded.
     * 
     * @param lastKnownVersion The last processed version, use -1 if the queue
     *            has not been processed
     * @return True if unprocessed changes have been discarded
     */
    public boolean hasLostChanges(int lastKnownVersion) {
        if (latestChanges.isEmpty())
            return lastKnownVersion < version - MAX_CHANGE_QUEUE_SIZE;
        
        // If the input version is over 1 version behind the last
        // stored version, a change has been dropped, so the input version
        // is too old
        return version < latestChanges.getFirst().version - 1;
    }

    /**
     * Remove all queued changes and increment the version. This is useful for
     * when a change invalidates all previous changes. After a clear,
     * {@link #isVersionStale(int)} will return true but
     * {@link #getChangesSince(int)} will not return any of the changes that
     * were removed from the queue. It will return any changes queued after the
     * clear.
     * 
     * @return The new version
     */
    public int clear() {
        latestChanges.clear();
        return ++version; // Increment version just in case
    }
    
    private static class VersionedChange<T> {
        final int version;
        final T change;
        
        public VersionedChange(T change, int version) {
            this.change = change;
            this.version = version;
        }
    }
}
