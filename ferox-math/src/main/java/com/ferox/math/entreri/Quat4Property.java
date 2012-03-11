package com.ferox.math.entreri;

import com.ferox.math.Const;
import com.ferox.math.Quat4;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.IndexedDataStore;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * Quat4Property is a caching property that wraps a DoubleProperty as a
 * Quat4.
 * 
 * @author Michael Ludwig
 */
public class Quat4Property implements Property {
    private static final int REQUIRED_ELEMENTS = 4;

    private final DoubleProperty data;
    
    /**
     * Create a new Quat4Property.
     */
    public Quat4Property() {
        data = new DoubleProperty(REQUIRED_ELEMENTS);
    }
    
    /**
     * @return PropertyFactory that creates Quat4Properties
     */
    public static PropertyFactory<Quat4Property> factory() {
        return new AbstractPropertyFactory<Quat4Property>() {
            @Override
            public Quat4Property create() {
                return new Quat4Property();
            }

            @Override
            public void setDefaultValue(Quat4Property property, int index) {
                for (int i = 0; i < REQUIRED_ELEMENTS; i++) {
                    property.data.set(0, index, i);
                }
            }
        };
    }
    
    /**
     * Get the quaternion of this property, for the component at the given
     * index, and store it into <tt>result</tt>. If result is null, a new
     * Quat4 is created and returned.
     * 
     * @param index The component index to retrieve
     * @param result The quaternion to store the data for the requested component
     * @return result, or a new Quat4 if result was null
     */
    public Quat4 get(int index, Quat4 result) {
        if (result == null)
            result = new Quat4();
        
        result.set(data.getIndexedData(), index * REQUIRED_ELEMENTS);
        return result;
    }
    
    /**
     * Copy the values of <tt>v</tt> into the underlying data of this property,
     * for the component at the given index.
     * 
     * @param v The quaternion to copy
     * @param index The index of the component being modified
     * @throws NullPointerException if v is null
     */
    public void set(@Const Quat4 v, int index) {
        v.get(data.getIndexedData(), index * REQUIRED_ELEMENTS);
    }
    
    @Override
    public IndexedDataStore getDataStore() {
        return data.getDataStore();
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        data.setDataStore(store);
    }
}
