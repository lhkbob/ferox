package com.ferox.math.entreri;

import com.ferox.math.Matrix3;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.IndexedDataStore;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * Matrix3Property is a caching property that wraps a DoubleProperty as a
 * Matrix3.
 * 
 * @author Michael Ludwig
 */
public class Matrix3Property implements Property {
    private static final int REQUIRED_ELEMENTS = 9;

    private final DoubleProperty data;

    /**
     * Create a new Matrix3Property.
     */
    public Matrix3Property() {
        data = new DoubleProperty(REQUIRED_ELEMENTS);
    }
    
    /**
     * @return PropertyFactory that creates Matrix3Properties
     */
    public static PropertyFactory<Matrix3Property> factory() {
        return new AbstractPropertyFactory<Matrix3Property>() {
            @Override
            public Matrix3Property create() {
                return new Matrix3Property();
            }

            @Override
            public void setDefaultValue(Matrix3Property property, int index) {
                for (int i = 0; i < REQUIRED_ELEMENTS; i++) {
                    property.data.set(0f, index, i);
                }
            }
        };
    }

    /**
     * Get the matrix of this property, for the component at the given
     * index, and store it into <tt>result</tt>. If result is null, a new
     * Matrix3 is created and returned.
     * 
     * @param index The component index to retrieve
     * @param result The matrix to store the data for the requested component
     * @return result, or a new Matrix3 if result was null
     */
    public Matrix3 get(int index, Matrix3 result) {
        if (result == null)
            result = new Matrix3();
        
        result.set(data.getIndexedData(), index * REQUIRED_ELEMENTS, false);
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
    public void set(Matrix3 v, int index) {
        v.get(data.getIndexedData(), index * REQUIRED_ELEMENTS, false);
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
