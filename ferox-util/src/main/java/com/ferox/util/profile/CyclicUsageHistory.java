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

public class CyclicUsageHistory {
    private final double[] values;

    public CyclicUsageHistory(int slots) {
        this.values = new double[slots];
    }

    private int addIndex;

    public void log(double value) {
        this.values[this.addIndex++ % this.values.length] = value;
    }

    //

    public double previous() {
        return this.previous(0);
    }

    public double previous(int age) {
        int len = this.values.length;
        return this.values[(((this.addIndex - 1 - age) % len) + len) % len];
    }

    //

    public double max() {
        return this.max(this.values.length);
    }

    public double max(int slots) {
        int count = Math.min(this.values.length, Math.min(slots, this.addIndex - 1));

        double max = 0.0;
        for (int i = 0; i < count; i++) {
            if (this.previous(i) > max) {
                max = this.previous(i);
            }
        }
        return max;
    }

    //

    public double sum() {
        return this.sum(this.values.length);
    }

    public double sum(int slots) {
        int count = Math.min(this.values.length, Math.min(slots, this.addIndex - 1));

        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += this.previous(i);
        }
        return sum;
    }

    //

    public double avg() {
        return this.avg(this.values.length);
    }

    public double avg(int slots) {
        int count = Math.min(this.values.length, Math.min(slots, this.addIndex - 1));

        return this.sum(slots) / count;
    }

    //

    public double nom() {
        return this.nom(this.values.length);
    }

    public double nom(int slots) {
        int count = Math.min(this.values.length, Math.min(slots, this.addIndex - 1));
        if (count == 0) {
            return 0.0;
        }

        double[] arr = new double[count];
        for (int i = 0; i < count; i++) {
            arr[i] = this.previous(i);
        }
        Arrays.sort(arr);
        return arr[arr.length / 2];
    }
}