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
import com.ferox.math.Quat4;
import com.lhkbob.entreri.*;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleDataStore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Quat4Property is a caching property that wraps a DoubleProperty as a Quat4.
 *
 * @author Michael Ludwig
 */
@Factory(Quat4Property.Factory.class)
public class Quat4Property implements Property {
    private static final int REQUIRED_ELEMENTS = 4;

    private DoubleDataStore data;

    /**
     * Create a new Quat4Property.
     */
    public Quat4Property() {
        data = new DoubleDataStore(REQUIRED_ELEMENTS, new double[REQUIRED_ELEMENTS]);
    }

    /**
     * Get the quaternion of this property, for the component at the given index, and
     * store it into <var>result</var>. If result is null, a new Quat4 is created and
     * returned.
     *
     * @param index  The component index to retrieve
     * @param result The quaternion to store the data for the requested component
     *
     * @return result, or a new Quat4 if result was null
     */
    public Quat4 get(int index, Quat4 result) {
        if (result == null) {
            result = new Quat4();
        }

        result.set(data.getArray(), index * REQUIRED_ELEMENTS);
        return result;
    }

    /**
     * Copy the values of <var>v</var> into the underlying data of this property, for the
     * component at the given index.
     *
     * @param v     The quaternion to copy
     * @param index The index of the component being modified
     *
     * @throws NullPointerException if v is null
     */
    public void set(@Const Quat4 v, int index) {
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
     * Attribute annotation to apply to Quat4Property declarations.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultQuat4 {
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
     * Default factory implementation for Quat4Properties, supports the {@link
     * DefaultQuat4} annotation to specify the default quaternion coordinates.
     *
     * @author Michael Ludwig
     */
    public static class Factory extends AbstractPropertyFactory<Quat4Property> {
        private final Quat4 dflt;

        public Factory(Attributes attrs) {
            super(attrs);
            if (attrs.hasAttribute(DefaultQuat4.class)) {
                DefaultQuat4 v = attrs.getAttribute(DefaultQuat4.class);
                dflt = new Quat4(v.x(), v.y(), v.z(), v.w());
            } else {
                dflt = new Quat4();
            }
        }

        public Factory(@Const Quat4 v) {
            super(null);
            dflt = new Quat4(v);
        }

        @Override
        public Quat4Property create() {
            return new Quat4Property();
        }

        @Override
        public void setDefaultValue(Quat4Property property, int index) {
            property.set(dflt, index);
        }
    }
}
