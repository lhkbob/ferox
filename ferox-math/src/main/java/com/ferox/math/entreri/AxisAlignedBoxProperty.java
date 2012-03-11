package com.ferox.math.entreri;

import com.ferox.math.Const;
import com.ferox.math.bounds.AxisAlignedBox;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.IndexedDataStore;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * AxisAlignedBoxProperty is a property that wraps a {@link DoubleProperty} as a
 * AxisAlginedBox.
 * 
 * @author Michael Ludwig
 */
public class AxisAlignedBoxProperty implements Property {
    private static final int REQUIRED_ELEMENTS = 6;
    
    private final DoubleProperty data;
    
    /**
     * Create a new AxisAlignedBoxProperty.
     */
    public AxisAlignedBoxProperty() {
        data = new DoubleProperty(REQUIRED_ELEMENTS);
    }
    
    /**
     * @return PropertyFactory that creates AxisAlignedBoxProperties
     */
    public static PropertyFactory<AxisAlignedBoxProperty> factory() {
        return new AbstractPropertyFactory<AxisAlignedBoxProperty>() {
            @Override
            public AxisAlignedBoxProperty create() {
                return new AxisAlignedBoxProperty();
            }

            @Override
            public void setDefaultValue(AxisAlignedBoxProperty property, int index) {
                for (int i = 0; i < REQUIRED_ELEMENTS; i++) {
                    property.data.set(0f, index, i);
                }
            }
        };
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
        
        result.min.set(data.getIndexedData(), index * REQUIRED_ELEMENTS);
        result.max.set(data.getIndexedData(), index * REQUIRED_ELEMENTS + 3);
        
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
    public void set(@Const AxisAlignedBox b, int index) {
        b.min.get(data.getIndexedData(), index * REQUIRED_ELEMENTS);
        b.max.get(data.getIndexedData(), index * REQUIRED_ELEMENTS + 3);
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
