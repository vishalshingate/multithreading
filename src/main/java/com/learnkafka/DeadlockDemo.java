package com.learnkafka;

public class DeadlockDemo {
    // Two resources (locks)
    private static final Object resource1 = new Object();
    private static final Object resource2 = new Object();

    public static void main(String[] args) {

        // Thread 1: Tries to lock resource1 then resource2
        Thread thread1 = new Thread(() -> {
            synchronized (resource1) {
                System.out.println("Thread 1: Locked Resource 1");

                try { Thread.sleep(100); } catch (Exception e) {} // Simulating work

                System.out.println("Thread 1: Waiting for Resource 2...");

                // CRITIAL POINT: Thread 1 creates a "Blocked" state here waiting for lock.
                // It does NOT release resource1 while it waits for resource2!
                synchronized (resource2) {
                    System.out.println("Thread 1: Locked Resource 2");
                }
            }
        });

        // Thread 2: Tries to lock resource2 then resource1
        Thread thread2 = new Thread(() -> {
            synchronized (resource2) {
                System.out.println("Thread 2: Locked Resource 2");

                try { Thread.sleep(100); } catch (Exception e) {} // Simulating work

                System.out.println("Thread 2: Waiting for Resource 1...");

                // CRITICAL POINT: Thread 2 blocks here waiting for resource1.
                // It holds resource2 while waiting!
                synchronized (resource1) {
                    System.out.println("Thread 2: Locked Resource 1");
                }
            }
        });

        thread1.start();
        thread2.start();
    }
}

