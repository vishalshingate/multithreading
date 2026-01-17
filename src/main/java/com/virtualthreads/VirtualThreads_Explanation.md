# Virtual Threads (Project Loom) vs Platform Threads

**Note:** You are running Java 17. Virtual Threads are a standard feature in **Java 21**. The code below explains the concept but requires an upgrade to run the "Virtual" examples.

---

## 1. The Comparison

| Feature | Platform Threads (Old) | Virtual Threads (New - Java 21) |
| :--- | :--- | :--- |
| **Mapping** | **1:1 with OS Threads**. One Java thread = One Kernel thread. | **M:N Mapping**. Many Virtual threads mapped to few OS threads. |
| **Metadata Size** | Heavy (~2MB stack memory). | Lightweight (Bytes/Kilobytes). Resizable stack. |
| **Creation Cost** | Expensive (System call). | Cheap (Just a Java Object). |
| **Context Switch** | Slow (OS Kernel switch). | Fast (JVM logic switch, mounting/unmounting). |
| **Scalability** | Limit ~5,000 - 10,000 threads max. | Limit ~1,000,000+ threads easily. |
| **Blocking** | Blocks the whole OS thread. CPU core sits idle. | **Unmounts** from OS thread. OS thread grabs another task immediately. |

---

## 2. Why Virtual Threads? The "Thread-per-Request" Model

In traditional Java (Spring Boot), we used `Thread Pools` (e.g., limit 200) because threads were expensive.
*   **Problem:** If you have 200 threads and they all block waiting for a Database, your server accepts **0** new requests.
*   **Old Solution:** Reactive Programming (WebFlux/RxJava) - callback hell, hard to debug.
*   **New Solution:** Virtual Threads. You can just spin up a new virtual thread for **every single request**. If it blocks, it costs nothing.

---

## 3. Code Comparison

### A. Platform Threads (Java 17 Code)
This crashes if you spawn too many.

```java
// Limit: ~10,000 before crashing/slowing down
for (int i = 0; i < 10_000; i++) {
    new Thread(() -> {
        System.out.println(Thread.currentThread()); // Thread[#23, name=...]
        try { Thread.sleep(1000); } catch (Exception e) {}
    }).start();
}
```

### B. Virtual Threads (Java 21 Code)
This runs 1,000,000 threads effortlessly.

```java
// Requires Java 21
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1_000_000; i++) {
        executor.submit(() -> {
            // Even if this blocks for 1 second, the implicit carrier threads 
            // simply switch to other tasks.
            Thread.sleep(1000); 
        });
    }
} // Auto-waits for all to finish
```

### C. Creating a Single Virtual Thread (Java 21)
```java
Thread.ofVirtual()
      .name("my-virtual-worker")
      .start(runnableTask);
```

---

## 4. Under the Hood: Carrier Threads

Virtual Threads run **on top of** Platform Threads.
1.  The JVM maintains a small pool of Platform Threads (called **Carrier Threads**), usually equal to the number of CPU cores.
2.  When a Virtual Thread executes generic code, it is "mounted" on a Carrier Thread.
3.  When the Virtual Thread hits a **Blocking Operation** (e.g., `Thread.sleep()`, `db.query()`, `socket.read()`), the JVM **unmounts** it.
    *   The Virtual Thread stack is moved to Heap memory.
    *   The Carrier Thread is now free to run *another* Virtual Thread.
4.  When the blocking operation finishes, the scheduler re-schedules the Virtual Thread to continue.

**Result:** You get non-blocking IO performance with simple blocking code style!

---

## 5. Disadvantages & Pitfalls

While Virtual Threads are powerful, they are not a silver bullet.

### A. NOT for CPU-Intensive Tasks
*   **Issue:** Virtual Threads are optimized for *waiting* (I/O). If you run a task that calculates Prime Numbers or processes Video for 10 minutes, it will hog the Carrier Thread (OS Thread).
*   **Result:** Other virtual threads cannot run.
*   **Advice:** Stick to `ForkJoinPool` or `Parallel Streams` (standard Platform Threads) for heavy computation.

### B. The "Pinning" Problem (synchronized blocks)
*   **Issue:** If a Virtual Thread enters a `synchronized` block or calls a native method (JNI), it is **Pinned** to the Carrier Thread.
*   **Result:** Even if it performs I/O inside that block, it CANNOT unmount. It behaves like a Platform Thread, blocking the OS thread.
*   **Advice:** Use `ReentrantLock` instead of `synchronized` where possible in critical I/O paths.

```java
// BAD (Pinning)
synchronized(lock) {
   socket.read(); // BLOCKS the Carrier Thread!
}

// GOOD
lock.lock();
try {
   socket.read(); // Unmounts correctly
} finally { lock.unlock(); }
```

### C. ThreadLocal Memory Explosion
*   **Issue:** In the old days, we had ~200 threads. Storing a 1MB object in `ThreadLocal` cost 200MB. Manageable.
*   **Issue:** Now you have 1,000,000 threads. Storing 1MB in `ThreadLocal` = 1 Terabyte of RAM!
*   **Advice:** Avoid heavy objects in `ThreadLocal`. Use `ScopedValues` (Preview feature) instead.

### D. Rate Limiting is harder
*   **Issue:** Thread Pools naturally throttled your app (e.g., max 50 concurrent DB connections).
*   **Issue:** With Virtual Threads, you can easily spawn 10,000 threads that all hit your Database at once, crashing the DB.
*   **Advice:** You MUST use explicit Rate Limiters or Semaphores to protect downstream resources.
