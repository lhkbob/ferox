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
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.lhkbob.entreri.property.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Matrix3Property is a value-semantics Property for handling Matrix3 instances.
 * Internally it packs them into a single array for more efficient storage.
 *
 * @author Michael Ludwig
 */
public class Matrix3Property implements Property<Matrix3Property>, Property.ValueSemantics {
    private static final int REQUIRED_ELEMENTS = 9;

    private final Matrix3 dflt;
    private final boolean clone;
    private double[] data;

    /**
     * Create a new Matrix3Property.
     */
    public Matrix3Property() {
        this(new Matrix3(), true);
    }

    /**
     * Create a new Matrix3Property with the selected default and clone policy.
     * @param dflt The default matrix
     * @param clone True if the property clones the value
     */
    public Matrix3Property(Matrix3 dflt, boolean clone) {
        this.dflt = new Matrix3(dflt);
        this.clone = clone;
        data = new double[REQUIRED_ELEMENTS];
    }

    /**
     * Constructor suitable for code generation with entreri.
     * @param dflt
     * @param clonePolicy
     */
    public Matrix3Property(DefaultMatrix3 dflt, DoNotClone clonePolicy) {
        this((dflt == null ? new Matrix3() : new Matrix3(dflt.m00(), dflt.m01(), dflt.m02(),
                dflt.m10(), dflt.m11(), dflt.m12(), dflt.m20(), dflt.m21(), dflt.m22())), clonePolicy == null);
    }

    public void get(int index, Matrix3 result) {
        result.set(data, index * REQUIRED_ELEMENTS, false);
    }

    public void set(int index, @Const Matrix3 v) {
        v.get(data, index * REQUIRED_ELEMENTS, false);
    }

    public Matrix3 get(int index) {
        Matrix3 m = new Matrix3();
        get(index, m);
        return m;
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

    @Override
    public void setDefaultValue(int index) {
        set(index, dflt);
    }

    @Override
    public void clone(Matrix3Property src, int srcIndex, int dstIndex) {
        if (!src.clone || !clone) {
            setDefaultValue(dstIndex);
        } else {
            System.arraycopy(src.data, srcIndex * REQUIRED_ELEMENTS, data, dstIndex * REQUIRED_ELEMENTS, REQUIRED_ELEMENTS);
        }
    }

    /**
     * Attribute annotation to apply to Matrix3Property declarations.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface DefaultMatrix3 {
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
    }
}
