package com.ferox.math.entreri;

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.IndexedDataStore;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * Vector4Property is a caching property that wraps a DoubleProperty as a
 * Vector4.
 * 
 * @author Michael Ludwig
 */
public class Vector4Property implements Property {
    private static final int REQUIRED_ELEMENTS = 4;

    private final DoubleProperty data;
    
    /**
     * Create a new Vector4Property.
     */
    public Vector4Property() {
        data = new DoubleProperty(REQUIRED_ELEMENTS);
    }
    
    /**
     * @return PropertyFactory that creates Vector4Properties
     */
    public static PropertyFactory<Vector4Property> factory() {
        return new AbstractPropertyFactory<Vector4Property>() {
            @Override
            public Vector4Property create() {
                return new Vector4Property();
            }

            @Override
            public void setDefaultValue(Vector4Property property, int index) {
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
     * @return result, or a new Vector4 if result was null
     */
    public Vector4 get(int index, Vector4 result) {
        if (result == null)
            result = new Vector4();
        
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
    public void set(@Const Vector4 v, int index) {
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
