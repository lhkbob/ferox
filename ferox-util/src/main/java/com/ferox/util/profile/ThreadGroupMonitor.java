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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThreadGroupMonitor {
    public ThreadGroupMonitor() {
        this(Thread.currentThread().getThreadGroup());
    }

    public ThreadGroupMonitor(ThreadGroup group) {
        this.group = group;
        this.lastThreadIds = new long[0];
        this.aliveId2mon = new HashMap<Long, ThreadMonitor>();
        this.deadId2mon = new HashMap<Long, ThreadMonitor>();
    }

    //

    private final ThreadGroup group;

    public ThreadGroup getThreadGroup() {
        return group;
    }

    //

    private int totalDeadThreadCount = 0;

    public synchronized int getTotalDeadThreadCount() {
        return this.totalDeadThreadCount;
    }

    //

    private int regularThreadCount = 0;

    public synchronized int getRegularThreadCount() {
        return this.regularThreadCount;
    }

    //

    private int deamonThreadCount = 0;

    public synchronized int getDeamonThreadCount() {
        return this.deamonThreadCount;
    }

    //

    private static final int default_slots = 100;

    private long[] lastThreadIds;
    private final Map<Long, ThreadMonitor> aliveId2mon;
    private final Map<Long, ThreadMonitor> deadId2mon;

    public synchronized void poll() {
        Thread[] threads = this.findAllThreads();

        long[] currThreadIds = this.findAllThreadIds(threads);
        long[] newIds = this.findNewThreadIds(this.lastThreadIds, currThreadIds);
        long[] deadIds = this.findDeadThreadIds(this.lastThreadIds, currThreadIds);

        this.totalDeadThreadCount += deadIds.length;

        for (long newId : newIds) {
            String name = "Unnamed(" + newId + ")";
            for (Thread t : threads) {
                if (t.getId() == newId) {
                    name = t.getName();
                }
            }
            aliveId2mon.put(Long.valueOf(newId), new ThreadMonitor(name,
                                                                   newId,
                                                                   default_slots));
        }
        for (long deadId : deadIds) {
            deadId2mon.put(Long.valueOf(deadId), aliveId2mon.remove(Long.valueOf(deadId)));
        }

        for (ThreadMonitor mon : aliveId2mon.values()) {
            mon.poll();
        }
        for (ThreadMonitor mon : deadId2mon.values()) {
            mon.poll();
        }

        this.analyzeThreads(threads);

        this.lastThreadIds = currThreadIds;
    }

    public synchronized double getAvgCpuTimeStats() {
        double sum = 0.0;
        for (ThreadMonitor mon : aliveId2mon.values()) {
            sum += mon.getCpuTimeStats().average();
        }
        return sum;
    }

    public synchronized double getAvgUserTimeStats() {
        double sum = 0.0;
        for (ThreadMonitor mon : aliveId2mon.values()) {
            sum += mon.getUserTimeStats().average();
        }
        return sum;
    }

    public Collection<ThreadMonitor> getAliveThreadMonitors() {
        return Collections.unmodifiableCollection(this.aliveId2mon.values());
    }

    public Collection<ThreadMonitor> getDeadThreadMonitors() {
        return Collections.unmodifiableCollection(this.deadId2mon.values());
    }

    private void analyzeThreads(Thread[] threads) {
        int deamonThreadCount = 0;
        int regularThreadCount = 0;

        for (Thread thread : threads) {
            if (!thread.isAlive()) {
                continue;
            }
            if (thread.isDaemon()) {
                deamonThreadCount++;
            } else {
                regularThreadCount++;
            }
        }

        this.deamonThreadCount = deamonThreadCount;
        this.regularThreadCount = regularThreadCount;
    }

    public Thread[] findAllThreads() {
        int threadCount;

        Thread[] tempThreadArray = new Thread[8];
        while ((threadCount = this.group.enumerate(tempThreadArray)) == tempThreadArray.length) {
            tempThreadArray = Arrays.copyOf(tempThreadArray, tempThreadArray.length * 2);
        }

        Thread[] threadArray = new Thread[threadCount];
        System.arraycopy(tempThreadArray, 0, threadArray, 0, threadCount);
        return threadArray;
    }

    private long[] findAllThreadIds(Thread[] threads) {
        long[] allThreadIds = new long[threads.length];
        for (int i = 0; i < allThreadIds.length; i++) {
            allThreadIds[i] = threads[i].getId();
        }
        return allThreadIds;
    }

    private long[] findNewThreadIds(long[] lastThreads, long[] currThreads) {
        long[] newThreadIds = new long[currThreads.length];
        int newThreadIndex = 0;

        outer: for (int i = 0; i < currThreads.length; i++) {
            for (int k = 0; k < lastThreads.length; k++) {
                if (currThreads[i] == lastThreads[k]) {
                    continue outer;
                }
            }
            newThreadIds[newThreadIndex++] = currThreads[i];
        }

        long[] ids = new long[newThreadIndex];
        System.arraycopy(newThreadIds, 0, ids, 0, newThreadIndex);
        return ids;
    }

    private long[] findDeadThreadIds(long[] lastThreads, long[] currThreads) {
        long[] deadThreadIds = new long[lastThreads.length];
        int deadThreadIndex = 0;

        outer: for (int i = 0; i < lastThreads.length; i++) {
            for (int k = 0; k < currThreads.length; k++) {
                if (lastThreads[i] == currThreads[k]) {
                    continue outer;
                }
            }
            deadThreadIds[deadThreadIndex++] = lastThreads[i];
        }

        long[] ids = new long[deadThreadIndex];
        System.arraycopy(deadThreadIds, 0, ids, 0, deadThreadIndex);
        return ids;
    }
}
