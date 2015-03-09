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

import java.lang.annotation.*;
import java.util.Arrays;

/**
 * Vector3Property is a value-semantics Property for handling Vector3 instances.
 * Internally it packs them into a single array for more efficient storage.
 *
 * @author Michael Ludwig
 */
public class Vector3Property implements Property<Vector3Property>, Property.ValueSemantics {
    private static final int REQUIRED_ELEMENTS = 3;

    private final Vector3 dflt;
    private final boolean clone;

    private double[] data;

    /**
     * Create a new Vector3Property using the 0 vector as its default.
     */
    public Vector3Property() {
        this(new Vector3(), true);
    }

    /**
     * Create a new Vector3Property with the selected default and clone policy.
     * @param dflt The default vector
     * @param clone True if the property clones the value
     */
    public Vector3Property(Vector3 dflt, boolean clone) {
        this.dflt = new Vector3(dflt);
        this.clone = clone;
        data = new double[REQUIRED_ELEMENTS];
    }

    /**
     * Constructor suitable for code generation with entreri.
     * @param dflt
     * @param clonePolicy
     */
    public Vector3Property(DefaultVector3 dflt, DoNotClone clonePolicy) {
        this((dflt == null ? new Vector3() : new Vector3(dflt.x(), dflt.y(), dflt.z())), clonePolicy == null);
    }

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

    @Override
    public void setDefaultValue(int index) {
        set(index, dflt);
    }

    @Override
    public void clone(Vector3Property src, int srcIndex, int dstIndex) {
        if (!src.clone || !clone) {
            setDefaultValue(dstIndex);
        } else {
            System.arraycopy(src.data, srcIndex * REQUIRED_ELEMENTS, data, dstIndex * REQUIRED_ELEMENTS, REQUIRED_ELEMENTS);
        }
    }

    /**
     * Attribute annotation to apply to Vector3Property declarations.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Documented
    @Target(ElementType.METHOD)
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
}
