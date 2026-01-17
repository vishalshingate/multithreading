# What is a Semaphore?

A **Semaphore** is a thread synchronization construct used to control access to a common resource by multiple threads. Unlike a standard Lock (which allows only 1 thread at a time), a Semaphore maintains a set of **permits**.

Think of it like a **Night Club Bouncer**:
*   The club has a capacity of **N** people.
*   If capacity is not full, the bouncer lets people in (`acquire()`).
*   If capacity is full, new people must wait in line outside.
*   When someone leaves (`release()`), the bouncer lets the next person from the line in.

---

### Key Methods

1.  `semaphore.acquire()`:
    *   Decrements the permit count.
    *   If the count is 0, the thread **blocks** (waits) until a permit comes available.
2.  `semaphore.release()`:
    *   Increments the permit count.
    *   If threads are waiting, one of them will be unblocked and acquire the permit.
3.  `semaphore.tryAcquire()`:
    *   Tries to get a permit. If not available immediately, it returns `false` instead of blocking.

---

### Visual Diagram

**Scenario: Semaphore(2)** (Max 2 concurrent threads)

```text
Permits: 2

[Thread A] calls acquire() -> OK. Permits: 1
[Thread B] calls acquire() -> OK. Permits: 0  <-- Resource Full
[Thread C] calls acquire() -> BLOCKED (Wait)

... Thread A finishes work ...

[Thread A] calls release() -> Permits: 1
[Thread C] Wakes up! Decrements -> Permits: 0
[Thread C] Enters critical section
```

---

### When to use Semaphores?

Semaphores are not for "Mutual Exclusion" (protecting a variable from corruption) as much as they are for **Flow Control**.

#### 1. Connection Pooling
If you have a database that can only handle 10 simultaneous connections, you create a `Semaphore(10)`. Before opening a connection, a thread must acquire a permit. This prevents your app from overwhelming the DB.

#### 2. Rate Limiting / Throttling
Limiting the number of API calls or heavy calculations performing simultaneously. For example, allowing only 5 users to download a large file at once to save bandwidth.

#### 3. Bounded Collection (Producer-Consumer)
A `Semaphore(0)` can be used as a signaling mechanism (wait for an item to arrive), though `locks` and `conditions` (or `BlockingQueue`) are often preferred for this specific case now.

---

### Semaphore vs Lock (Mutex)

| Feature | Mutex (ReentrantLock / synchronized) | Semaphore |
| :--- | :--- | :--- |
| **Concept** | "I own this." | "I have a ticket." |
| **Ownership** | **Thread Ownership.** Only the thread that locked it can unlock it. | **No Ownership.** Thread A can acquire, Thread B can release (in some designs). |
| **Concurrency** | **1 Thread** at a time. | **N Threads** at a time (defined by permits). |
| **Analogy** | Toilet Key (Only one person). | Parking Lot (Multiple spots). |

