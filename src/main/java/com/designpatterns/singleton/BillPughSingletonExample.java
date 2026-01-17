package com.designpatterns.singleton;

/**
 * Bill Pugh Singleton Implementation.
 *
 * This is widely considered the best approach for implementing a Singleton in Java.
 * It uses a static inner helper class to hold the singleton instance.
 */
public class BillPughSingletonExample {

    // 1. Private Constructor
    // Prevents instantiation from other classes.
    private BillPughSingletonExample() {
        System.out.println("BillPughSingletonExample Initialized!");
    }

    // 2. Static Inner Helper Class
    // This class is NOT loaded into memory when the outer class (BillPughSingletonExample) is loaded.
    // It is ONLY loaded when the getInstance() method is called.
    // This provides "Lazy Initialization" automatically handled by the JVM ClassLoader.
    private static class SingletonHelper {
        // The instance is created when this class is loaded.
        // Because class loading is thread-safe in Java, we don't need 'synchronized'.
        private static final BillPughSingletonExample INSTANCE = new BillPughSingletonExample();
    }

    // 3. Global Access Point
    public static BillPughSingletonExample getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public void doSomething() {
        System.out.println("I am the Bill Pugh Singleton instance!");
    }

    // Main method to demonstrate
    public static void main(String[] args) {
        System.out.println("Main method started. Singleton not created yet.");

        // Notice: The constructor print statement hasn't appeared yet.
        // This proves Lazy Loading.

        System.out.println("Calling getInstance() for the first time...");
        BillPughSingletonExample instance1 = BillPughSingletonExample.getInstance();

        System.out.println("Calling getInstance() for the second time...");
        BillPughSingletonExample instance2 = BillPughSingletonExample.getInstance();

        System.out.println("Are instances same? " + (instance1 == instance2));
        instance1.doSomething();
    }
}

