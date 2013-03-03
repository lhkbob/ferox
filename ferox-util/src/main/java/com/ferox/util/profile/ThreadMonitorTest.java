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

import java.util.Random;

public class ThreadMonitorTest {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 8; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Random r = new Random();
                    double total = 0;
                    while (true) {
                        double a = r.nextDouble();
                        double b = r.nextDouble();
                        total += (a * b);
                    }
                }
            }, "Task " + i).start();
        }

        ThreadGroupMonitor gmonitor = new ThreadGroupMonitor();

        long start = System.currentTimeMillis();
        long veryStart = System.currentTimeMillis();
        while (true) {
            gmonitor.poll();

            if ((System.currentTimeMillis() - start) > 1000) {
                System.out.println("-----------------------");
                System.out.println("Estimated run time: " +
                                   ((System.currentTimeMillis() - veryStart) / 1000.0));
                for (ThreadMonitor tmon : gmonitor.getAliveThreadMonitors()) {
                    //              double avg = tmon.getCpuTimeStats().avg();  // avg of last polls
                    //              double avg = tmon.getCpuTimeStats().avg(3); // avg of last 3 polls
                    //                   double avg = tmon.getCpuTimeStats().avg(1000); // avg of last 5 polls
                    //                   System.out.println("Alive " + tmon.getId() + ": " + avg);
                    System.out.println(
                            "Alive " + tmon.getName() + ": " + tmon.getTotalCpuTime() +
                            ", " + tmon.getCpuUsageStats().average() + ", " +
                            tmon.getUserUsageStats().average());
                }

                //               double totalCpu = gmonitor.getAvgCpuTimeStats(1000);
                //               double totalUser = gmonitor.getAvgUserTimeStats(1000);
                //               System.out.println("Total avg cpu: " + totalCpu + ", total avg user: " + totalUser);
                //               System.out.println("% usage: " + (totalCpu / totalUser * 100));
                System.out.println("-----------------------\n");
                start = System.currentTimeMillis();
            }

            // sleep for a bit
            Thread.sleep(100);
        }
    }
}
