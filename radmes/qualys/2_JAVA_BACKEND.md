# Qualys Interview Guide - Part 2: Java Backend Questions

This document covers detailed answers for frequently asked **Core Java** and **Multithreading** questions for Qualys Senior Software Engineer (Backend).

---

## 1. Core Java OOPs

### A. OOP Pillars
1.  **Encapsulation:** Wrapping data (variables) and code (methods) together as a single unit. Achieved using `private` fields and `public` methods.
2.  **Abstraction:** Hiding implementation details and showing only functionality. Achieved using `abstract class` and `interface`.
3.  **Inheritance:** Mechanism where one class acquires properties of another. Promotes code reusability. (`extends`).
4.  **Polymorphism:** Ability to take many forms.
    *   **Compile-time:** Method Overloading.
    *   **Runtime:** Method Overriding.

### B. Immutable Class Design
**Characteristics:**
1.  Class is `final` (cannot be extended).
2.  All fields are `private` and `final`.
3.  No setter methods.
4.  Constructor initializes all fields (deep copy for mutable objects).
5.  Getter methods return a copy of mutable objects (defensive copy).

### C. equals() vs hashCode()
*   **Contract:** If two objects are equal (`o1.equals(o2)` is true), they **must** have the same `hashCode()`.
*   **Reverse:** If two objects have the same hash code, they are **not necessarily equal** (collision).
*   **Reason:** HashMap uses hashCode to find the bucket, then uses equals to find the specific object. If hashCode is inconsistent, HashMap breaks.

### D. final vs finally vs finalize
| Keyword | Context | Usage |
| :--- | :--- | :--- |
| **final** | Variable, Method, Class | Variable: Constant. Method: Cannot override. Class: Cannot extend. |
| **finally** | Exception Handling | Block that always executes (used for cleanup like closing streams). |
| **finalize** | Garbage Collection | Method called by GC before destroying an object (Deprecated in Java 9). |

### E. String vs StringBuilder vs StringBuffer
| Class | Mutability | Thread Safety | Performance |
| :--- | :--- | :--- | :--- |
| **String** | **Immutable** | Yes (inherently safe). | Slow for concatenation (creates new objects). |
| **StringBuffer** | **Mutable** | **Yes (Synchronized).** | Slower than StringBuilder due to synchronization. |
| **StringBuilder**| **Mutable** | **No (Not safe).** | **Fastest** (best for single-threaded loops). |

### F. Java Memory Model (JMM)
JMM defines how threads interact through memory.
1.  **Heap:** Shared memory for objects.
2.  **Stack:** Thread-local memory for methods and primitive variables.
3.  **Visibility:** Changes made by one thread to shared variables might not be visible to others immediately due to CPU cache.
    *   **volatile:** Ensures visibility (writes to main memory immediately).
    *   **synchronized:** Ensures atomicity and visibility.
4.  **Happens-Before Relationship:** Guarantees memory consistency.

---

## 2. Multithreading (Very Important for Qualys)

### A. Synchronization vs Lock
| Feature | Synchronization (`synchronized`) | ReentrantLock (`Lock`) |
| :--- | :--- | :--- |
| **Mechanism** | Implicit monitor lock. | Explicit lock object. |
| **Flexibility** | Wait/Notify must be in block. | `tryLock()`, simpler condition variables. |
| **Fairness** | Unfair (random thread selection). | Configurable (Fair lock possible). |
| **Release** | Automatic (end of block). | Manual (`unlock()` in `finally`). |

### B. wait() vs sleep() vs notify()
| Method | Class | Purpose | Lock Status |
| :--- | :--- | :--- | :--- |
| **wait()** | `Object` | Waits until notified. | **Releases** the lock. |
| **sleep()** | `Thread` | Pauses execution for time. | **Keeps** the lock. |
| **notify()** | `Object` | Wakes up **one** waiting thread. | Does not release lock immediately. |
| **notifyAll()**| `Object` | Wakes up **all** waiting threads. | Does not release lock immediately. |

### C. Deadlock Detection & Prevention
**Detection:**
1.  **jstack:** Thread dump analysis. Look for "Found one Java-level deadlock".
2.  **VisualVM:** Graphical tool to detect circular waits.

**Prevention:**
1.  **Lock Ordering:** Always acquire locks in a consistent order (e.g., sort resources by ID and lock smaller ID first).
2.  **Timeouts:** Use `tryLock(time)` instead of waiting indefinitely.
3.  **Use Higher-level Concurrency:** Use `java.util.concurrent` classes (ConcurrentHashMap, BlockingQueue) instead of manual synchronization.

### D. Thread Pools (Executor Framework)
**Why use?** Creating threads is expensive (OS resource). Pools reuse threads.
**Types:**
1.  **FixedThreadPool(n):** Fixed number of threads. Unbounded Queue. Good for steady load.
2.  **CachedThreadPool:** Creates threads as needed. Kills idle threads (60s). Good for many short tasks.
3.  **SingleThreadExecutor:** Sequential execution.
4.  **ScheduledThreadPool:** For periodic tasks.
**Core Parameters:**
*   `corePoolSize`: Minimum threads.
*   `maximumPoolSize`: Maximum threads.
*   `keepAliveTime`: Time before idle excess threads die.
*   `workQueue`: Queue for holding tasks (ArrayBlockingQueue, LinkedBlockingQueue).

---

## 3. Java 8 Features (Important for Interviews)

### A. Lambda Expressions & Functional Interfaces
*   **Lambda:** Anonymous function (`(args) -> body`). Concise implementation of single method interface.
*   **Functional Interface:** Interface with exactly one abstract method. (`@FunctionalInterface`).
    *   `Predicate<T>`: `boolean test(T t)`
    *   `Consumer<T>`: `void accept(T t)`
    *   `Supplier<T>`: `T get()`
    *   `Function<T,R>`: `R apply(T t)`

### B. Stream API (`java.util.stream`)
Allows declarative processing of collections.
*   **Intermediate (Lazy):** `filter`, `map`, `sorted`, `distinct`, `peek`.
*   **Terminal (Eager):** `collect`, `forEach`, `reduce`, `count`, `anyMatch`.
*   **Parallel Stream:** Uses ForkJoinPool common pool for parallel processing. `list.parallelStream()`.

**Example:**
```java
List<String> result = list.stream()
    .filter(s -> s.startsWith("A"))
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

### C. Optional Class (`java.util.Optional`)
Container to avoid `NullPointerException`.
*   `Optional.of(value)`: throws NPE if null.
*   `Optional.ofNullable(value)`: allows null.
*   `user.ifPresent(u -> print(u))`
*   `user.orElse(defaultUser)`

### D. Default & Static Methods in Interfaces
*   **Default Methods:** Concrete methods inside interface using `default` keyword. Used for backward compatibility (e.g., `stream()` in `Collection`).
*   **Static Methods:** Utility methods inside interface (e.g., `Stream.of()`).

### E. Method References (`::`)
Shorthand for lambda calling a specific method.
*   `System.out::println` (Instance method)
*   `Math::max` (Static method)
*   `String::new` (Constructor reference)

### F. Date/Time API (`java.time`)
Immutable and Thread-safe.
*   `LocalDate`, `LocalTime`, `LocalDateTime`, `ZonedDateTime`, `Duration`, `Period`.
