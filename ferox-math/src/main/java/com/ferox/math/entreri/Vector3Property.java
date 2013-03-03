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
import com.ferox.math.Vector3;
import com.lhkbob.entreri.*;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleDataStore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Vector3Property is a caching property that wraps a DoubleProperty as a Vector3.
 *
 * @author Michael Ludwig
 */
@Factory(Vector3Property.Factory.class)
public class Vector3Property implements Property {
    private static final int REQUIRED_ELEMENTS = 3;

    private DoubleDataStore data;

    /**
     * Create a new Vector3Property.
     */
    public Vector3Property() {
        data = new DoubleDataStore(REQUIRED_ELEMENTS, new double[REQUIRED_ELEMENTS]);
    }

    /**
     * Get the vector of this property, for the component at the given index, and store it
     * into <tt>result</tt>. If result is null, a new Vector3 is created and returned.
     *
     * @param index  The component index to retrieve
     * @param result The vector to store the data for the requested component
     *
     * @return result, or a new Vector3 if result was null
     */
    public Vector3 get(int index, Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }

        result.set(data.getArray(), index * REQUIRED_ELEMENTS);
        return result;
    }

    /**
     * Copy the values of <tt>v</tt> into the underlying data of this property, for the
     * component at the given index.
     *
     * @param v     The vector to copy
     * @param index The index of the component being modified
     *
     * @throws NullPointerException if v is null
     */
    public void set(@Const Vector3 v, int index) {
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
     * Attribute annotation to apply to Vector3Property declarations.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultVector3 {
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
    }

    /**
     * Default factory implementation for Vector3Properties, supports the {@link
     * DefaultVector3} annotation to specify the default vector coordinates.
     *
     * @author Michael Ludwig
     */
    public static class Factory extends AbstractPropertyFactory<Vector3Property> {
        private final Vector3 dflt;

        public Factory(Attributes attrs) {
            super(attrs);
            if (attrs.hasAttribute(DefaultVector3.class)) {
                DefaultVector3 v = attrs.getAttribute(DefaultVector3.class);
                dflt = new Vector3(v.x(), v.y(), v.z());
            } else {
                dflt = new Vector3();
            }
        }

        public Factory(@Const Vector3 v) {
            super(null);
            dflt = new Vector3(v);
        }

        @Override
        public Vector3Property create() {
            return new Vector3Property();
        }

        @Override
        public void setDefaultValue(Vector3Property property, int index) {
            property.set(dflt, index);
        }
    }
}
