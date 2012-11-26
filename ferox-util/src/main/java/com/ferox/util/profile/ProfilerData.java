package com.ferox.util.profile;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProfilerData {
    private final String label;
    private final double avg;
    private final double min;
    private final double max;

    private final int count;

    private final Map<String, ProfilerData> children;

    ProfilerData(String label, double avg, double min, double max, int count,
                 Map<String, ProfilerData> children) {
        this.label = label;
        this.avg = avg;
        this.min = min;
        this.max = max;
        this.count = count;
        this.children = Collections.unmodifiableMap(new HashMap<String, ProfilerData>(children));
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

    public int getInvokeCount() {
        return count;
    }

    public Map<String, ProfilerData> getChildren() {
        return children;
    }

    public void print(PrintStream out) {
        printWithOffset(out, 0);
    }

    private void printWithOffset(PrintStream out, int offset) {
        for (int i = 0; i < offset; i++) {
            out.print(' ');
        }

        out.print(label);
        out.print(" -\tavg: ");
        out.print(formatTime(avg));
        out.print("\tmin: ");
        out.print(formatTime(min));
        out.print("\tmax: ");
        out.print(formatTime(max));
        out.print("\tcount: ");
        out.print(count);
        out.print('\n');

        for (ProfilerData child : children.values()) {
            child.printWithOffset(out, offset + 2);
        }
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
