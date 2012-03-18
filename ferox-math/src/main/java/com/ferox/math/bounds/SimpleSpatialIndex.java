package com.ferox.math.bounds;

import com.ferox.math.Const;
import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.util.Bag;

/**
 * SimpleSpatialIndex is a SpatialIndex that performs no spatial organization.
 * Each query performs a linear scan through the elements within the hierarchy.
 * Inserts, updates and removals are always constant time, and the
 * SimpleSpatialIndex always accepts every element added to it. It is intended
 * that this hierarchy be used to test the validity of other implementations, or
 * as a last-resort to contain items in a hierarch.
 * 
 * @author Michael Ludwig
 * @param <T> The Class type of elements within this hierarchy
 */
public class SimpleSpatialIndex<T> implements SpatialIndex<T> {
    private final Bag<SimpleKey<T>> elements;
    
    /**
     * Create a new SimpleSpatialIndex that is initially empty.
     */
    public SimpleSpatialIndex() {
        elements = new Bag<SimpleKey<T>>();
    }
    
    @Override
    public Object add(T item, @Const AxisAlignedBox bounds) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        SimpleKey<T> newKey = new SimpleKey<T>(this, item, bounds);
        newKey.index = elements.size();
        
        elements.add(newKey);
        return newKey;
    }
    
    @Override
    @SuppressWarnings({ "rawtypes" })
    public boolean update(T item, @Const AxisAlignedBox bounds, Object key) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        
        if (key instanceof SimpleKey) {
            SimpleKey sk = (SimpleKey) key;
            if (sk.owner == this && sk.data == item) {
                // key is valid, update bounds and return
                sk.bounds.set(bounds);
                return true;
            }
        }
        
        // else key was invalid (not a SimpleKey or the wrong hierarchy)
        throw new IllegalArgumentException("Invalid key: " + key);
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public void remove(T item, Object key) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        
        if (key instanceof SimpleKey) {
            SimpleKey sk = (SimpleKey) key;
            if (sk.owner == this && sk.data == item) {
                // remove quickly based on the key
                elements.remove(sk.index);
                if (sk.index != elements.size()) {
                    // update index of swapped item
                    elements.get(sk.index).index = sk.index;
                }
                return;
            }
        }
        
        // else key was invalid 
        throw new IllegalArgumentException("Invalid key: " + key);
    }

    @Override
    public void query(@Const AxisAlignedBox volume, QueryCallback<T> callback) {
        if (volume == null)
            throw new NullPointerException("Query bound volume cannot be null");
        if (callback == null)
            throw new NullPointerException("Callback cannot be null");
        
        SimpleKey<T> key;
        int sz = elements.size();
        for (int i = 0; i < sz; i++) {
            key = elements.get(i);
            if (key.bounds == null || key.bounds.intersects(volume))
                callback.process(key.data, key.bounds);
        }
    }

    @Override
    public void query(Frustum frustum, QueryCallback<T> callback) {
        if (frustum == null)
            throw new NullPointerException("Query Frustum cannot be null");
        if (callback == null)
            throw new NullPointerException("Callback cannot be null");
        
        SimpleKey<T> key;
        int sz = elements.size();
        for (int i = 0; i < sz; i++) {
            key = elements.get(i);
            // we can't use a PlaneState because each item has no spatial relationship
            // with the items around it in elements
            if (key.bounds.intersects(frustum, null) != FrustumIntersection.OUTSIDE)
                callback.process(key.data, key.bounds);
        }
    }

    private static class SimpleKey<T> {
        private final T data;
        private final AxisAlignedBox bounds;
        
        private int index;
        private final SimpleSpatialIndex<T> owner;
        
        public SimpleKey(SimpleSpatialIndex<T> owner, T data, @Const AxisAlignedBox bounds) {
            this.owner = owner;
            this.data = data;
            this.bounds = new AxisAlignedBox(bounds);
        }
    }
}
