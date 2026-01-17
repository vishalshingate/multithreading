package com.threadlocal;

public class ThreadLocalDemo {

    // ThreadLocal variable to store a unique ID per thread.
    // Imagine this is storing the "Current User ID" for a web request.
    private static final ThreadLocal<String> userContext = new ThreadLocal<>();

    public static void main(String[] args) {

        // Thread 1: Simulating handling Request A (User: Alice)
        Thread t1 = new Thread(() -> {
            userContext.set("Alice"); // Identify this thread as Alice
            try {
                processRequest();
            } finally {
                // ALWAYS clean up ThreadLocals to prevent memory leaks in pools!
                userContext.remove();
            }
        }, "Thread-A");

        // Thread 2: Simulating handling Request B (User: Bob)
        Thread t2 = new Thread(() -> {
            userContext.set("Bob"); // Identify this thread as Bob
            try {
                processRequest();
            } finally {
                userContext.remove();
            }
        }, "Thread-B");

        t1.start();
        t2.start();
    }

    // A method deep in the service layer.
    // Notice: We don't verify arguments! We just grab the context from the air.
    private static void processRequest() {
        System.out.println(Thread.currentThread().getName() + " start processing for user: " + userContext.get());

        callServiceLayer();

        System.out.println(Thread.currentThread().getName() + " finished processing.");
    }

    private static void callServiceLayer() {
        try { Thread.sleep(500); } catch (Exception e) {}
        // Even deeper in the stack, we can still see who the user is without passing params.
        System.out.println("   [Service Layer] " + Thread.currentThread().getName() + " working for: " + userContext.get());
    }
}

