 package com.virtualthreads;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the limitations of Platform Threads (OS Threads).
 * Trying to start 100,000 threads will likely crash the JVM with OutOfMemoryError
 * or take a very long time.
 */
public class PlatformThreadLimit {

    public static void main(String[] args) {
        int MAX_THREADS = 10_000; // Try increasing to 100,000 if your PC is strong
        AtomicInteger count = new AtomicInteger(0);

        long start = System.currentTimeMillis();

        try {
            for (int i = 0; i < MAX_THREADS; i++) {
                new Thread(() -> {
                    try {
                        count.incrementAndGet();
                        Thread.sleep(5000); // Simulate blocking IO
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (OutOfMemoryError e) {
            System.err.println("CRASHED! Native Memory Exhausted at thread #" + count.get());
            System.exit(1);
        }

        long end = System.currentTimeMillis();
        System.out.println("Started " + MAX_THREADS + " Platform Threads in " + (end - start) + "ms");
        System.out.println("Active Threads: " + Thread.activeCount());
    }
}

