# JVM Garbage Collectors: focus on G1GC

## 1. What is G1GC (Garbage First)?

Introduced in Java 7 and the default collector since Java 9, **G1GC** is a server-style garbage collector, targeted for multi-processor machines with large memories. It attempts to meet garbage collection pause-time goals with high probability while achieving high throughput.

### Key Innovation: Heap Regions
Unlike traditional collectors (Parallel, CMS) which divide the heap into three fixed physical areas (Eden, Survivor, Old), G1 divides the heap into approximately **2,048 equal-sized regions**.

Each region can be assigned as:
*   **Eden (E):** Young generation.
*   **Survivor (S):** Young generation.
*   **Old (O):** Old generation.
*   **Humongous (H):** For objects larger than 50% of a region size.

---

## 2. How G1GC Works (The "Garbage First" Logic)

The name "Garbage First" comes from its strategy: it tracks which regions have the most "reclaimable" space (garbage) and collects those first.

### Cycle Phases:

1.  **Initial Mark (STW):** Marks the roots. This is done during a normal Young GC (Evacuation Pause).
2.  **Concurrent Marking:** While the application is running, G1 identifies reachable objects across the entire heap.
3.  **Remark (STW):** Finalizes the marking. Uses an algorithm called **Snapshot-At-The-Beginning (SATB)** which is faster than what CMS used.
4.  **Cleanup (STW):** Accounts for free regions and identifies regions with high reclaimable space.
5.  **Copying / Evacuation (STW):** G1 selects the "Collection Set" (CSet) — specific regions to be reclaimed. It copies live objects from these regions to new ones, effectively compacting the memory.

---

## 3. Why use G1GC?

| Feature | Benefit |
| :--- | :--- |
| **Predictable Pauses** | You can set a target: `-XX:MaxGCPauseMillis=200`. G1 will try its best to keep pauses under 200ms. |
| **Compaction** | Unlike CMS (which leaves holes/fragmentation), G1 compacts memory during every collection by copying objects. |
| **No "Full GC" requirement** | G1 performs "Mixed GCs" (collecting both Young and some Old regions) to prevent the heap from getting so full that it needs a slow, single-threaded Full GC. |

---

## 4. When to Tune or Switch?

*   Use G1GC if your heap is **larger than 4GB** and you want to avoid long GC pauses (e.g., > 1 second).
*   If your application is very sensitive to throughput and can tolerate longer pauses, **Parallel GC** (`-XX:+UseParallelGC`) might be better.
*   For ultra-low latency (pauses < 1ms) with massive heaps, look at **ZGC** or **Shenandoah**.

---

## 5. Important G1GC Flags

*   `-XX:+UseG1GC`: Enables it.
*   `-XX:MaxGCPauseMillis=200`: The most important tuning knob.
*   `-XX:G1HeapRegionSize=n`: Sets the size of individual regions (1MB to 32MB).
*   `-XX:InitiatingHeapOccupancyPercent=45`: When the total heap usage reaches 45%, start the marking cycle.

---

## 6. Deadlock Revisited (Real World Scenario)

The user asked: *"If any thread goes into blocked state that time it releases all the monitor locks then why deadlock happens in real systems?"*

**Clarification:**
*   A thread **only** releases locks when it calls `object.wait()`.
*   If a thread is **Blocked** (waiting for a `synchronized` block) or **Sleeping** (`Thread.sleep()`), it **DOES NOT** release its locks.

**Real World Example: The "Bank Transfer" Deadlock**
1.  Thread-A starts transferring money from **Acc-1** to **Acc-2**. It locks **Acc-1**.
2.  Thread-B starts transferring money from **Acc-2** to **Acc-1**. It locks **Acc-2**.
3.  Thread-A tries to lock **Acc-2** (Blocked, waiting for Thread-B).
4.  Thread-B tries to lock **Acc-1** (Blocked, waiting for Thread-A).
5.  **Deadlock!** Both are blocked, neither releases their first lock.

**How to prevent?**
*   **Lock Ordering:** Always lock accounts in the same order (e.g., by ID number).
*   **Timeouts:** Use `lock.tryLock(timeout)` instead of `synchronized`.

