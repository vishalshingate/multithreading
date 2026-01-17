package com.learnkafka;

public class LockTypesDemo {

    public static void main(String[] args) {
        // SCENARIO 1: Object Lock (Different instances)
        System.out.println("--- SCENARIO 1: Object Locks on DIFFERENT objects ---");
        SharedResource obj1 = new SharedResource("Obj1");
        SharedResource obj2 = new SharedResource("Obj2");

        Thread t1 = new Thread(() -> obj1.instanceMethod(), "Thread-1");
        Thread t2 = new Thread(() -> obj2.instanceMethod(), "Thread-2");

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) { e.printStackTrace(); }

        System.out.println("\n--- SCENARIO 2: Class Lock (Static methods) ---");
        // SCENARIO 2: Class Lock (Shared across ALL instances)

        Thread t3 = new Thread(() -> SharedResource.staticMethod(), "Thread-3");
        Thread t4 = new Thread(() -> SharedResource.staticMethod(), "Thread-4");

        t3.start();
        t4.start();
    }
}

class SharedResource {
    private String name;

    public SharedResource(String name) {
        this.name = name;
    }

    // OBJECT LOCK: Locks only 'this' instance
    public synchronized void instanceMethod() {
        System.out.println(Thread.currentThread().getName() + " acquired OBJECT lock on " + this.name);
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        System.out.println(Thread.currentThread().getName() + " released OBJECT lock on " + this.name);
    }

    // CLASS LOCK: Locks 'SharedResource.class' (Global for the class)
    public static synchronized void staticMethod() {
        System.out.println(Thread.currentThread().getName() + " acquired CLASS lock");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        System.out.println(Thread.currentThread().getName() + " released CLASS lock");
    }
}

