package com.countdownlatch;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {

    public static void main(String[] args) {
        // We have 3 dependent services that must start before the main app.
        // So we initialize the latch with count = 3.
        int numberOfServices = 3;
        CountDownLatch latch = new CountDownLatch(numberOfServices);

        System.out.println("Main Service: Starting dependency services...");

        // Start 3 different threads (Services)
        new Thread(new Service("Database Service", 2000, latch)).start();
        new Thread(new Service("Cache Service", 1000, latch)).start();
        new Thread(new Service("Messaging Service", 3000, latch)).start();

        try {
            // Main thread pauses here!
            // It will NOT proceed until count reaches 0.
            System.out.println("Main Service: Waiting for latch (Count: " + latch.getCount() + ")");
            latch.await();

            System.out.println("\nMain Service: All dependent services are UP!");
            System.out.println("Main Service: Accepting HTTP requests now.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Service implements Runnable {
    private String name;
    private int timeToStart;
    private CountDownLatch latch;

    public Service(String name, int timeToStart, CountDownLatch latch) {
        this.name = name;
        this.timeToStart = timeToStart;
        this.latch = latch;
    }

    @Override
    public void run() {
        System.out.println(name + " starting...");
        try {
            Thread.sleep(timeToStart);
            System.out.println(name + " is UP");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            latch.countDown();
            System.out.println(name + " latch count: " + latch.getCount());
        }
    }
}

