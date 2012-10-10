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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

// FIXME: part of me thinks that the thread monitoring should move into a ferox-util
// project (rename of ferox-collections).  Memory reporting of controller types
// can be a simple query method on EntitySystem.  Controller timing could be handled
// by ferox-scene controllers using a ferox-util profiler, they could be timed at
// the top-level only (so need of a stack based system), or I could keep the stack-based
// task profiler in detail in entreri

// I think I will have memory querying in EntitySystem, and automated profiling
// handled by the ControllerManager. The stack profiler isn't really needed.
// Thread monitoring will be cool, but it's not part of entreri
public class ThreadMonitor {
    private static ThreadMXBean tmxb;

    static {
        tmxb = ManagementFactory.getThreadMXBean();
        tmxb.setThreadCpuTimeEnabled(true);
    }

    //

    private String name;
    private long tid;
    private CyclicUsageHistory cpuTimeHistory;
    private CyclicUsageHistory userTimeHistory;
    private CyclicUsageHistory cpuUsageHistory;
    private CyclicUsageHistory userUsageHistory;

    public ThreadMonitor(String name, long tid, int slots) {
        this.tid = tid;
        this.name = name;
        this.cpuTimeHistory = new CyclicUsageHistory(slots);
        this.userTimeHistory = new CyclicUsageHistory(slots);
        this.cpuUsageHistory = new CyclicUsageHistory(slots);
        this.userUsageHistory = new CyclicUsageHistory(slots);

        lastPoll = System.nanoTime();
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return tid;
    }

    private double totalCpuTime;
    private double totalUserTime;
    // FIXME: cpu time includes user time and system time
    // system time = cpu time - user time

    private long lastPoll;

    public double getTotalCpuTime() {
        return this.totalCpuTime;
    }

    public double getTotalUserTime() {
        return this.totalUserTime;
    }

    public void poll() {
        // a time of -1 means not alive
        long now = System.nanoTime();
        double wallTime = (now - lastPoll) / 1000000000.0;
        lastPoll = now;

        double cpuTime = tmxb.getThreadCpuTime(this.tid) / 1000000000.0;
        //        this.totalCpuTime += cpuTime < 0 ? 0 : cpuTime;
        totalCpuTime = cpuTime < 0 ? 0 : cpuTime;
        cpuTimeHistory.log(totalCpuTime);
        cpuUsageHistory.log((cpuTimeHistory.previous(0) - cpuTimeHistory.previous(1)) / wallTime * 100.0);

        double userTime = tmxb.getThreadUserTime(this.tid) / 1000000000.0;
        //        this.totalUserTime += userTime < 0 ? 0 : userTime;
        totalUserTime = userTime < 0 ? 0 : userTime;
        userTimeHistory.log(totalUserTime);
        userUsageHistory.log((userTimeHistory.previous(0) - userTimeHistory.previous(1)) / wallTime * 100.0);

        //        System.out.println("poll: " + name + ", cpu: " + cpuUsageHistory.previous() + ", user: " + userUsageHistory.previous() + ", wall: " + wallClock);
        //        System.out.println("      " + name + ", total cpu: " + cpuTime + ", total user: " + userTime);
        //        usage.log(cpuTime / wallClock * 100.0);
    }

    public CyclicUsageHistory getCpuUsageStats() {
        return this.cpuUsageHistory;
    }

    public CyclicUsageHistory getUserUsageStats() {
        return this.userUsageHistory;
    }

    public CyclicUsageHistory getCpuTimeStats() {
        return this.cpuTimeHistory;
    }

    public CyclicUsageHistory getUserTimeStats() {
        return this.userTimeHistory;
    }
}
