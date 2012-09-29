package com.ferox.util.profile;

import java.util.Random;

public class ThreadMonitorTest {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 4; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Random r = new Random();
                    double total = 0;
                    while(true) {
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
        while(true)
        {
            gmonitor.poll();

            if ((System.currentTimeMillis() - start) > 1000) {
                System.out.println("-----------------------");
                System.out.println("Estimated run time: " + ((System.currentTimeMillis() - veryStart) / 1000.0));
                for(ThreadMonitor tmon: gmonitor.getAliveThreadMonitors())
                {
                    //              double avg = tmon.getCpuTimeStats().avg();  // avg of last polls
                    //              double avg = tmon.getCpuTimeStats().avg(3); // avg of last 3 polls
                    //                   double avg = tmon.getCpuTimeStats().avg(1000); // avg of last 5 polls
                    //                   System.out.println("Alive " + tmon.getId() + ": " + avg);
                    System.out.println("Alive " + tmon.getName() + ": " + tmon.getTotalCpuTime() + ", " + tmon.getCpuUsageStats().avg(5));
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
