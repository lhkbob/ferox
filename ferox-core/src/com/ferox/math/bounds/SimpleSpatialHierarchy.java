package com.ferox.math.bounds;

import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.util.Bag;

/**
 * SimpleSpatialHierarchy is a SpatialHierarchy that performs no spatial
 * organization. Each query performs a linear scan through the elements within
 * the hierarchy. Inserts, updates and removals are always constant time, and
 * the SimpleSpatialHierarchy always accepts every element added to it. It is
 * intended that this hierarchy be used to test the validity of other
 * implementations.
 * 
 * @author Michael Ludwig
 * @param <T> The Class type of elements within this hierarchy
 */
public class SimpleSpatialHierarchy<T> implements SpatialHierarchy<T> {
    private final Bag<SimpleKey<T>> elements;
    
    /**
     * Create a new SimpleSpatialHierarchy that is initially empty.
     */
    public SimpleSpatialHierarchy() {
        elements = new Bag<SimpleKey<T>>();
    }
    
    @Override
    public Object add(T item, AxisAlignedBox bounds) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        SimpleKey<T> newKey = new SimpleKey<T>(this, item);
        newKey.index = elements.size();
        newKey.bounds = bounds;
        
        elements.add(newKey);
        return newKey;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void update(T item, AxisAlignedBox bounds, Object key) {
        if (item == null)
            throw new NullPointerException("Item cannot be null");
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        
        if (key instanceof SimpleKey) {
            SimpleKey sk = (SimpleKey) key;
            if (sk.owner == this && sk.data == item) {
                // key is valid, update bounds and return
                sk.bounds = bounds;
                return;
            }
        }
        
        // else key was invalid (not a SimpleKey or the wrong hierarchy)
        throw new IllegalArgumentException("Invalid key: " + key);
    }
    
    @Override
    @SuppressWarnings("unchecked")
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
    public void query(AxisAlignedBox volume, QueryCallback<T> callback) {
        if (volume == null)
            throw new NullPointerException("Query bound volume cannot be null");
        if (callback == null)
            throw new NullPointerException("Callback cannot be null");
        
        SimpleKey<T> key;
        int sz = elements.size();
        for (int i = 0; i < sz; i++) {
            key = elements.get(i);
            if (key.bounds == null || key.bounds.intersects(volume))
                callback.process(key.data);
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
            // we can't use a PlaneState because each item has no spatial locality
            // with the items around it in elements
            if (key.bounds == null || key.bounds.intersects(frustum, null) != FrustumIntersection.OUTSIDE)
                callback.process(key.data);
        }
    }
    
    @Override
    public void query(IntersectionCallback<T> callback) {
        if (callback == null)
            throw new NullPointerException("Callback cannot be null");
        
        int ct = elements.size();
        SimpleKey<T> e1, e2;
        for (int i = 0; i < ct; i++) {
            e1 = elements.get(i);
            for (int j = i + 1; j < ct; j++) {
                e2 = elements.get(j);
                if (e1.bounds == null || e2.bounds == null || e1.bounds.intersects(e2.bounds))
                    callback.process(e1.data, e2.data);
            }
        }
    }

    private static class SimpleKey<T> {
        private final T data;
        private AxisAlignedBox bounds;
        
        private int index;
        private final SimpleSpatialHierarchy<T> owner;
        
        public SimpleKey(SimpleSpatialHierarchy<T> owner, T data) {
            this.owner = owner;
            this.data = data;
        }
    }
}
