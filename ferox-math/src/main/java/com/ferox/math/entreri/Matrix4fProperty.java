package com.ferox.math.entreri;

import com.ferox.math.Matrix4f;
import com.ferox.math.MutableMatrix4f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.googlecode.entreri.property.CompactAwareProperty;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.IndexedDataStore;

/**
 * Matrix4fProperty is a caching property that wraps a FloatProperty as a
 * ReadOnlyMatrix4f, but also provides a setter so it can be mutated.
 * 
 * @author Michael Ludwig
 */
public class Matrix4fProperty implements CompactAwareProperty {
    private final FloatProperty data;
    private final Matrix4f cache;
    
    private int lastIndex;
    
    /**
     * Create a new Matrix4fProperty.
     */
    public Matrix4fProperty() {
        data = new FloatProperty(16);
        cache = new Matrix4f();
        lastIndex = -1;
    }

    /**
     * Get the ReadOnlyMatrix4f at the given component index. The values are
     * transferred from the underlying FloatProperty into a cached Matrix4
     * instance if needed. This means that the returned instance is invalidated
     * when a new index is fetched.
     * 
     * @param index The component index to retrieve
     * @return The matrix for the requested component
     */
    public ReadOnlyMatrix4f get(int index) {
        if (lastIndex != index) {
            cache.set(data.getIndexedData(), index * 16, false);
            lastIndex = index;
        }
        return cache;
    }
    
    /**
     * Get the matrix of this property, for the component at the given
     * index, and store it into <tt>result</tt>. If result is null, a new
     * Matrix4 is created and returned.
     * 
     * @param index The component index to retrieve
     * @param result The matrix to store the data for the requested component
     * @return result, or a new Matrix4 if result was null
     */
    public MutableMatrix4f get(int index, MutableMatrix4f result) {
        if (result == null)
            result = new Matrix4f();
        
        result.set(data.getIndexedData(), index * 16, false);
        return result;
    }
    
    /**
     * Copy the values of <tt>v</tt> into the underlying data of this property,
     * for the component at the given index.
     * 
     * @param v The matrix to copy
     * @param index The index of the component being modified
     * @throws NullPointerException if v is null
     */
    public void set(ReadOnlyMatrix4f v, int index) {
        v.get(data.getIndexedData(), index * 16, false);
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
