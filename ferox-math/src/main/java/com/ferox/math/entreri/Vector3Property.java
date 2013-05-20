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
import com.lhkbob.entreri.property.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Vector3Property is a caching property that wraps a DoubleProperty as a Vector3.
 *
 * @author Michael Ludwig
 */
@Factory(Vector3Property.Factory.class)
public class Vector3Property implements ShareableProperty<Vector3> {
    private static final int REQUIRED_ELEMENTS = 3;

    private double[] data;

    /**
     * Create a new Vector3Property.
     */
    public Vector3Property() {
        data = new double[REQUIRED_ELEMENTS];
    }

    @Override
    public Vector3 createShareableInstance() {
        return new Vector3();
    }

    @Override
    public void get(int index, Vector3 result) {
        result.set(data, index * REQUIRED_ELEMENTS);
    }

    public void set(int index, @Const Vector3 v) {
        v.get(data, index * REQUIRED_ELEMENTS);
    }

    public Vector3 get(int index) {
        Vector3 v = new Vector3();
        get(index, v);
        return v;
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
    public static class Factory implements PropertyFactory<Vector3Property> {
        private final Vector3 dflt;
        private final boolean disableClone;

        public Factory(Attributes attrs) {
            if (attrs.hasAttribute(DefaultVector3.class)) {
                DefaultVector3 v = attrs.getAttribute(DefaultVector3.class);
                dflt = new Vector3(v.x(), v.y(), v.z());
            } else {
                dflt = new Vector3();
            }

            disableClone = attrs.hasAttribute(Clone.class) &&
                           attrs.getAttribute(Clone.class).value() ==
                           Clone.Policy.DISABLE;
        }

        public Factory(@Const Vector3 v) {
            dflt = new Vector3(v);
            disableClone = false;
        }

        @Override
        public Vector3Property create() {
            return new Vector3Property();
        }

        @Override
        public void setDefaultValue(Vector3Property property, int index) {
            property.set(index, dflt);
        }

        @Override
        public void clone(Vector3Property src, int srcIndex, Vector3Property dst,
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
