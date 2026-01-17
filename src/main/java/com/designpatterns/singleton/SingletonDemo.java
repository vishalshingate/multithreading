package com.designpatterns.singleton;

import java.io.Serializable;
import java.lang.reflect.Constructor;

public class SingletonDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("--- 1. Eager Initialization ---");
        EagerSingleton eager1 = EagerSingleton.getInstance();
        EagerSingleton eager2 = EagerSingleton.getInstance();
        System.out.println("Are instances same? " + (eager1 == eager2));

        System.out.println("\n--- 2. Thread Safe (Double Checked Locking) ---");
        ThreadSafeSingleton safe1 = ThreadSafeSingleton.getInstance();
        ThreadSafeSingleton safe2 = ThreadSafeSingleton.getInstance();
        System.out.println("Are instances same? " + (safe1 == safe2));

        System.out.println("\n--- 3. Bill Pugh Singleton (Best Practice) ---");
        BillPughSingleton bill1 = BillPughSingleton.getInstance();
        BillPughSingleton bill2 = BillPughSingleton.getInstance();
        System.out.println("Are instances same? " + (bill1 == bill2));

        System.out.println("\n--- 4. Breaking Singleton with Reflection ---");
        Constructor<EagerSingleton> constructor = EagerSingleton.class.getDeclaredConstructor();
        constructor.setAccessible(true); // Bypass private constructor
        EagerSingleton brokenInstance = constructor.newInstance();
        System.out.println("Broken Instance same as Original? " + (eager1 == brokenInstance));

        System.out.println("\n--- 5. Enum Singleton (Reflection Safe) ---");
        EnumSingleton enum1 = EnumSingleton.INSTANCE;
        EnumSingleton enum2 = EnumSingleton.INSTANCE;
        System.out.println("Are instances same? " + (enum1 == enum2));
        // Reflection on Enum throws IllegalArgumentException ("Cannot reflectively create enum objects")
    }
}

// ---------------------------------------------------------
// 1. EAGER INITIALIZATION
// Problem: Instance created even if client triggers other static logic but never calls getInstance()
// ---------------------------------------------------------
class EagerSingleton {
    private static final EagerSingleton INSTANCE = new EagerSingleton();

    private EagerSingleton() {} // Private Constructor

    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}

// ---------------------------------------------------------
// 2. DOUBLE CHECKED LOCKING (Thread Safe + Lazy)
// Problem: Complex logic, requires 'volatile'
// ---------------------------------------------------------
class ThreadSafeSingleton {
    private static volatile ThreadSafeSingleton instance;

    private ThreadSafeSingleton() {}

    public static ThreadSafeSingleton getInstance() {
        if (instance == null) { // First check (No Check)
            synchronized (ThreadSafeSingleton.class) {
                if (instance == null) { // Second check (With Lock)
                    instance = new ThreadSafeSingleton();
                }
            }
        }
        return instance;
    }
}

// ---------------------------------------------------------
// 3. BILL PUGH SINGLETON (Static Inner Helper) - RECOMMENDED
// Benefit: Lazy Loading without Synchronization overhead.
// ---------------------------------------------------------
class BillPughSingleton {
    private BillPughSingleton() {}

    // Inner class is NOT loaded until getInstance() is called
    private static class SingletonHelper {
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }

    public static BillPughSingleton getInstance() {
        return SingletonHelper.INSTANCE;
    }
}

// ---------------------------------------------------------
// 4. ENUM SINGLETON - ABSOLUTELY SAFE
// Benefit: Handles Serialization and Reflection attacks automatically.
// ---------------------------------------------------------
enum EnumSingleton {
    INSTANCE;

    public void doSomething() {
        System.out.println("Enum Singleton working...");
    }
}

