# The Lost Update Problem

The **Lost Update Problem** is a race condition that occurs when two or more threads attempt to update the same shared variable simultaneously. 

Because the update operation (read -> modify -> write) is not **atomic**, updates can overwrite each other.

### The Scenario
Imagine `count = 10`.
1. **Thread A** reads `count` (10).
2. **Thread B** reads `count` (10).
3. **Thread A** adds 1 (11) and writes it back.
4. **Thread B** adds 1 (11) and writes it back.

**Result:** `count` is 11.
**Expected:** `count` should be 12 (10 + 1 + 1).
Thread A's update was effectively "lost" because Thread B overwrote it based on stale data.

## Code Examples

We have provided 4 files in this package to demonstrate the problem and 3 common Java solutions.

### 1. The Problem
*   **File:** `ProblemDemo.java`
*   **Description:** Runs 1000 threads incrementing a counter 1000 times each. The result should be 1,000,000, but it will be significantly less because of lost updates.

### 2. Solution: Synchronized Keyword
*   **File:** `SolutionSynchronized.java`
*   **Method:** Uses `synchronized` on the `increment()` method.
*   **Pros:** Simple standard Java mechanism.
*   **Cons:** Can be slow due to blocking; blocking can lead to thread contention.

### 3. Solution: Atomic Variables (Recommended)
*   **File:** `SolutionAtomic.java`
*   **Method:** Uses `java.util.concurrent.atomic.AtomicInteger`.
*   **Pros:** Critical because it uses non-blocking **CAS (Compare-And-Swap)** hardware instructions. usually fastest for simple counters.

### 4. Solution: Explicit Locks
*   **File:** `SolutionLock.java`
*   **Method:** Uses `java.util.concurrent.locks.ReentrantLock`.
*   **Pros:** More flexible than `synchronized` (can `tryLock()`, lock interruptibly, or lock for a specific duration).

## Spring Boot & Database Context

In a real-world Spring Boot application, shared state often lives in a database, not just in memory. The Lost Update Problem happens here too (two transactions read a row, modify it, and save it).

### 5. Solution: Optimistic Locking (`@Version`)
*   **Concept:** You don't lock the database row. Instead, you have a version column. When saving, you check if `db_version == your_version`. If not, it means someone else changed it.
*   **Code:** See `SpringDataJPASolution.java` for the `@Version` annotation example.
*   **Use case:** High concurrency, low contention (collisions are rare).

### 6. Solution: Pessimistic Locking (`PESSIMISTIC_WRITE`)
*   **Concept:** You lock the database row essentially using `SELECT ... FOR UPDATE`. No one else can read/write that row until your transaction finishes.
*   **Code:** See `SpringDataJPASolution.java` usage of `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
*   **Use case:** High contention (collisions are expected), preventing `ObjectOptimisticLockingFailureException`.
