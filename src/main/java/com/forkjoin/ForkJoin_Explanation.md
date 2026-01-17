# ForkJoinPool & Work Stealing (Java 7/8+)

`ForkJoinPool` is a specialized implementation of `ExecutorService` designed for **"Divide and Conquer"** algorithms (Recursive tasks).

The JDK 8 convenience method `Executors.newWorkStealingPool()` is simply a factory that creates a `ForkJoinPool` configured to use the number of available processors as its target parallelism level.

---

## 1. The Work-Stealing Algorithm

Standard Thread Pools (e.g., `FixedThreadPool`) have **One Common Queue**. All threads fight to grab tasks from it (Lock Contention).

`ForkJoinPool` is different:

1.  **Deque per Thread:** Every worker thread has its own **Double-Ended Queue (Deque)**.
2.  **Push/Pop (LIFO):** When a thread creates a new sub-task (using `fork()`), it pushes it to the **Head** of its own deque. It processes its own tasks like a Stack (Last-In-First-Out). Ideally, it never touches shared memory.
3.  **Stealing (FIFO):** If a thread runs out of tasks (its deque is empty), it becomes a "Thief". It looks at another busy thread's deque and "steals" a task from the **Tail**.

### Visualizing Work Stealing

```text
[Thread A Deque]       [Thread B Deque] (Empty!)
| Task 4 (Head) |      |               |
| Task 3        |      |  (Looking...) |
| Task 2        |      |               |
| Task 1 (Tail) | <--- | Steals Task 1 |
```

*   **Thread A** works on Task 4 (Newest / Smallest sub-task).
*   **Thread B** steals Task 1 (Oldest / Largest chunk).
*   **Benefit:** Stealing from the tail minimizes conflict. The thief takes the biggest available chunk of work, so it won't need to steal again soon.

---

## 2. When to use `newWorkStealingPool`?

| Scenario | Use `FixedThreadPool` | Use `WorkStealingPool` |
| :--- | :--- | :--- |
| **Task Type** | Independent, small tasks (e.g., Handling HTTP requests, DB queries). | Recursive, "Divide and Conquer" (e.g., Recursion, Sort, Image Processing). |
| **Logic** | Linear processing. | Nested sub-tasks (`fork()` / `join()`). |
| **Blocking?** | Good for IO Blocking tasks. | **BAD** for Blocking. Threads should compute, not sleep. |
| **Load Balance**| Can be uneven if one task takes forever. | **Self-Balancing.** Idle threads help busy threads. |
| **Order** | FIFO (First In First Out). | LIFO (for local tasks) / FIFO (for steals). |

## 3. How to use it?

### Option A: The Wrapper (Java 8+)
This is just a shortcut.
```java
// Creates a ForkJoinPool with parallelism = Runtime.getRuntime().availableProcessors()
ExecutorService executor = Executors.newWorkStealingPool();
```

### Option B: Direct Instantiation (More Control)
```java
ForkJoinPool pool = new ForkJoinPool(4); // limit to 4 threads
```

### Option C: RecursiveTask (The "Job")
To actually use the stealing capability, your tasks must split themselves!
*   **`RecursiveTask<T>`**: Returns a result (like `Callable`).
*   **`RecursiveAction`**: Returns void (like `Runnable`).

```java
class MyTask extends RecursiveTask<Integer> {
    compute() {
        if (task is small) return calc();
        
        // Split
        left = new MyTask();
        left.fork(); // Async push to deque
        
        right = new MyTask();
        resultRight = right.compute(); // Compute sync on this thread
        
        resultLeft = left.join(); // Wait for result (or help run it)
        
        return resultLeft + resultRight;
    }
}
```

