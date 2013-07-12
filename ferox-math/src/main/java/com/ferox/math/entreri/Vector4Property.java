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
import com.lhkbob.entreri.property.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Vector4Property is a caching property that wraps a DoubleProperty as a Vector4.
 *
 * @author Michael Ludwig
 */
@Factory(Vector4Property.Factory.class)
public class Vector4Property implements ShareableProperty<Vector4> {
    private static final int REQUIRED_ELEMENTS = 4;

    private double[] data;

    /**
     * Create a new Vector4Property.
     */
    public Vector4Property() {
        data = new double[REQUIRED_ELEMENTS];
    }

    @Override
    public void get(int index, Vector4 result) {
        result.set(data, index * REQUIRED_ELEMENTS);
    }

    public void set(int index, @Const Vector4 v) {
        v.get(data, index * REQUIRED_ELEMENTS);
    }

    public Vector4 get(int index) {
        Vector4 v = new Vector4();
        get(index, v);
        return v;
    }

    @Override
    public Vector4 createShareableInstance() {
        return new Vector4();
    }

    @Override
    public int getCapacity() {
        return data.length / REQUIRED_ELEMENTS;
    }

    @Override
    public void setCapacity(int size) {
        data = Arrays.copyOf(data, size * REQUIRED_ELEMENTS);
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
     * Attribute annotation to apply to Vector4Property declarations.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.METHOD)
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
    public static class Factory implements PropertyFactory<Vector4Property> {
        private final Vector4 dflt;
        private final boolean disableClone;

        public Factory(Attributes attrs) {
            if (attrs.hasAttribute(DefaultVector4.class)) {
                DefaultVector4 v = attrs.getAttribute(DefaultVector4.class);
                dflt = new Vector4(v.x(), v.y(), v.z(), v.w());
            } else {
                dflt = new Vector4();
            }

            disableClone = attrs.hasAttribute(Clone.class) &&
                           attrs.getAttribute(Clone.class).value() ==
                           Clone.Policy.DISABLE;
        }

        public Factory(@Const Vector4 v) {
            dflt = new Vector4(v);
            disableClone = false;
        }

        @Override
        public Vector4Property create() {
            return new Vector4Property();
        }

        @Override
        public void setDefaultValue(Vector4Property property, int index) {
            property.set(index, dflt);
        }

        @Override
        public void clone(Vector4Property src, int srcIndex, Vector4Property dst,
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
