package com.ferox.math.entreri;

import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.ReadOnlyAxisAlignedBox;
import com.googlecode.entreri.property.CompactAwareProperty;
import com.googlecode.entreri.property.FloatProperty;
import com.googlecode.entreri.property.IndexedDataStore;

/**
 * AxisAlignedBoxProperty is a caching property that wraps a FloatProperty as a
 * ReadOnlyAxisAlignedBox, but also provides a setter so it can be mutated.
 * 
 * @author Michael Ludwig
 */
public class AxisAlignedBoxProperty implements CompactAwareProperty {
    private final FloatProperty data;
    private final AxisAlignedBox cache;
    
    private int lastIndex;
    
    /**
     * Create a new AxisAlignedBoxProperty.
     */
    public AxisAlignedBoxProperty() {
        data = new FloatProperty(6);
        cache = new AxisAlignedBox();
        lastIndex = -1;
    }

    /**
     * Get the ReadOnlyAxisAlignedBox at the given component index. The values
     * are transferred from the underlying FloatProperty into a cached
     * AxisAlignedBox instance if needed. This means that the returned instance
     * is invalidated when a new index is fetched.
     * 
     * @param index The component index to retrieve
     * @return The box for the requested component
     */
    public ReadOnlyAxisAlignedBox get(int index) {
        if (lastIndex != index) {
            cache.getMin().set(data.getIndexedData(), index * 6);
            cache.getMax().set(data.getIndexedData(), index * 6 + 3);
            lastIndex = index;
        }
        return cache;
    }

    /**
     * Get the axis aligned box of this property, for the component at the given
     * index, and store it into <tt>result</tt>. If result is null, a new
     * AxisAlignedBox is created and returned.
     * 
     * @param index The component index to retrieve
     * @param result The box to store the bounds for the requested component
     * @return result, or a new AxisAlignedBox if result was null
     */
    public AxisAlignedBox get(int index, AxisAlignedBox result) {
        if (result == null)
            result = new AxisAlignedBox();
        
        result.getMin().set(data.getIndexedData(), index * 6);
        result.getMax().set(data.getIndexedData(), index * 6 + 3);
        
        return result;
    }
    
    /**
     * Copy the state of <tt>b</tt> into the underlying data of this property,
     * for the component at the given index.
     * 
     * @param v The box to copy
     * @param index The index of the component being modified
     * @throws NullPointerException if b is null
     */
    public void set(ReadOnlyAxisAlignedBox b, int index) {
        b.getMin().get(data.getIndexedData(), index * 6);
        b.getMax().get(data.getIndexedData(), index * 6 + 3);
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
