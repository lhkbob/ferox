/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.math.entreri;

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.lhkbob.entreri.*;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleDataStore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Vector4Property is a caching property that wraps a DoubleProperty as a Vector4.
 *
 * @author Michael Ludwig
 */
@Factory(Vector4Property.Factory.class)
public class Vector4Property implements Property {
    private static final int REQUIRED_ELEMENTS = 4;

    private DoubleDataStore data;

    /**
     * Create a new Vector4Property.
     */
    public Vector4Property() {
        data = new DoubleDataStore(REQUIRED_ELEMENTS, new double[REQUIRED_ELEMENTS]);
    }

    /**
     * Get the vector of this property, for the component at the given index, and store it
     * into <var>result</var>. If result is null, a new Vector3 is created and returned.
     *
     * @param index  The component index to retrieve
     * @param result The vector to store the data for the requested component
     *
     * @return result, or a new Vector4 if result was null
     */
    public Vector4 get(int index, Vector4 result) {
        if (result == null) {
            result = new Vector4();
        }

        result.set(data.getArray(), index * REQUIRED_ELEMENTS);
        return result;
    }

    /**
     * Copy the values of <var>v</var> into the underlying data of this property, for the
     * component at the given index.
     *
     * @param v     The vector to copy
     * @param index The index of the component being modified
     *
     * @throws NullPointerException if v is null
     */
    public void set(@Const Vector4 v, int index) {
        v.get(data.getArray(), index * REQUIRED_ELEMENTS);
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
     * Attribute annotation to apply to Vector4Property declarations.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultVector4 {
        /**
         * @return Default x coordinate
         */
        double x();

        /**
         * @return Default y coordinate
         */
        double y();

        /**
         * @return Default z coordinate
         */
        double z();

        /**
         * @return Default w coordinate
         */
        double w();
    }

    /**
     * Default factory implementation for Vector4Properties, supports the {@link
     * DefaultVector4} annotation to specify the default vector coordinates.
     *
     * @author Michael Ludwig
     */
    public static class Factory extends AbstractPropertyFactory<Vector4Property> {
        private final Vector4 dflt;

        public Factory(Attributes attrs) {
            super(attrs);
            if (attrs.hasAttribute(DefaultVector4.class)) {
                DefaultVector4 v = attrs.getAttribute(DefaultVector4.class);
                dflt = new Vector4(v.x(), v.y(), v.z(), v.w());
            } else {
                dflt = new Vector4();
            }
        }

        public Factory(@Const Vector4 v) {
            super(null);
            dflt = new Vector4(v);
        }

        @Override
        public Vector4Property create() {
            return new Vector4Property();
        }

        @Override
        public void setDefaultValue(Vector4Property property, int index) {
            property.set(dflt, index);
        }
    }
}
