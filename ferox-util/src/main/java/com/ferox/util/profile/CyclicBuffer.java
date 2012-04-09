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
            if (values[i] < min)
                min = values[i];
        }
        return min;
    }
    
    public double max() {
        int len = (wrappedOnce ? values.length : index);
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < len; i++) {
            if (values[i] > max)
                max = values[i];
        }
        return max;
    }
}
