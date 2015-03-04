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
import com.ferox.math.ColorRGB;
import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.lhkbob.entreri.property.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * AxisAlignedBoxProperty is a value-semantics Property for handling AxisAlignedBox instances.
 * Internally it packs them into a single array for more efficient storage.
 *
 * @author Michael Ludwig
 */
public class AxisAlignedBoxProperty implements Property<AxisAlignedBoxProperty>, Property.ValueSemantics {
    private static final int REQUIRED_ELEMENTS = 6;
    private static final int OFFSET = 3;

    private final AxisAlignedBox dflt;
    private final boolean clone;
    private double[] data;

    /**
     * Create a new AxisAlignedBoxProperty.
     */
    public AxisAlignedBoxProperty() {
        this(new AxisAlignedBox(), true);
    }

    /**
     * Create a new AxisAlignedBoxProperty with the selected default and clone policy.
     * @param dflt The default aabb
     * @param clone True if the property clones the value
     */
    public AxisAlignedBoxProperty(AxisAlignedBox dflt, boolean clone) {
        this.dflt = new AxisAlignedBox(dflt);
        this.clone = clone;
        data = new double[REQUIRED_ELEMENTS];
    }

    /**
     * Constructor suitable for code generation with entreri.
     * @param dfltMin
     * @param dfltMax
     * @param clonePolicy
     */
    public AxisAlignedBoxProperty(DefaultMin dfltMin, DefaultMax dfltMax, DoNotClone clonePolicy) {
        this(new AxisAlignedBox((dfltMin == null ? new Vector3() : new Vector3(dfltMin.x(), dfltMin.y(), dfltMin.z())),
                (dfltMax == null ? new Vector3() : new Vector3(dfltMax.x(), dfltMax.y(), dfltMax.z()))), clonePolicy == null);
    }

    public void get(int index, AxisAlignedBox result) {
        result.min.set(data, index * REQUIRED_ELEMENTS);
        result.max.set(data, index * REQUIRED_ELEMENTS + OFFSET);
    }

    public void set(int index, @Const AxisAlignedBox b) {
        b.min.get(data, index * REQUIRED_ELEMENTS);
        b.max.get(data, index * REQUIRED_ELEMENTS + OFFSET);
    }

    public AxisAlignedBox get(int index) {
        AxisAlignedBox a = new AxisAlignedBox();
        get(index, a);
        return a;
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
    public void clone(AxisAlignedBoxProperty src, int srcIndex, int dstIndex) {
        if (!src.clone || !clone) {
            setDefaultValue(dstIndex);
        } else {
            System.arraycopy(src.data, srcIndex * REQUIRED_ELEMENTS, data, dstIndex * REQUIRED_ELEMENTS, REQUIRED_ELEMENTS);
        }
    }

    /**
     * Attribute annotation to apply to AxisAlignedBoxProperty declarations, to specify the minimum coordinate
     * of the box.
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
     * Attribute annotation to apply to AxisAlignedBoxProperty declarations, to specify the maximum coordinate
     * of the box.
     *
     * @author Michael Ludwig
     */
    @Attribute
    @Target(ElementType.METHOD)
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
}
