package com.ferox.math.entreri;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.lhkbob.entreri.Attribute;
import com.lhkbob.entreri.Attributes;
import com.lhkbob.entreri.Factory;
import com.lhkbob.entreri.IndexedDataStore;
import com.lhkbob.entreri.Property;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleDataStore;

/**
 * Matrix3Property is a caching property that wraps a DoubleProperty as a
 * Matrix3.
 * 
 * @author Michael Ludwig
 */
@Factory(Matrix3Property.Factory.class)
public class Matrix3Property implements Property {
    private static final int REQUIRED_ELEMENTS = 9;

    private DoubleDataStore data;

    /**
     * Create a new Matrix3Property.
     */
    public Matrix3Property() {
        data = new DoubleDataStore(REQUIRED_ELEMENTS, new double[REQUIRED_ELEMENTS]);
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
        
        result.set(data.getArray(), index * REQUIRED_ELEMENTS, false);
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
        v.get(data.getArray(), index * REQUIRED_ELEMENTS, false);
    }
    
    @Override
    public IndexedDataStore getDataStore() {
        return data;
    }

    @Override
    public void setDataStore(IndexedDataStore store) {
        data = (DoubleDataStore) store;
    }
    
    /**
     * Attribute annotation to apply to Matrix3Property declarations.
     * 
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultMatrix3 {
        /**
         * @return Default m00 value
         */
        double m00();
        /**
         * @return Default m01 value
         */
        double m01();
        /**
         * @return Default m02 value
         */
        double m02();
        /**
         * @return Default m10 value
         */
        double m10();
        /**
         * @return Default m11 value
         */
        double m11();
        /**
         * @return Default m12 value
         */
        double m12();
        /**
         * @return Default m20 value
         */
        double m20();
        /**
         * @return Default m21 value
         */
        double m21();
        /**
         * @return Default m22 value
         */
        double m22();
    }
    
    /**
     * Default factory implementation for Matrix3Properties, supports the
     * {@link DefaultMatrix3} annotation to specify the default matrix
     * coordinates.
     * 
     * @author Michael Ludwig
     */
    public static class Factory extends AbstractPropertyFactory<Matrix3Property> {
        private final Matrix3 dflt;
        
        public Factory(Attributes attrs) {
            super(attrs);
            if (attrs.hasAttribute(DefaultMatrix3.class)) {
                DefaultMatrix3 v = attrs.getAttribute(DefaultMatrix3.class);
                dflt = new Matrix3(v.m00(), v.m01(), v.m02(),
                                   v.m10(), v.m11(), v.m12(),
                                   v.m20(), v.m21(), v.m22());
            } else {
                dflt = new Matrix3();
            }
        }
        
        public Factory(@Const Matrix3 v) {
            super(null);
            dflt = new Matrix3(v);
        }

        @Override
        public Matrix3Property create() {
            return new Matrix3Property();
        }

        @Override
        public void setDefaultValue(Matrix3Property property, int index) {
            property.set(dflt, index);
        }
    }
}
