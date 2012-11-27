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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class Profiler {
    private static final ThreadLocal<Profiler> profilers = new ThreadLocal<Profiler>() {
        @Override
        protected Profiler initialValue() {
            return new Profiler();
        }
    };

    private final Map<String, ProfileRecord> rootRecords;
    private ProfileRecord currentRecord;

    private Profiler() {
        rootRecords = new HashMap<String, ProfileRecord>();
        currentRecord = null;
    }

    // FIXME for most utility this should expose/clone the cyclic buffer so
    // in depth histograms of performance can be presented
    private ProfilerData getProfilerData() {
        Map<String, ProfilerData> roots = getData(rootRecords);
        double avg = 0;
        double min = 0;
        double max = 0;

        // FIXME need to actually measure proper root data
        for (ProfilerData data : roots.values()) {
            avg += data.getAverageTime();
            min += data.getMinTime();
            max += data.getMaxTime();
        }

        return new ProfilerData("root", avg, min, max, -1, roots);
    }

    private ProfilerData getData(ProfileRecord record) {
        Map<String, ProfilerData> children = getData(record.children);
        return new ProfilerData(record.label,
                                record.timings.average(),
                                record.timings.min(),
                                record.timings.max(),
                                record.invokeCount,
                                children);
    }

    private Map<String, ProfilerData> getData(Map<String, ProfileRecord> records) {
        Map<String, ProfilerData> data = new HashMap<String, ProfilerData>();
        for (Entry<String, ProfileRecord> r : records.entrySet()) {
            data.put(r.getKey(), getData(r.getValue()));
        }
        return data;
    }

    private void pushImpl(String label) {
        if (label == null) {
            throw new NullPointerException("Label cannot be null");
        }

        if (currentRecord == null) {
            // root record
            currentRecord = rootRecords.get(label);
            if (currentRecord == null) {
                currentRecord = new ProfileRecord(label, null);
                rootRecords.put(label, currentRecord);
            }
        } else {
            ProfileRecord next = currentRecord.children.get(label);
            if (next == null) {
                // new label
                next = new ProfileRecord(label, currentRecord);
                currentRecord.children.put(label, next);
            }
            currentRecord = next;
        }

        currentRecord.startTime = System.nanoTime();
        currentRecord.invokeCount++;
    }

    private void popImpl() {
        if (currentRecord == null) {
            throw new IllegalStateException("No record to pop off");
        }

        currentRecord.timings.log((System.nanoTime() - currentRecord.startTime) / 1e9);
        currentRecord = currentRecord.parent;
    }

    public static void push(String label) {
        Profiler profiler = profilers.get();
        profiler.pushImpl(label);
    }

    public static void pop() {
        Profiler profiler = profilers.get();
        profiler.popImpl();
    }

    // FIXME there really needs to be a thread-safe way of getting the data
    // from another thread
    public static ProfilerData getDataSnapshot() {
        Profiler profiler = profilers.get();
        return profiler.getProfilerData();
    }

    private static class ProfileRecord {
        private final String label;
        private final CyclicBuffer timings;
        private int invokeCount;

        private final Map<String, ProfileRecord> children;
        private final ProfileRecord parent;

        private long startTime;

        private ProfileRecord(String label, ProfileRecord parent) {
            this.label = label;
            this.parent = parent;

            timings = new CyclicBuffer(10);
            invokeCount = 0;
            children = new HashMap<String, ProfileRecord>();
        }
    }
}
