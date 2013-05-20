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
import com.lhkbob.entreri.property.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * AxisAlignedBoxProperty is a property that wraps a {@link DoubleProperty} as a
 * AxisAlginedBox.
 *
 * @author Michael Ludwig
 */
@Factory(AxisAlignedBoxProperty.Factory.class)
public class AxisAlignedBoxProperty implements ShareableProperty<AxisAlignedBox> {
    private static final int REQUIRED_ELEMENTS = 6;
    private static final int OFFSET = 3;

    private double[] data;

    /**
     * Create a new AxisAlignedBoxProperty.
     */
    public AxisAlignedBoxProperty() {
        data = new double[REQUIRED_ELEMENTS];
    }

    @Override
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
    public AxisAlignedBox createShareableInstance() {
        return new AxisAlignedBox();
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
    public static class Factory implements PropertyFactory<AxisAlignedBoxProperty> {
        private final AxisAlignedBox dflt;
        private final boolean disableClone;

        public Factory(Attributes attrs) {
            dflt = new AxisAlignedBox();

            if (attrs.hasAttribute(DefaultMin.class)) {
                DefaultMin min = attrs.getAttribute(DefaultMin.class);
                dflt.min.set(min.x(), min.y(), min.z());
            }

            if (attrs.hasAttribute(DefaultMax.class)) {
                DefaultMax max = attrs.getAttribute(DefaultMax.class);
                dflt.min.set(max.x(), max.y(), max.z());
            }

            disableClone = attrs.hasAttribute(Clone.class) &&
                           attrs.getAttribute(Clone.class).value() ==
                           Clone.Policy.DISABLE;
        }

        public Factory(@Const AxisAlignedBox v) {
            dflt = new AxisAlignedBox(v);
            disableClone = false;
        }

        @Override
        public AxisAlignedBoxProperty create() {
            return new AxisAlignedBoxProperty();
        }

        @Override
        public void setDefaultValue(AxisAlignedBoxProperty property, int index) {
            property.set(index, dflt);
        }

        @Override
        public void clone(AxisAlignedBoxProperty src, int srcIndex,
                          AxisAlignedBoxProperty dst, int dstIndex) {
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
