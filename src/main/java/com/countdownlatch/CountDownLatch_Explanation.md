# What is CountDownLatch?

`CountDownLatch` is a synchronization aid that allows one or more threads to wait until a set of operations being performed in other threads completes.

Think of it like a **Rocket Launch Sequence**:
1.  **Commander (Main Thread):** "Waiting for all systems go."
2.  **Engine Engineer:** "Engines Ready." (Count - 1)
3.  **Fuel Engineer:** "Fuel Loaded." (Count - 1)
4.  **Nav Engineer:** "Coordinates Set." (Count - 1)
5.  **Commander:** "All checks complete (Count == 0). **LIFT OFF!**"

---

### Key Methods

1.  **`new CountDownLatch(N)`**
    *   Initializes the latch with a counter of `N`.
    *   This count cannot be reset! (Use `CyclicBarrier` if you need to reuse it).

2.  **`await()`**
    *   The calling thread **blocks** (waits) until the count reaches zero.
    *   If the count is already zero, it returns immediately.
    *   Often called by the Main thread or a Coordinator thread.

3.  **`countDown()`**
    *   Decrements the count by 1.
    *   Usually called by worker threads when they finish a task.
    *   If the new count is 0, it wakes up all waiting threads.

---

### Visual Diagram

**Scenario: CountDownLatch(3)** (Waiting for 3 Services)

```text
    [Main Thread]                   [Service A]      [Service B]      [Service C]
          |                              |                |                |
    latch.await()                        |                |                |
    (BLOCKED)                            |                |                |
          |                              | (Work done)    |                |
          |                      latch.countDown()        |                |
          |                        (Count=2)              |                |
          |                              |                | (Work done)    |
          |                              |        latch.countDown()        |
          |                              |          (Count=1)              |
          |                              |                |                | (Work done)
          |                              |                |        latch.countDown()
          |                              |                |          (Count=0)
    (UNBLOCKED!)                         |                |                |
    "System Start!"                      v                v                v
          v
```

### When to use it?

1.  **Waiting for Initialization:**
    *   Like the demo: Don't accept API requests until DB, Cache, and Messaging are connected.

2.  **Parallel Processing / Scatter-Gather:**
    *   Break a large task into N chunks.
    *   Give each chunk to a thread.
    *   Main thread `await()`s.
    *   When all N threads are done (`countDown()`), Main thread merges the results.

3.  **Testing Concurrency:**
    *   Start 10 threads, but make them all wait on a `latch(1)`.
    *   When you are ready, call `countDown()` once to release all 10 threads exactly at the same millisecond to test race conditions.

---

### Comparison: CountDownLatch vs CyclicBarrier

| Feature | CountDownLatch | CyclicBarrier |
| :--- | :--- | :--- |
| **Action** | Count goes **Down** (N to 0). | Threads wait for each other to reach a **Barrier**. |
| **Reusability** | **One-time use**. Once 0, it's dead. | **Reusable**. Can be reset automatically after tripping. |
| **Focus** | One thread waits for N threads. | N threads wait for each other. |

