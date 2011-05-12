package com.ferox.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class BulkChangeQueue<T> {
    public static final int MAX_CHANGE_QUEUE_SIZE = 20;
    
    private int version;
    private LinkedList<VersionedChange<T>> latestChanges;
    
    public BulkChangeQueue() {
        version = 0;
        latestChanges = new LinkedList<VersionedChange<T>>();
    }
    
    public int push(T change) {
        if (change == null)
            throw new NullPointerException("Change cannot be null");
        
        latestChanges.addLast(new VersionedChange<T>(change, ++version));
        if (latestChanges.size() > MAX_CHANGE_QUEUE_SIZE)
            latestChanges.removeFirst();
        return version;
    }
    
    public int getVersion() {
        return version;
    }
    
    public T getLatestChange() {
        return (latestChanges.isEmpty() ? null : latestChanges.getLast().change);
    }
    
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
    
    public boolean isVersionStale(int lastKnownVersion) {
        return version > lastKnownVersion || (lastKnownVersion > 0 && version < 0);
    }
    
    public boolean hasLostChanges(int lastKnownVersion) {
        if (latestChanges.isEmpty()) {
            // Not too old if we don't have any changes to report
            return false;
        } else {
            // If the input version is over 1 version behind the last
            // stored version, a change has been dropped, so the input version
            // is too old
            return version < latestChanges.getFirst().version - 1;
        }
    }
    
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
