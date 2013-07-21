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
package com.ferox.scene.task.ffp;

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