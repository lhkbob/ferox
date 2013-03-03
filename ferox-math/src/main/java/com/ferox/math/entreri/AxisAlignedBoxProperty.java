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

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.lhkbob.entreri.*;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleDataStore;
import com.lhkbob.entreri.property.DoubleProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AxisAlignedBoxProperty is a property that wraps a {@link DoubleProperty} as a
 * AxisAlginedBox.
 *
 * @author Michael Ludwig
 */
@Factory(AxisAlignedBoxProperty.Factory.class)
public class AxisAlignedBoxProperty implements Property {
    private static final int REQUIRED_ELEMENTS = 6;

    private DoubleDataStore data;

    /**
     * Create a new AxisAlignedBoxProperty.
     */
    public AxisAlignedBoxProperty() {
        data = new DoubleDataStore(REQUIRED_ELEMENTS, new double[REQUIRED_ELEMENTS]);
    }

    /**
     * Get the axis aligned box of this property, for the component at the given index,
     * and store it into <tt>result</tt>. If result is null, a new AxisAlignedBox is
     * created and returned.
     *
     * @param index  The component index to retrieve
     * @param result The box to store the bounds for the requested component
     *
     * @return result, or a new AxisAlignedBox if result was null
     */
    public AxisAlignedBox get(int index, AxisAlignedBox result) {
        if (result == null) {
            result = new AxisAlignedBox();
        }

        result.min.set(data.getArray(), index * REQUIRED_ELEMENTS);
        result.max.set(data.getArray(), index * REQUIRED_ELEMENTS + 3);

        return result;
    }

    /**
     * Copy the state of <tt>b</tt> into the underlying data of this property, for the
     * component at the given index.
     *
     * @param v     The box to copy
     * @param index The index of the component being modified
     *
     * @throws NullPointerException if b is null
     */
    public void set(@Const AxisAlignedBox b, int index) {
        b.min.get(data.getArray(), index * REQUIRED_ELEMENTS);
        b.max.get(data.getArray(), index * REQUIRED_ELEMENTS + 3);
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
     * Attribute annotation to apply to AxisAlignedBoxProperty declarations, to specify
     * the minimum coordinate of the box.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultMin {
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
     * Attribute annotation to apply to AxisAlignedBoxProperty declarations, to specify
     * the maximum coordinate of the box.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultMax {
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
     * Default factory implementation for AxisAlignedBoxProperties, supports the {@link
     * DefaultMin} and {@link DefaultMax} annotations to specify the default bounding
     * box.
     *
     * @author Michael Ludwig
     */
    public static class Factory extends AbstractPropertyFactory<AxisAlignedBoxProperty> {
        private final AxisAlignedBox dflt;

        public Factory(Attributes attrs) {
            super(attrs);
            dflt = new AxisAlignedBox();

            if (attrs.hasAttribute(DefaultMin.class)) {
                DefaultMin min = attrs.getAttribute(DefaultMin.class);
                dflt.min.set(min.x(), min.y(), min.z());
            }

            if (attrs.hasAttribute(DefaultMax.class)) {
                DefaultMax max = attrs.getAttribute(DefaultMax.class);
                dflt.min.set(max.x(), max.y(), max.z());
            }
        }

        public Factory(@Const AxisAlignedBox v) {
            super(null);
            dflt = new AxisAlignedBox(v);
        }

        @Override
        public AxisAlignedBoxProperty create() {
            return new AxisAlignedBoxProperty();
        }

        @Override
        public void setDefaultValue(AxisAlignedBoxProperty property, int index) {
            property.set(dflt, index);
        }
    }
}
