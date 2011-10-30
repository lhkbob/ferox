package com.ferox.math.entreri;

import com.ferox.math.Quat4f;
import com.ferox.math.ReadOnlyQuat4f;
import com.googlecode.entreri.property.CompactAwareProperty;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.IndexedDataStore;

/**
 * Quat4fProperty is a caching property that wraps a FloatProperty
 * as a ReadOnlyQuat4f, but also provides a setter so it can be
 * mutated.
 * @author Michael LUdwig
 *
 */
public class Quat4fProperty implements CompactAwareProperty {
    private final FloatProperty data;
    private final Quat4f cache;
    
    private int lastIndex;
    
    /**
     * Create a new Quat4fProperty.
     */
    public Quat4fProperty() {
        data = new FloatProperty(4);
        cache = new Quat4f();
        lastIndex = -1;
    }

    /**
     * Get the ReadOnlyQuat4f at the given component index. The values are
     * transferred from the underlying FloatProperty into a cached Quat4f
     * instance if needed. This means that the returned instance is invalidated
     * when a new index is fetched.
     * 
     * @param index The component index to retrieve
     * @return The quaternion for the requested component
     */
    public ReadOnlyQuat4f get(int index) {
        if (lastIndex != index) {
            cache.set(data.getIndexedData(), index * 4);
            lastIndex = index;
        }
        return cache;
    }
    
    /**
     * Copy the values of <tt>v</tt> into the underlying data of this property,
     * for the component at the given index.
     * 
     * @param v The quaternion to copy
     * @param index The index of the component being modified
     * @throws NullPointerException if v is null
     */
    public void set(ReadOnlyQuat4f v, int index) {
        v.get(data.getIndexedData(), index * 4);
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
