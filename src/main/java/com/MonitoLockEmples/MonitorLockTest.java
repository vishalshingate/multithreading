package com.MonitoLockEmples;

public class MonitorLockTest {
    public static void main(String[] args) throws InterruptedException {
        MonitorLock obj = new MonitorLock();

        System.out.println("--- TEST 1: Instance Method (task1) vs Synchronized Block(this) (task4) ---");
        // These should be SEQUENTIAL because they both use the object 'obj' lock.
        Thread t1 = new Thread(() -> obj.task1(), "T1");
        Thread t2 = new Thread(() -> obj.task4(), "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("\n--- TEST 2: Instance Method (task1) vs Class Lock (task5) ---");
        // These should be PARALLEL because one locks 'obj' and the other locks 'MonitorLock.class'
        Thread t3 = new Thread(() -> obj.task1(), "T3");
        Thread t4 = new Thread(() -> obj.task5(), "T4");

        t3.start();
        t4.start();

        t3.join();
        t4.join();
    }
}

