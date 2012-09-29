package com.ferox.math.entreri;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.lhkbob.entreri.Attribute;
import com.lhkbob.entreri.Attributes;
import com.lhkbob.entreri.Factory;
import com.lhkbob.entreri.IndexedDataStore;
import com.lhkbob.entreri.Property;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleDataStore;

/**
 * Matrix4Property is a caching property that wraps a DoubleProperty as a
 * Matrix4.
 * 
 * @author Michael Ludwig
 */
@Factory(Matrix4Property.Factory.class)
public class Matrix4Property implements Property {
    private static final int REQUIRED_ELEMENTS = 16;

    private DoubleDataStore data;

    /**
     * Create a new Matrix4Property.
     */
    public Matrix4Property() {
        data = new DoubleDataStore(REQUIRED_ELEMENTS, new double[REQUIRED_ELEMENTS]);
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
        if (result == null) {
            result = new Matrix4();
        }

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
    public void set(@Const Matrix4 v, int index) {
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
     * Attribute annotation to apply to Matrix4Property declarations.
     * 
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultMatrix4 {
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
         * @return Default m03 value
         */
        double m03();
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
         * @return Default m13 value
         */
        double m13();
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
        /**
         * @return Default m23 value
         */
        double m23();
        /**
         * @return Default m30 value
         */
        double m30();
        /**
         * @return Default m31 value
         */
        double m31();
        /**
         * @return Default m32 value
         */
        double m32();
        /**
         * @return Default m33 value
         */
        double m33();
    }

    /**
     * Default factory implementation for Matrix4Properties, supports the
     * {@link DefaultMatrix4} annotation to specify the default matrix
     * coordinates.
     * 
     * @author Michael Ludwig
     */
    public static class Factory extends AbstractPropertyFactory<Matrix4Property> {
        private final Matrix4 dflt;

        public Factory(Attributes attrs) {
            super(attrs);
            if (attrs.hasAttribute(DefaultMatrix4.class)) {
                DefaultMatrix4 v = attrs.getAttribute(DefaultMatrix4.class);
                dflt = new Matrix4(v.m00(), v.m01(), v.m02(), v.m03(),
                                   v.m10(), v.m11(), v.m12(), v.m13(),
                                   v.m20(), v.m21(), v.m22(), v.m23(),
                                   v.m30(), v.m31(), v.m32(), v.m33());
            } else {
                dflt = new Matrix4();
            }
        }

        public Factory(@Const Matrix4 v) {
            super(null);
            dflt = new Matrix4(v);
        }

        @Override
        public Matrix4Property create() {
            return new Matrix4Property();
        }

        @Override
        public void setDefaultValue(Matrix4Property property, int index) {
            property.set(dflt, index);
        }
    }
}
