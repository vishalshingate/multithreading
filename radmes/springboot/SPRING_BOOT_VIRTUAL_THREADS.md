# Spring Boot 3.2+: Virtual Threads (`spring.threads.virtual.enabled=true`)

This document explains what happens internally when you enable Virtual Threads in a Spring Boot application using the property:

```properties
spring.threads.virtual.enabled=true
```

## 1. What Changes Occur?

When this property is set to `true`, Spring Boot auto-configuration activates the Virtual Thread support introduced in Java 21 (Project Loom). The major changes are:

### A. Embedded Web Server (Tomcat / Jetty)
*   **Before (False):** The embedded Tomcat server uses a standard thread pool (default max 200 threads). Each HTTP request is handled by one OS-level platform thread. If 200 requests are blocked (e.g., waiting for DB), the 201st request waits in the queue.
*   **After (True):** Tomcat is configured to use a **Virtual Thread Executor**.
    *   Instead of a fixed thread pool, a **new virtual thread** is created for **every single incoming HTTP request**.
    *   Virtual threads are cheap to create (millions can exist).
    *   **Result:** The server rarely runs out of threads to handle connections, significantly improving throughput for I/O-bound applications.

### B. Task Executors (`@Async`)
*   **Before:** `SimpleAsyncTaskExecutor` or `ThreadPoolTaskExecutor` were used, which relied on platform threads.
*   **After:** The default `AsyncTaskExecutor` and `TaskExecutor` beans are replaced with `SimpleAsyncTaskExecutor` configured to use **Virtual Threads**.
*   **Effect:** Methods annotated with `@Async` will now run on virtual threads automatically.

### C. Scheduling (`@Scheduled`)
*   **Before:** The `TaskScheduler` used a pool of platform threads (size 1 by default).
*   **After:** The scheduled tasks are launched on virtual threads.

---

## 2. How it Works Internally?

### The "Carrier Thread" Model
Virtual threads are **not** OS threads. They are Java objects managed by the JVM.
1.  **Mounting:** When a virtual thread needs to run (CPU execution), the JVM **mounts** it onto a "Carrier Thread" (a real Platform Thread, usually from a `ForkJoinPool`).
2.  **Unmounting (Blocking):** When the virtual thread performs a blocking I/O operation (e.g., `db.query()` or `restTemplate.getForObject()`), the JVM **unmounts** it from the carrier thread.
    *   The carrier thread is now free to mount and execute another virtual thread.
    *   The blocked virtual thread stays in heap memory (dormant).
3.  **Resuming:** Once the I/O completes (data arrives), the OS notifies the JVM, and the virtual thread is scheduled to run again on any available carrier thread.

### Why is this better?
*   **OS Threads are Expensive:** 1 OS thread ≈ 1MB RAM. 5000 threads = 5GB RAM.
*   **Virtual Threads are Cheap:** 1 Virtual thread ≈ nearly zero bytes (a few kb). 1 million threads = modest RAM.
*   **Non-Blocking I/O for Free:** You write synchronous blocking code (`User user = repo.findById(1);`), but the JVM makes it run like non-blocking async code under the hood.

---

## 3. What does NOT change? (Common Misconceptions)

### A. Latency for CPU-Bound Tasks
*   **Myth:** "Virtual threads make my complex calculations faster."
*   **Reality:** No. If a request involves calculating the nth prime number or processing an image (CPU work), it still hogs the carrier thread. Virtual threads offer **no benefit** for CPU-intensive tasks; they might even be slightly slower due to mounting overhead.

### B. Reactive Programming (WebFlux)
*   Virtual threads are an **alternative** towards the same goal as WebFlux (high concurrency).
*   They allow you to keep the simple "Thread-per-request" coding style (Spring MVC) while getting the scalability benefits of Reactive programming.

---

## 4. Potential Pitfalls (The "Pinning" Issue)

Even with virtual threads enabled, your application might not scale well if you encounter **Pinning**.

### The Problem
A virtual thread is "pinned" to its carrier thread if it performs a blocking operation while:
1.  Inside a `synchronized` block/method.
2.  Executing a native method (JNI).

**Consequence:** The JVM **cannot unmount** the virtual thread. The carrier thread remains blocked. If all carrier threads are blocked, the application halts (similar to traditional thread exhaustion).

### The Fix
*   Replace `synchronized` blocks with `ReentrantLock`.
*   Ensure libraries (JDBC drivers, XML parsers) are updated to be virtual-thread-friendly (most modern drivers like Postgres JDBC 42.6+ are compatible).

---

## 5. Summary

| Feature | Virtual Threads Disabled (`false`) | Virtual Threads Enabled (`true`) |
| :--- | :--- | :--- |
| **Request Handling** | Fixed Thread Pool (Standard) | One Virtual Thread per Request |
| **Scalability** | Limited by OS Threads (Memory/CPU context switch) | Limited only by Heap Memory / CPU cycles |
| **Style** | Blocking I/O blocks the OS Thread | Blocking I/O unmounts the Virtual Thread (Carrier free) |
| **Best For** | CPU-intensive or low-concurrency apps | High-concurrency I/O-bound apps (DB, Network calls) |

