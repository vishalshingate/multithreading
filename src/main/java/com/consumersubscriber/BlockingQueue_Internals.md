# Internal Working of BlockingQueues in Java

Java's `BlockingQueue` implementations (like `ArrayBlockingQueue` and `LinkedBlockingQueue`) abstract away the low-level synchronization (wait/notify) we manually implemented. They use **ReentrantLocks** and **Conditions** internally to handle concurrency safely and efficiently.

---

## 1. ArrayBlockingQueue (Single Lock)

`ArrayBlockingQueue` is backed by a fixed-size array. It is simpler but has less concurrency potential because it uses a **single lock** for both production and consumption.

### Internal Structure
*   **Storage:** `Object[] items` (Fixed array)
*   **Lock:** `ReentrantLock lock` (One lock for everything)
*   **Condition 1:** `Condition notEmpty` (Wait here if queue is empty)
*   **Condition 2:** `Condition notFull` (Wait here if queue is full)

### Visual Diagram: The Single Gatekeeper

```text
       Producer Thread                       Consumer Thread
             |                                     |
             v                                     v
      +-------------------------------------------------------+
      |                   ReentrantLock                       | <--- Only ONE thread can be
      +-------------------------------------------------------+      inside this box at a time!
             |                                     |
    [Wait if FULL]                           [Wait if EMPTY]
    (await notFull)                          (await notEmpty)
             |                                     |
             v                                     v
      +-------------+                       +-------------+
      |  Enqueue()  |                       |  Dequeue()  |
      | (Add Item)  |                       | (Remove It) |
      +-------------+                       +-------------+
             |                                     |
      [Signal notEmpty]                     [Signal notFull]
      (Wake Consumer)                       (Wake Producer)
             |                                     |
             +------------------+------------------+
                                |
                          Unlock & Exit
```

### How `put(E e)` works internally:
1.  **Acquire Main Lock:** `lock.lockInterruptibly()`
2.  **Check Full:** `while (count == items.length)` -> `notFull.await()`
3.  **Insert:** Add item to array index. `count++`
4.  **Signal:** `notEmpty.signal()` (Wake up waiting consumers)
5.  **Release Lock:** `lock.unlock()`

---

## 2. Producer-Consumer Interview Questions

### Q1: Why use `while(queue.isEmpty())` instead of `if(queue.isEmpty())`?
**A:** Because of **Spurious Wakeups**. A thread might wake up from `wait()` without being notified, or multiple consumers might be notified but only one can consume. Using `while` ensures the thread re-checks the condition after waking up.

### Q2: What is the difference between `wait()` and `sleep()`?
| `wait()` | `sleep()` |
| :--- | :--- |
| Releases the monitor lock. | Holds the monitor lock. |
| Belong to `Object` class. | Belongs to `Thread` class. |
| Must be in a synchronized context. | Can be anywhere. |
| Wakes up on `notify()` or timeout. | Wakes up after time expires. |

### Q3: Why `notifyAll()` is generally preferred over `notify()`?
**A:** `notify()` only wakes up ONE random thread. If that thread can't proceed (e.g., it's a second producer when the queue is still full), the whole system might hang. `notifyAll()` ensures ALL threads wake up, so at least one capable thread proceeds.

---

## 3. Atomic vs Volatile vs Synchronized

### Volatile
*   Ensures **Visibility**: Any change made by one thread to the variable is immediately visible to others.
*   Prevents Instruction Reordering.
*   Does **NOT** ensure atomicity. (e.g., `count++` is not safe).

### Atomic (e.g., AtomicInteger)
*   Ensures **Visibility AND Atomicity**.
*   Uses **CAS (Compare-And-Swap)** logic at the CPU level.
*   Non-blocking (faster than synchronized for simple increments).

### Synchronized
*   Ensures **Visibility, Atomicity, AND Mutual Exclusion**.
*   Blocking (heavyweight).
*   Can sync multiple lines of code.

---

## 4. Deadlocks

### If a thread goes into BLOCKED state, does it release locks?
**NO.** This is a common misconception.
*   If a thread is **Blocked** (waiting to enter a synchronized block), it doesn't have the lock yet.
*   If a thread is **Waiting** (`wait()`), it **releases** the lock.
*   If a thread is **Timed_Waiting** (`sleep()`), it **keeps** the lock.

**Why Deadlock happens?**
Deadlock happens when Thread-1 holds Lock-A and waits for Lock-B, while Thread-2 holds Lock-B and waits for Lock-A. Since neither releases their held lock, both are stuck forever.

---

## 5. Semaphore vs CountDownLatch

### Semaphore
*   A "Counter" that allows **N** threads to access a resource.
*   Used for **Throttling** (e.g., limiting DB connections to 10).
*   Threads "acquire" a permit and "release" it when done.

### CountDownLatch
*   A "one-time" gate.
*   One or more threads wait until the latch reaches zero.
*   Once zero, it cannot be reset (unlike CyclicBarrier).
*   Example: Main thread waits for 3 microservices to start up.
