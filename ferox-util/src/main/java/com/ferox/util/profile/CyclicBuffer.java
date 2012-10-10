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
package com.ferox.util.profile;

import java.util.Arrays;

public class CyclicBuffer {
    private final double[] values;

    private int index;
    private boolean wrappedOnce;

    public CyclicBuffer(int size) {
        values = new double[size];
        index = 0;
        wrappedOnce = false;
    }

    public void log(double value) {
        if (index == values.length) {
            // wrap around
            index = 0;
            wrappedOnce = true;
        }
        values[index++] = value;
    }

    public double first() {
        if (!wrappedOnce) {
            // the first element is clamped to start of array
            return values[0];
        } else {
            // first is 1 past last index, mod the length of the array
            return values[index % values.length];
        }
    }

    public double last() {
        return values[index - 1];
    }

    public double[] values() {
        if (!wrappedOnce) {
            // values are in chronological order
            return Arrays.copyOf(values, index);
        } else {
            // must unwrap array
            double[] ordered = new double[values.length];
            int firstIndex = index % values.length;
            int numFirstSize = values.length - firstIndex;
            System.arraycopy(values, firstIndex, ordered, 0, numFirstSize);
            System.arraycopy(values, 0, ordered, firstIndex, values.length - numFirstSize);
            return ordered;
        }
    }

    public double average() {
        int len = (wrappedOnce ? values.length : index);
        double total = 0;
        for (int i = 0; i < len; i++) {
            total += values[i];
        }
        return total / len;
    }

    public double sum() {
        int len = (wrappedOnce ? values.length : index);
        double total = 0;
        for (int i = 0; i < len; i++) {
            total += values[i];
        }
        return total;
    }

    public double min() {
        int len = (wrappedOnce ? values.length : index);
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < len; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    public double max() {
        int len = (wrappedOnce ? values.length : index);
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < len; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }
}
