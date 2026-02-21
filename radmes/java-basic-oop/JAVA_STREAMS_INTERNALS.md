# Java Streams: Sequential vs Parallel Internals

This document explains the internal working mechanism of Java Streams, focusing on the differences between **Sequential Streams** and **Parallel Streams**.

---

## 1. High-Level Difference

| Feature | Sequential Stream (`stream()`) | Parallel Stream (`parallelStream()`) |
| :--- | :--- | :--- |
| **Execution Mode** | Single-threaded | Multi-threaded |
| **Ordering** | Maintains processing order (encounter order) | Does not guarantee processing order (unless specified) |
| **Underlying Mechanism** | Simple Iterator/Spliterator traversal | Fork/Join Framework (Divide and Conquer) |
| **Core Usage** | Simple tasks, small datasets, order-sensitive tasks | Massive datasets, CPU-intensive tasks, order-independent tasks |

---

## 2. Internal Working: Sequential Stream

A Sequential Stream runs on a **single thread** (usually the main thread or the thread that initiated the stream).

### How it works:
1.  **Source:** Created from a collection, array, or generator.
2.  **Spliterator:** The source uses a `Spliterator` (Split-Iterator) to traverse elements.
    *   In sequential mode, `tryAdvance()` is called repeatedly to process elements one by one.
3.  **Pipeline:** Operations (filter, map) are chained.
4.  **Terminal Operation:** The stream is lazy. Execution triggers only when the terminal operation (collect, reduce, forEach) is called.

**Visual Flow:**
```mermaid
[Method Call] -> [Thread A] -> [Filter] -> [Map] -> [Collect] -> [Result]
```

---

## 3. Internal Working: Parallel Stream

A Parallel Stream utilizes multiple threads to process the data simultaneously. It relies heavily on the **Fork/Join Framework** introduced in Java 7.

### Core Components:
1.  **ForkJoinPool:** Parallel streams use the common static pool: `ForkJoinPool.commonPool()`.
    *   The number of threads defaults to `Runtime.getRuntime().availableProcessors() - 1`.
2.  **Spliterator (The Splitter):**
    *   Instead of just traversing (`tryAdvance`), the Spliterator uses `trySplit()`.
    *   **Decomposition:** The data source is recursively split into smaller chunks until a threshold is reached.
3.  **Divide and Conquer:**
    *   **Fork (Split):** Tasks are split recursively.
    *   **Process (Compute):** Leaf nodes (small chunks) are processed sequentially.
    *   **Join (Merge):** Results from sub-tasks are combined (e.g., using `combiner` in `reduce` or `collect`).

### Step-by-Step Execution:
1.  **Split Phase:** The collection is partitioned into multiple chunks.
2.  **Map Phase:** Each chunk is processed by a separate thread in the `ForkJoinPool`.
3.  **Reduce Phase:** The partial results are merged back together.

**Visual Flow:**
```
                [Source Collection]
                        |
            (Spliterator.trySplit())
           /            |            \
       [Chunk 1]    [Chunk 2]    [Chunk 3]
          |             |            |
      (Thread A)    (Thread B)   (Thread C)
          |             |            |
       [Process]     [Process]    [Process]
          \             |            /
           \            |           /
              [Combined Result]
```

---

## 4. Key Differences in Detail

### A. Threading Model
*   **Sequential:** No context switching overhead. Great for simple tasks.
*   **Parallel:** Significant overhead for creating tasks, forking, and joining. If the task is too small (e.g., `5 + 5`), the overhead exceeds the computation benefit.

### B. Order of Execution
*   **Sequential:** `forEach` respects the encounter order of the stream.
*   **Parallel:** `forEach` does **not** guarantee order (for performance reasons).
    *   **Fix:** Use `forEachOrdered()` if strict ordering is required in a parallel stream, but this kills the performance benefit of parallelism.

### C. The "State" Problem (Thread Safety)
Parallel streams are dangerous if used with **non-thread-safe** collections or stateful lambda expressions.

**Dangerous Code Example:**
```java
List<Integer> unsafeList = new ArrayList<>();
IntStream.range(0, 1000).parallel().forEach(i -> unsafeList.add(i));
// unsafeList size might be < 1000 or throw ArrayIndexOutOfBoundsException
// because ArrayList is NOT thread-safe.
```

**Correct Approach:**
Use thread-safe collectors or synchronized collections.
```java
List<Integer> safeList = IntStream.range(0, 1000)
    .parallel()
    .boxed()
    .collect(Collectors.toList()); // Collectors handle merging safely
```

---

## 5. When to use Parallel Stream? (The NQ Model)

A common heuristic is the **NQ Model**:
`N` = Number of elements.
`Q` = Cost per element (computational complexity).

Use Parallel Stream only if: **N * Q > 10,000 (roughly)**

1.  **Huge Data Amount (High N):** Processing 10 million integers.
2.  **Expensive Computation (High Q):** Each element involves heavy calculation (e.g., encryption, image processing, heavy regex).
3.  **Source is Splittable:** `ArrayList` splits easily (index-based). `LinkedList` splits poorly (requires traversal).

### When to AVOID Parallel Stream?
1.  **Blocking Operations:** Operations involving I/O (Database calls, File reading, Network calls).
    *   *Reason:* All parallel streams share the **same** `ForkJoinPool.commonPool()`. If you block threads here, you starve the entire JVM's parallel capability.
2.  **Small DataSets:** The overhead of splitting outweighs the benefit.
3.  **Boxing/Unboxing Overhead:** `IntStream` is faster than `Stream<Integer>`. Using objects in parallel might add GC overhead.

---

## 6. Configuring the Pool Size

By default, the parallel stream uses `ForkJoinPool.commonPool()`. You can change the parallelism level globally (not recommended) or wrap execution in a custom pool.

**Global Setting (JVM Argument):**
`-Djava.util.concurrent.ForkJoinPool.common.parallelism=4`

**Custom Pool (For specialized tasks):**
```java
ForkJoinPool customPool = new ForkJoinPool(4);
customPool.submit(() -> 
    myList.parallelStream().forEach(System.out::println)
).get();
```

---

## 7. Summary for Interviews

| Internal Component | Role |
| :--- | :--- |
| **Spliterator** | Responsible for traversing and partitioning elements (`trySplit`). |
| **ForkJoinPool** | The thread pool implementation (Work-Stealing algorithm). |
| **Work Stealing** | If a thread finishes its task early, it "steals" work from another busy thread's queue to stay productive. |
| **Combiner** | In operations like `reduce()`, this function merges partial results from different threads. |

