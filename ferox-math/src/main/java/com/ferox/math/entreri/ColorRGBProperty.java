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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.lhkbob.entreri.Attribute;
import com.lhkbob.entreri.Attributes;
import com.lhkbob.entreri.Factory;
import com.lhkbob.entreri.IndexedDataStore;
import com.lhkbob.entreri.Property;
import com.lhkbob.entreri.property.AbstractPropertyFactory;
import com.lhkbob.entreri.property.DoubleDataStore;

/**
 * ColorRGBProperty is a caching property that wraps a DoubleProperty as a
 * ColorRGB.
 * 
 * @author Michael Ludwig
 */
@Factory(ColorRGBProperty.Factory.class)
public class ColorRGBProperty implements Property {
    private static final int REQUIRED_ELEMENTS = 3;

    private DoubleDataStore data;

    /**
     * Create a new ColorRGBProperty.
     */
    public ColorRGBProperty() {
        data = new DoubleDataStore(REQUIRED_ELEMENTS, new double[REQUIRED_ELEMENTS]);
    }

    /**
     * Get the color of this property, for the component at the given index, and
     * store it into <tt>result</tt>. If result is null, a new ColorRGB is
     * created and returned.
     * 
     * @param index The component index to retrieve
     * @param result The color to store the data for the requested component
     * @return result, or a new ColorRGB if result was null
     */
    public ColorRGB get(int index, ColorRGB result) {
        if (result == null) {
            result = new ColorRGB();
        }

        result.set(data.getArray(), index * REQUIRED_ELEMENTS);
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
        v.getHDR(data.getArray(), index * REQUIRED_ELEMENTS);
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
     * Attribute annotation to apply to ColorRGBProperty declarations.
     * 
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultColor {
        /**
         * @return Default red value in HDR
         */
        double red();

        /**
         * @return Default green value in HDR
         */
        double green();

        /**
         * @return Default blue value in HDR
         */
        double blue();
    }

    /**
     * Default factory implementation for ColorRGBProperties, supports the
     * {@link DefaultColor} annotation to specify the default color.
     * 
     * @author Michael Ludwig
     */
    public static class Factory extends AbstractPropertyFactory<ColorRGBProperty> {
        private final ColorRGB dflt;

        public Factory(Attributes attrs) {
            super(attrs);
            if (attrs.hasAttribute(DefaultColor.class)) {
                DefaultColor v = attrs.getAttribute(DefaultColor.class);
                dflt = new ColorRGB(v.red(), v.green(), v.blue());
            } else {
                dflt = new ColorRGB();
            }
        }

        public Factory(@Const ColorRGB v) {
            super(null);
            dflt = new ColorRGB(v);
        }

        @Override
        public ColorRGBProperty create() {
            return new ColorRGBProperty();
        }

        @Override
        public void setDefaultValue(ColorRGBProperty property, int index) {
            property.set(dflt, index);
        }
    }
}
