package com.ferox.util.profile;

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
}
