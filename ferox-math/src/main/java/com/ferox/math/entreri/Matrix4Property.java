package com.ferox.math.entreri;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.IndexedDataStore;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * Matrix4Property is a caching property that wraps a DoubleProperty as a
 * Matrix4.
 * 
 * @author Michael Ludwig
 */
public class Matrix4Property implements Property {
    private static final int REQUIRED_ELEMENTS = 16;

    private final DoubleProperty data;
    
    /**
     * Create a new Matrix4Property.
     */
    public Matrix4Property() {
        data = new DoubleProperty(REQUIRED_ELEMENTS);
    }
    
    /**
     * @return PropertyFactory that creates Matrix4Properties
     */
    public static PropertyFactory<Matrix4Property> factory() {
        return new AbstractPropertyFactory<Matrix4Property>() {
            @Override
            public Matrix4Property create() {
                return new Matrix4Property();
            }

            @Override
            public void setDefaultValue(Matrix4Property property, int index) {
                for (int i = 0; i < REQUIRED_ELEMENTS; i++) {
                    property.data.set(0f, index, i);
                }
            }
        };
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
    public Matrix4 get(int index, Matrix4 result) {
        if (result == null)
            result = new Matrix4();
        
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
    public void set(@Const Matrix4 v, int index) {
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
