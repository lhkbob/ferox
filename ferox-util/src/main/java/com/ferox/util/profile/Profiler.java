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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class Profiler {
    private static final int BUFFER_LENGTH = 100;

    private static final ThreadLocal<ThreadProfile> profiles = new ThreadLocal<>();
    private static final ConcurrentHashMap<Thread, ProfileRoot> roots = new ConcurrentHashMap<>();

    public static void push(String label) {
        ThreadProfile p = profiles.get();
        if (p == null) {
            roots.put(Thread.currentThread(), new ProfileRoot());
            p = new ThreadProfile();
            profiles.set(p);
        }
        p.push(label);
    }

    public static void pop() {
        ThreadProfile p = profiles.get();
        if (p == null) {
            throw new IllegalStateException("Mismatched pop");
        }
        p.pop();
    }

    public static Map<Thread, ProfilerData> getDataSnapshot() {
        Map<Thread, ProfilerData> data = new HashMap<>();
        Iterator<Map.Entry<Thread, ProfileRoot>> threadData = roots.entrySet().iterator();
        while (threadData.hasNext()) {
            Map.Entry<Thread, ProfileRoot> k = threadData.next();
            if (k.getKey().isAlive()) {
                data.put(k.getKey(), k.getValue().getSnapshot());
            } else {
                threadData.remove();
            }
        }
        return data;
    }

    private static class ProfileRoot {
        private final Map<String, ProfileNode> roots;

        private final CyclicBuffer totalTimes;

        public ProfileRoot() {
            roots = new HashMap<>();
            totalTimes = new CyclicBuffer(BUFFER_LENGTH);
        }

        public ProfilerData getSnapshot() {
            // compute children
            int histogramLength = 0;
            Map<String, ProfilerData> children = new HashMap<>();
            List<double[]> histograms = new ArrayList<>();
            for (Map.Entry<String, ProfileNode> c : roots.entrySet()) {
                ProfilerData data = c.getValue().getSnapshot();
                histogramLength = data.getHistogram().length; // every root should have the same length
                histograms.add(data.getHistogram());

                children.put(c.getKey(), data);
            }

            // combine children into total histogram
            totalTimes.clear();
            for (int i = 0; i < histogramLength; i++) {
                double sum = 0;
                for (double[] h : histograms) {
                    sum += h[i];
                }
                totalTimes.log(sum);
            }

            return new ProfilerData("root", totalTimes.average(), totalTimes.min(), totalTimes.max(),
                                    totalTimes.values(), children);
        }
    }

    private static class ProfileNode {
        private final String label;
        private final CyclicBuffer records;

        private final ProfileNode parent;
        private final Map<String, ProfileNode> children;

        // temporary map index to build up the profile tree from the flat thread profile data
        private int reverseTreeIndex;
        // temporary time that is later logged once the given record has been accumulated
        private long totalTime;

        public ProfileNode(String label, ProfileNode parent) {
            this.parent = parent;
            this.label = label;
            records = new CyclicBuffer(BUFFER_LENGTH);
            children = new HashMap<>(); // FIXME does this need to be thread safe?
        }

        public ProfilerData getSnapshot() {
            Map<String, ProfilerData> children = new HashMap<>();
            for (Map.Entry<String, ProfileNode> c : this.children.entrySet()) {
                children.put(c.getKey(), c.getValue().getSnapshot());
            }
            return new ProfilerData(label, records.average(), records.min(), records.max(), records.values(),
                                    children);
        }

        public void updateRecord() {
            // the way in which ThreadProfile invokes this method guarantees that for a root tree,
            // every node's record histogram is in sync. Multiple visits are merged and lack of a visit
            // is marked with a time of 0
            records.log(totalTime / 1e9);
            for (ProfileNode c : children.values()) {
                c.updateRecord();
            }
            reverseTreeIndex = -1;
            totalTime = 0;
        }
    }

    private static class ThreadProfile {
        private String[] records;
        private long[] recordStart;
        private long[] recordEnd;
        private int[] parentIndex;

        private int writeIndex;
        private int parent;

        public ThreadProfile() {
            records = new String[20];
            recordStart = new long[20];
            recordEnd = new long[20];
            parentIndex = new int[20];

            writeIndex = 0;
            parent = -1;
        }

        public void push(String label) {
            int idx = writeIndex++;
            if (idx >= records.length) {
                // grow records, hopefully this doesn't happen too often
                records = Arrays.copyOf(records, idx + 20);
                recordStart = Arrays.copyOf(recordStart, idx + 20);
                recordEnd = Arrays.copyOf(recordEnd, idx + 20);
                parentIndex = Arrays.copyOf(parentIndex, idx + 20);
            }

            records[idx] = label;
            recordStart[idx] = System.nanoTime();
            recordEnd[idx] = -1L; // not finished yet
            parentIndex[idx] = parent;
            parent = idx;
        }

        public void pop() {
            if (parent < 0) {
                throw new IllegalStateException("Mismatched pop");
            }

            // parent contains the index of the last pushed label that hasn't been popped yet
            recordEnd[parent] = System.nanoTime();
            parent = parentIndex[parent];

            if (parent < 0) {
                // report the accumulated timings
                ProfileRoot root = roots.get(Thread.currentThread());
                ProfileNode rootNode = null;
                ProfileNode prev = null;
                for (int i = 0; i < writeIndex; i++) {
                    while (prev != null && prev.reverseTreeIndex != parentIndex[i]) {
                        prev = prev.parent;
                    }

                    ProfileNode current;
                    if (prev == null) {
                        current = root.roots.get(records[i]);
                        if (current == null) {
                            current = new ProfileNode(records[i], null);
                            root.roots.put(records[i], current);
                        }
                        // record the root node for later use
                        rootNode = current;
                    } else {
                        current = prev.children.get(records[i]);
                        if (current == null) {
                            current = new ProfileNode(records[i], prev);
                            prev.children.put(records[i], current);
                        }
                    }

                    current.reverseTreeIndex = i;
                    current.totalTime += (recordEnd[i] - recordStart[i]);
                    prev = current;
                }

                // now log the accumulated times into the cyclic buffers, which lets us keep nodes that weren't
                // visited in sync and merge nodes that were visited multiple times into a single time block
                if (rootNode != null) {
                    rootNode.updateRecord();
                } else {
                    throw new RuntimeException("Shouldn't happen!");
                }

                writeIndex = 0;
                parent = -1;
            }
        }
    }

}
