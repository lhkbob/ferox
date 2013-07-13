package com.ferox.util.profile;

import java.io.PrintStream;
import java.util.*;

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
        out.print(" count: ");
        out.print(count);
        out.println();

        List<ProfilerData> sorted = new ArrayList<ProfilerData>(children.values());
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
