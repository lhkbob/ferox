package com.ferox.math.entreri;

import com.ferox.math.Color3f;
import com.ferox.math.ReadOnlyColor3f;
import com.googlecode.entreri.property.CompactAwareProperty;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.IndexedDataStore;

/**
 * Color3fProperty is a caching property that wraps a FloatProperty as a
 * ReadOnlyColor3f, but also provides a setter so it can be mutated.
 * 
 * @author Michael Ludwig
 */
public class Color3fProperty implements CompactAwareProperty {
    private final FloatProperty data;
    private final Color3f cache;
    
    private int lastIndex;
    
    /**
     * Create a new Color3fProperty.
     */
    public Color3fProperty() {
        data = new FloatProperty(3);
        cache = new Color3f();
        lastIndex = -1;
    }

    /**
     * Get the ReadOnlyColor3f at the given component index. The values are
     * transferred from the underlying FloatProperty into a cached Color3f
     * instance if needed. This means that the returned instance is invalidated
     * when a new index is fetched.
     * 
     * @param index The component index to retrieve
     * @return The color for the requested component
     */
    public ReadOnlyColor3f get(int index) {
        if (lastIndex != index) {
            cache.set(data.getIndexedData(), index * 3);
            lastIndex = index;
        }
        return cache;
    }

    /**
     * Get the color of this property, for the component at the given
     * index, and store it into <tt>result</tt>. If result is null, a new
     * Color3f is created and returned.
     * 
     * @param index The component index to retrieve
     * @param result The color to store the data for the requested component
     * @return result, or a new Color3f if result was null
     */
    public Color3f get(int index, Color3f result) {
        if (result == null)
            result = new Color3f();
        
        result.set(data.getIndexedData(), index * 3);
        return result;
    }
    
    /**
     * Copy the values of <tt>v</tt> into the underlying data of this property,
     * for the component at the given index.
     * 
     * @param v The color to copy
     * @param index The index of the component being modified
     * @throws NullPointerException if v is null
     */
    public void set(ReadOnlyColor3f v, int index) {
        v.getHDR(data.getIndexedData(), index * 3);
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
