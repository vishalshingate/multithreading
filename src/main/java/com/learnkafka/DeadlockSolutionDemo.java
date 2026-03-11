package com.learnkafka;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeadlockSolutionDemo {

    public static void main(String[] args) {
        // Start a background thread to detect deadlocks
        startDeadlockMonitor();

        Account acc1 = new Account(1, "Account-1", 1000);
        Account acc2 = new Account(2, "Account-2", 1000);

        // DEADLOCK SCENARIO (Uncomment to see the app hang!)
        /*
        System.out.println("--- Starting Deadlock Scenario ---");
        Thread t1 = new Thread(() -> transferDeadlockProne(acc1, acc2, 50), "Thread-1");
        Thread t2 = new Thread(() -> transferDeadlockProne(acc2, acc1, 50), "Thread-2");

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("--- Deadlock Scenario Finished ---");
        */

        // SOLUTION SCENARIO:
        // Both threads lock accounts in the same order (based on ID)
        System.out.println("\n--- Starting Solution Scenario (Lock Ordering) ---");

        Thread t3 = new Thread(() -> transferSafe(acc1, acc2, 50), "Thread-3");
        Thread t4 = new Thread(() -> transferSafe(acc2, acc1, 50), "Thread-4");

        t3.start();
        t4.start();

        try {
            t3.join();
            t4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("--- Solution Scenario Finished ---");
    }

    private static void startDeadlockMonitor() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();

        scheduler.scheduleAtFixedRate(() -> {
            long[] threadIds = bean.findDeadlockedThreads(); // Checks primarily for object monitor deadlocks
            if (threadIds != null) {
                System.err.println("!!! DEADLOCK DETECTED !!!");
                ThreadInfo[] threadInfos = bean.getThreadInfo(threadIds);
                for (ThreadInfo info : threadInfos) {
                    System.err.println("Thread '" + info.getThreadName() + "' is blocked waiting for lock: " + info.getLockName());
                    System.err.println("   held by thread: " + info.getLockOwnerName());
                }
                // In a real app, you might trigger an alert or a heap dump here
            }
        }, 2, 5, TimeUnit.SECONDS);
    }

    // ---------------------------------------------------------
    // INCORRECT: Locks based on parameter order
    // ---------------------------------------------------------
    public static void transferDeadlockProne(Account from, Account to, double amount) {
        synchronized (from) {
            System.out.println(Thread.currentThread().getName() + " locked " + from.name);
            try { Thread.sleep(100); } catch (InterruptedException e) {} // Simulate work

            System.out.println(Thread.currentThread().getName() + " waiting for " + to.name);
            synchronized (to) {
                System.out.println(Thread.currentThread().getName() + " locked " + to.name);
                from.withdraw(amount);
                to.deposit(amount);
            }
        }
    }

    // ---------------------------------------------------------
    // CORRECT: Locks based on consistent global ordering (ID)
    // ---------------------------------------------------------
    public static void transferSafe(Account from, Account to, double amount) {
        Account firstLock = from.id < to.id ? from : to;
        Account secondLock = from.id < to.id ? to : from;

        synchronized (firstLock) {
            System.out.println(Thread.currentThread().getName() + " locked " + firstLock.name);
            try { Thread.sleep(100); } catch (InterruptedException e) {}

            System.out.println(Thread.currentThread().getName() + " waiting for " + secondLock.name);
            synchronized (secondLock) {
                System.out.println(Thread.currentThread().getName() + " locked " + secondLock.name);
                from.withdraw(amount);
                to.deposit(amount);
            }
        }
    }

    static class Account {
        int id;
        String name;
        double balance;

        public Account(int id, String name, double balance) {
            this.id = id;
            this.name = name;
            this.balance = balance;
        }

        void withdraw(double amount) { balance -= amount; }
        void deposit(double amount) { balance += amount; }
    }
}

