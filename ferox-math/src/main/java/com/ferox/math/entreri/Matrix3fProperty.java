package com.ferox.math.entreri;

import com.ferox.math.Matrix3f;
import com.ferox.math.MutableMatrix3f;
import com.ferox.math.ReadOnlyMatrix3f;
import com.googlecode.entreri.property.CompactAwareProperty;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.IndexedDataStore;

/**
 * Matrix3fProperty is a caching property that wraps a FloatProperty as a
 * ReadOnlyMatrix3f, but also provides a setter so it can be mutated.
 * 
 * @author Michael Ludwig
 */
public class Matrix3fProperty implements CompactAwareProperty {
    private final FloatProperty data;
    private final Matrix3f cache;
    
    private int lastIndex;
    
    /**
     * Create a new Matrix3fProperty.
     */
    public Matrix3fProperty() {
        data = new FloatProperty(9);
        cache = new Matrix3f();
        lastIndex = -1;
    }

    /**
     * Get the ReadOnlyMatrix3f at the given component index. The values are
     * transferred from the underlying FloatProperty into a cached Matrix3f
     * instance if needed. This means that the returned instance is invalidated
     * when a new index is fetched.
     * 
     * @param index The component index to retrieve
     * @return The matrix for the requested component
     */
    public ReadOnlyMatrix3f get(int index) {
        if (lastIndex != index) {
            cache.set(data.getIndexedData(), index * 9, false);
            lastIndex = index;
        }
        return cache;
    }
    
    /**
     * Get the matrix of this property, for the component at the given
     * index, and store it into <tt>result</tt>. If result is null, a new
     * Matrix3f is created and returned.
     * 
     * @param index The component index to retrieve
     * @param result The matrix to store the data for the requested component
     * @return result, or a new Matrix3f if result was null
     */
    public MutableMatrix3f get(int index, MutableMatrix3f result) {
        if (result == null)
            result = new Matrix3f();
        
        result.set(data.getIndexedData(), index * 9, false);
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
    public void set(ReadOnlyMatrix3f v, int index) {
        v.get(data.getIndexedData(), index * 9, false);
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
