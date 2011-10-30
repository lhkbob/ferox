package com.ferox.math.entreri;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.googlecode.entreri.property.CompactAwareProperty;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.IndexedDataStore;

/**
 * Vector3fProperty is a caching property that wraps a FloatProperty
 * as a ReadOnlyVector3f, but also provides a setter so it can be
 * mutated.
 * @author Michael LUdwig
 *
 */
public class Vector3fProperty implements CompactAwareProperty {
    private final FloatProperty data;
    private final Vector3f cache;
    
    private int lastIndex;
    
    /**
     * Create a new Vector3fProperty.
     */
    public Vector3fProperty() {
        data = new FloatProperty(3);
        cache = new Vector3f();
        lastIndex = -1;
    }

    /**
     * Get the ReadOnlyVector3f at the given component index. The values are
     * transferred from the underlying FloatProperty into a cached Vector3f
     * instance if needed. This means that the returned instance is invalidated
     * when a new index is fetched.
     * 
     * @param index The component index to retrieve
     * @return The vector for the requested component
     */
    public ReadOnlyVector3f get(int index) {
        if (lastIndex != index) {
            cache.set(data.getIndexedData(), index * 3);
            lastIndex = index;
        }
        return cache;
    }
    
    /**
     * Copy the values of <tt>v</tt> into the underlying data of this property,
     * for the component at the given index.
     * 
     * @param v The vector to copy
     * @param index The index of the component being modified
     * @throws NullPointerException if v is null
     */
    public void set(ReadOnlyVector3f v, int index) {
        v.get(data.getIndexedData(), index * 3);
        lastIndex = -1;
    }
    
    @Override
    public IndexedDataStore getDataStore() {
        return data.getDataStore();
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        data.setDataStore(store);
    }

    @Override
    public void onCompactComplete() {
        // must reset the index cache in case the currently
        // cached component was moved by the compact.
        lastIndex = -1;
    }
}
