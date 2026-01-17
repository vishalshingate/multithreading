package com.lockvssync;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockVsSyncDemo {

    // 1. Synchronized Block (Implicit Lock)
    // - Simple to use
    // - Automatic release (even on exception)
    // - Non-fair (thread ordering not guaranteed)
    // - Blocks forever if lock not available
    public void synchronizedMethod() {
        synchronized (this) {
            System.out.println(Thread.currentThread().getName() + " acquired synchronized lock");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + " released synchronized lock");
        }
    }

    // 2. ReentrantLock (Explicit Lock)
    private final Lock lock = new ReentrantLock();

    // Feature A: tryLock() - Non-blocking attempt
    public void tryLockMethod() {
        try {
            // Try to get lock for 500ms, if not, give up.
            // Synchronized CANNOT do this.
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                try {
                    System.out.println(Thread.currentThread().getName() + " acquired ReentrantLock");
                    Thread.sleep(1000);
                } finally {
                    lock.unlock(); // MUST unlock in finally
                    System.out.println(Thread.currentThread().getName() + " released ReentrantLock");
                }
            } else {
                System.out.println(Thread.currentThread().getName() + " could NOT acquire lock (timeout)");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LockVsSyncDemo demo = new LockVsSyncDemo();

        System.out.println("--- TEST 1: Synchronized Block ---");
        Thread t1 = new Thread(() -> demo.synchronizedMethod(), "Sync-Thread-1");
        Thread t2 = new Thread(() -> demo.synchronizedMethod(), "Sync-Thread-2");
        t1.start(); t2.start();
        t1.join(); t2.join();

        System.out.println("\n--- TEST 2: ReentrantLock tryLock() ---");
        // Thread 3 gets the lock easily
        Thread t3 = new Thread(() -> demo.tryLockMethod(), "Lock-Thread-1");
        t3.start();

        Thread.sleep(100); // Ensure Lock-Thread-1 gets the lock first

        // Thread 4 tries, waits 500ms, then gives up because Thread 3 holds it for 1000ms
        Thread t4 = new Thread(() -> demo.tryLockMethod(), "Lock-Thread-2");
        t4.start();

        t3.join();
        t4.join();
    }
}

