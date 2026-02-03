# Java Concurrency Internals

This guide explains the lower-level mechanics of Java concurrency, memory models, and synchronization primitives.

---

## 1. Java Memory Model (JMM) & Happens-Before
The JMM defines how and when different threads can see values written by others.

### Happens-Before Relationship
This is a guarantee that memory writes by one specific statement are visible to another specific statement.
- **Locking:** Unlocking a monitor happens-before every subsequent locking of the same monitor.
- **Volatile:** A write to a volatile field happens-before every subsequent read of that same field.
- **Thread Start:** Calling `thread.start()` happens-before any action in the started thread.
- **Thread Join:** All actions in a thread happen-before any other thread successfully returns from a `join()` on that thread.

---

## 2. Volatile vs. Atomic vs. Synchronized

| Feature | `volatile` | `AtomicInteger` / `Atomic*` | `synchronized` |
| :--- | :--- | :--- | :--- |
| **Visibility** | Yes | Yes | Yes |
| **Atomicity** | No (only single read/write) | Yes (using CAS) | Yes |
| **Locking** | No-lock | Lock-free (CPU level) | Pessimistic Lock |
| **Use Case** | Flag (true/false) | Counters/Accumulators | Complex logic updates |

### Volatile Keyword
- Ensures **Visibility**: Prevents threads from caching the variable locally. All reads/writes go to main RAM.
- Prevents **Instruction Reordering**: The compiler and CPU cannot shuffle code around a volatile access.
- **Limitation:** Does not make `i++` atomic because `i++` is 3 operations (read, increment, write).

---

## 3. Monitor Locks: Object Lock vs. Class Lock

### Object Lock
- Synchronizing on a non-static method or `synchronized(this)`.
- Multiple threads can enter the same method if they are operating on **different instances** of the object.
- **Monitor:** The specific object instance.

### Class Lock
- Synchronizing on a static method or `synchronized(MyClass.class)`.
- Only **one thread** can enter across the **entire JVM** for that class, regardless of how many instances exist.
- **Monitor:** The `Class` object.

---

## 4. Why Deadlocks Happen Despite Releasing Locks?
When a thread goes into `BLOCKED` or `WAITING` state:
- `wait()` **RELEASES** the monitor lock.
- `Thread.sleep()`, `join()`, or blocking on I/O **DOES NOT** release the lock.
- **Deadlock:** Happens because Thread A is holding Lock 1 and waiting for Lock 2 (which Thread B holds), while Thread B is waiting for Lock 1. Neither can progress.

---

## 5. Semaphore vs. CountDownLatch vs. CyclicBarrier

### Semaphore
- **Purpose:** Restricts the number of threads that can access a resource (Throttling).
- **Analogy:** A library with 5 study rooms. You need a permit to enter.
- **Use Case:** Connection pooling, limiting concurrent API calls.

### CountDownLatch
- **Purpose:** A thread waits for N other events to complete before it can proceed.
- **State:** Decrement only. Once it hits zero, it cannot be reset.
- **Use Case:** Main thread waiting for 3 microservices to initialize before starting the server.

### CyclicBarrier
- **Purpose:** N threads must all arrive at a "barrier" point before any can continue.
- **State:** Resetable.
- **Use Case:** Parallel processing where you need to sync results before the next phase (e.g., MapReduce).

---

## 6. ExecutorService & custom ThreadPoolExecutor
In Spring Boot, you usually configure it via `@Bean`:

```java
@Configuration
public class ThreadPoolConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            10, // Core pool size
            50, // Max pool size
            60, TimeUnit.SECONDS, // Keep alive time
            new ArrayBlockingQueue<>(100), // Work Queue
            new ThreadPoolExecutor.CallerRunsPolicy() // Rejection Policy
        );
        return executor;
    }
}
```

### ForkJoinPool & Work-Stealing
- **Work-Stealing:** Idle threads "steal" tasks from the back of the queue of busy threads.
- `Executors.newWorkStealingPool()`: Creates a `ForkJoinPool` with parallelism equal to available processors.
- Use for recursively divisible tasks (divide and conquer).

---

## 7. ThreadLocal
- **Purpose:** Provides variables that are private to the thread.
- **Real-life use:** Storing `UserContext`, `TransactionID`, or `SimpleDateFormat` (which is not thread-safe).
- **Risk:** **Memory Leaks** in thread pools. If you don't call `.remove()`, the value stays in the thread (which is reused by the pool), preventing GC.

---

## 8. Virtual Threads (JDK 21)
- **What:** Lightweight threads managed by the JVM, not the OS.
- **Benefit:** You can run millions of virtual threads. Great for I/O bound tasks.
- **Platform Threads:** Traditional OS threads. Heavy, expensive to switch.
- **Disadvantage:** If you do CPU-intensive work or use certain native/synchronized blocks (Pinning), performance might drop.
