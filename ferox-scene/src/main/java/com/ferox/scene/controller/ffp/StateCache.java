package com.ferox.scene.controller.ffp;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StateCache<T> {
    private T[] states;
    private int[] usage;
    private Map<T, Integer> lookup;

    @SuppressWarnings("unchecked")
    public StateCache(Class<T> type) {
        states = (T[]) Array.newInstance(type, 0);
        usage = new int[0];
        lookup = new HashMap<T, Integer>();
    }

    public int getStateIndex(T newState, int oldIndex) {
        Integer index = lookup.get(newState);
        if (index == null) {
            // must form a new state
            index = states.length;
            states = Arrays.copyOf(states, states.length + 1);
            usage = Arrays.copyOf(usage, usage.length + 1);
            states[index] = newState;
            lookup.put(newState, index);
        }

        // update usage
        if (oldIndex >= 0) {
            usage[oldIndex]--;
        }
        usage[index]++;
        return index;
    }

    public int count() {
        return states.length;
    }

    public T getState(int index) {
        return states[index];
    }

    public boolean needsReset() {
        int empty = 0;
        for (int i = 0; i < usage.length; i++) {
            if (usage[i] == 0) {
                empty++;
            }
        }
        return empty / (double) usage.length > .5;
    }

    @SuppressWarnings("unchecked")
    public void reset() {
        states = (T[]) Array.newInstance(states.getClass().getComponentType(), 0);
        usage = new int[0];
        lookup = new HashMap<T, Integer>();
    }
}