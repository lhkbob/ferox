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
import com.ferox.math.Matrix4;
import com.lhkbob.entreri.property.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Matrix4Property is a caching property that wraps a DoubleProperty as a Matrix4.
 *
 * @author Michael Ludwig
 */
@Factory(Matrix4Property.Factory.class)
public class Matrix4Property implements ShareableProperty<Matrix4> {
    private static final int REQUIRED_ELEMENTS = 16;

    private double[] data;

    /**
     * Create a new Matrix4Property.
     */
    public Matrix4Property() {
        data = new double[REQUIRED_ELEMENTS];
    }

    @Override
    public void get(int index, Matrix4 result) {
        result.set(data, index * REQUIRED_ELEMENTS, false);
    }


    public void set(int index, @Const Matrix4 v) {
        v.get(data, index * REQUIRED_ELEMENTS, false);
    }

    public Matrix4 get(int index) {
        Matrix4 m = new Matrix4();
        get(index, m);
        return m;
    }

    @Override
    public Matrix4 createShareableInstance() {
        return new Matrix4();
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
     * Default factory implementation for Matrix4Properties, supports the {@link
     * DefaultMatrix4} annotation to specify the default matrix coordinates.
     *
     * @author Michael Ludwig
     */
    public static class Factory implements PropertyFactory<Matrix4Property> {
        private final Matrix4 dflt;
        private final boolean disableClone;

        public Factory(Attributes attrs) {
            if (attrs.hasAttribute(DefaultMatrix4.class)) {
                DefaultMatrix4 v = attrs.getAttribute(DefaultMatrix4.class);
                dflt = new Matrix4(v.m00(), v.m01(), v.m02(), v.m03(), v.m10(), v.m11(),
                                   v.m12(), v.m13(), v.m20(), v.m21(), v.m22(), v.m23(),
                                   v.m30(), v.m31(), v.m32(), v.m33());
            } else {
                dflt = new Matrix4();
            }

            disableClone = attrs.hasAttribute(Clone.class) &&
                           attrs.getAttribute(Clone.class).value() ==
                           Clone.Policy.DISABLE;
        }

        public Factory(@Const Matrix4 v) {
            dflt = new Matrix4(v);
            disableClone = false;
        }

        @Override
        public Matrix4Property create() {
            return new Matrix4Property();
        }

        @Override
        public void setDefaultValue(Matrix4Property property, int index) {
            property.set(index, dflt);
        }

        @Override
        public void clone(Matrix4Property src, int srcIndex, Matrix4Property dst,
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
