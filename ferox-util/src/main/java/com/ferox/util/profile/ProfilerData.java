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

import java.io.PrintStream;
import java.util.*;

public class ProfilerData {
    private final String label;
    private final double avg;
    private final double min;
    private final double max;

    private final double[] histogram;

    private final Map<String, ProfilerData> children;

    ProfilerData(String label, double avg, double min, double max, double[] histogram,
                 Map<String, ProfilerData> children) {
        this.label = label;
        this.avg = avg;
        this.min = min;
        this.max = max;
        this.histogram = histogram;
        this.children = Collections.unmodifiableMap(new HashMap<>(children));
    }

    public String getLabel() {
        return label;
    }

    public double getAverageTime() {
        return avg;
    }

    public double getMinTime() {
        return min;
    }

    public double getMaxTime() {
        return max;
    }

    public double[] getHistogram() {
        return histogram;
    }

    public Map<String, ProfilerData> getChildren() {
        return children;
    }

    public void print(PrintStream out) {
        printWithOffset(out, 0, getLabelColumnWidth(0));
    }

    private void printWithOffset(PrintStream out, int offset, int labelWidth) {
        for (int i = 0; i < offset; i++) {
            out.print(' ');
        }

        out.print(label);

        for (int i = offset + label.length(); i < labelWidth; i++) {
            out.print(' ');
        }

        out.print(" - ");

        for (int i = 0; i < offset; i++) {
            out.print(' ');
        }

        out.print("avg: ");
        out.print(formatTime(avg));
        out.print(" min: ");
        out.print(formatTime(min));
        out.print(" max: ");
        out.print(formatTime(max));
        out.println();

        List<ProfilerData> sorted = new ArrayList<>(children.values());
        Collections.sort(sorted, new Comparator<ProfilerData>() {
            @Override
            public int compare(ProfilerData o1, ProfilerData o2) {
                return Double.compare(o2.avg, o1.avg);
            }
        });

        for (ProfilerData child : sorted) {
            child.printWithOffset(out, offset + 2, labelWidth);
        }
    }

    private int getLabelColumnWidth(int offset) {
        int currentWidth = offset + label.length();
        for (ProfilerData child : children.values()) {
            currentWidth = Math.max(currentWidth, child.getLabelColumnWidth(offset + 2));
        }
        return currentWidth;
    }

    private String formatTime(double time) {
        if (time < 1e-6) {
            // nanoseconds
            return String.format("%.4f ns", (time * 1e9));
        } else if (time < 1) {
            // milliseconds
            return String.format("%.4f ms", (time * 1e3));
        } else {
            // seconds
            return String.format("%.4f s", time);
        }
    }
}
