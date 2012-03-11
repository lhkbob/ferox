package com.ferox.math.entreri;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.IndexedDataStore;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * Vector3Property is a caching property that wraps a DoubleProperty as a
 * Vector3.
 * 
 * @author Michael Ludwig
 */
public class Vector3Property implements Property {
    private static final int REQUIRED_ELEMENTS = 3;

    private final DoubleProperty data;
    
    /**
     * Create a new Vector3Property.
     */
    public Vector3Property() {
        data = new DoubleProperty(REQUIRED_ELEMENTS);
    }
    
    /**
     * @return PropertyFactory that creates Vector3Properties
     */
    public static PropertyFactory<Vector3Property> factory() {
        return new AbstractPropertyFactory<Vector3Property>() {
            @Override
            public Vector3Property create() {
                return new Vector3Property();
            }

            @Override
            public void setDefaultValue(Vector3Property property, int index) {
                for (int i = 0; i < REQUIRED_ELEMENTS; i++) {
                    property.data.set(0f, index, i);
                }
            }
        };
    }

    /**
     * Get the vector of this property, for the component at the given index,
     * and store it into <tt>result</tt>. If result is null, a new Vector3 is
     * created and returned.
     * 
     * @param index The component index to retrieve
     * @param result The vector to store the data for the requested component
     * @return result, or a new Vector3 if result was null
     */
    public Vector3 get(int index, Vector3 result) {
        if (result == null)
            result = new Vector3();
        
        result.set(data.getIndexedData(), index * REQUIRED_ELEMENTS);
        return result;
    }
    
    /**
     * Copy the values of <tt>v</tt> into the underlying data of this property,
     * for the component at the given index.
     * 
     * @param v The vector to copy
     * @param index The index of the component being modified
     * @throws NullPointerException if v is null
     */
    public void set(@Const Vector3 v, int index) {
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
