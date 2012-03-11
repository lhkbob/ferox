package com.ferox.math.entreri;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.IndexedDataStore;
import com.lhkbob.entreri.property.Property;
import com.lhkbob.entreri.property.PropertyFactory;

/**
 * ColorRGBProperty is a caching property that wraps a DoubleProperty as a
 * ColorRGB.
 * 
 * @author Michael Ludwig
 */
public class ColorRGBProperty implements Property {
    private static final int REQUIRED_ELEMENTS = 3;

    private final DoubleProperty data;
    
    /**
     * Create a new ColorRGBProperty.
     */
    public ColorRGBProperty() {
        data = new DoubleProperty(REQUIRED_ELEMENTS);
    }
    
    /**
     * @return PropertyFactory that creates ColorRGBProperties
     */
    public static PropertyFactory<ColorRGBProperty> factory() {
        return new AbstractPropertyFactory<ColorRGBProperty>() {
            @Override
            public ColorRGBProperty create() {
                return new ColorRGBProperty();
            }

            @Override
            public void setDefaultValue(ColorRGBProperty property, int index) {
                for (int i = 0; i < REQUIRED_ELEMENTS; i++) {
                    property.data.set(0f, index, i);
                }
            }
        };
    }

    /**
     * Get the color of this property, for the component at the given
     * index, and store it into <tt>result</tt>. If result is null, a new
     * ColorRGB is created and returned.
     * 
     * @param index The component index to retrieve
     * @param result The color to store the data for the requested component
     * @return result, or a new ColorRGB if result was null
     */
    public ColorRGB get(int index, ColorRGB result) {
        if (result == null)
            result = new ColorRGB();
        
        result.set(data.getIndexedData(), index * REQUIRED_ELEMENTS);
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
    public void set(@Const ColorRGB v, int index) {
        v.getHDR(data.getIndexedData(), index * REQUIRED_ELEMENTS);
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
