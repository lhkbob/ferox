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
