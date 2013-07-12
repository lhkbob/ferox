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

import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.lhkbob.entreri.property.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * ColorRGBProperty is a caching property that wraps a DoubleProperty as a ColorRGB.
 *
 * @author Michael Ludwig
 */
@Factory(ColorRGBProperty.Factory.class)
public class ColorRGBProperty implements ShareableProperty<ColorRGB> {
    private static final int REQUIRED_ELEMENTS = 3;

    private double[] data;

    /**
     * Create a new ColorRGBProperty.
     */
    public ColorRGBProperty() {
        data = new double[REQUIRED_ELEMENTS];
    }

    @Override
    public void get(int index, ColorRGB result) {
        result.set(data, index * REQUIRED_ELEMENTS);
    }

    public void set(int index, @Const ColorRGB v) {
        v.getHDR(data, index * REQUIRED_ELEMENTS);
    }

    public ColorRGB get(int index) {
        ColorRGB c = new ColorRGB();
        get(index, c);
        return c;
    }

    @Override
    public ColorRGB createShareableInstance() {
        return new ColorRGB();
    }

    @Override
    public void setCapacity(int size) {
        data = Arrays.copyOf(data, size * REQUIRED_ELEMENTS);
    }

    @Override
    public int getCapacity() {
        return data.length / REQUIRED_ELEMENTS;
    }

    @Override
    public void swap(int indexA, int indexB) {
        int ia = indexA * REQUIRED_ELEMENTS;
        int ib = indexB * REQUIRED_ELEMENTS;

        for (int i = 0; i < REQUIRED_ELEMENTS; i++) {
            double t = data[ia + i];
            data[ia + i] = data[ib + i];
            data[ib + i] = t;
        }
    }

    /**
     * Attribute annotation to apply to ColorRGBProperty declarations.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.METHOD)
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
     * Default factory implementation for ColorRGBProperties, supports the {@link
     * DefaultColor} annotation to specify the default color.
     *
     * @author Michael Ludwig
     */
    public static class Factory implements PropertyFactory<ColorRGBProperty> {
        private final ColorRGB dflt;
        private final boolean disableClone;

        public Factory(Attributes attrs) {
            if (attrs.hasAttribute(DefaultColor.class)) {
                DefaultColor v = attrs.getAttribute(DefaultColor.class);
                dflt = new ColorRGB(v.red(), v.green(), v.blue());
            } else {
                dflt = new ColorRGB();
            }

            disableClone = attrs.hasAttribute(Clone.class) &&
                           attrs.getAttribute(Clone.class).value() ==
                           Clone.Policy.DISABLE;
        }

        public Factory(@Const ColorRGB v) {
            dflt = new ColorRGB(v);
            disableClone = false;
        }

        @Override
        public ColorRGBProperty create() {
            return new ColorRGBProperty();
        }

        @Override
        public void setDefaultValue(ColorRGBProperty property, int index) {
            property.set(index, dflt);
        }

        @Override
        public void clone(ColorRGBProperty src, int srcIndex, ColorRGBProperty dst,
                          int dstIndex) {
            if (disableClone) {
                setDefaultValue(dst, dstIndex);
            } else {
                int ia = srcIndex * REQUIRED_ELEMENTS;
                int ib = dstIndex * REQUIRED_ELEMENTS;

                System.arraycopy(src.data, ia, dst.data, ib, REQUIRED_ELEMENTS);
            }
        }
    }
}
