# Synchronized Block vs ReentrantLock

Both `synchronized` and `ReentrantLock` (from `java.util.concurrent.locks`) are used to achieve thread synchronization. However, `ReentrantLock` offers more advanced features and flexibility.

| Feature | Synchronized Block | ReentrantLock |
| :--- | :--- | :--- |
| **Type** | Implicit (built-in keyword) | Explicit (Class implementation) |
| **Locking Mechanism** | Automatic (JVM handles acquire/release) | Manual (Programmer must call `lock()` and `unlock()`) |
| **Flexibility** | Low. Must release lock in same block. | High. Can lock in one method, unlock in another. |
| **Fairness** | **Unfair**. Random thread gets lock clearly. | Configurable. `new ReentrantLock(true)` grants lock to longest waiting thread. |
| **Waiting** | **Blocks Forever**. Can't check if lock is free. | **Timeouts**. `tryLock(time)` allows failing if lock is busy. |
| **Interruption** | Impossible. Thread blocked on `synchronized` cannot be interrupted. | Possible. `lockInterruptibly()` allows interrupting a waiting thread. |
| **Condition Support** | Single wait set (`wait/notify`) | Multiple conditions (`newCondition()`) allow selective waking. |

## 1. Synchronized
*   **Best for:** Simple synchronization where you just need mutual exclusion.
*   **Pros:** Less code, impossible to forget releasing lock (exception safe).
*   **Cons:** Limited control.

```java
synchronized(obj) {
    // Critical section
} // Lock automatically released
```

## 2. ReentrantLock
*   **Best for:** Complex scenarios requiring timeouts, fairness, or multiple condition variables.
*   **Pros:** Powerful features (`tryLock`, fairness).
*   **Cons:** More boilerplate code. **MUST** use `try-finally` to ensure unlock.

```java
Lock lock = new ReentrantLock();
lock.lock();
try {
    // Critical section
} finally {
    lock.unlock(); // CRITICAL! If you forget this -> Deadlock
}
```

## Special Features of Locks

### A. TryLock (Non-blocking)
Instead of getting stuck waiting, you can check if the lock is available.
```java
if (lock.tryLock()) {
    try { /* do work */ } finally { lock.unlock(); }
} else {
    // Do something else, don't wait
}
```

### B. Fairness
Prevents "Thread Starvation".
```java
// True = Fair (FIFO order for threads)
Lock lock = new ReentrantLock(true);
```

### C. Multiple Conditions
Allows specialized waiting rooms (e.g., "Full" vs "Empty" in Producer-Consumer).
```java
Condition notFull = lock.newCondition();
Condition notEmpty = lock.newCondition();
```

