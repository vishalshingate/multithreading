# Fail-Fast vs Fail-Safe Iterators in Java

In Java Collections, iterators are classified into two main types based on how they handle **Concurrent Modifications** (modifying the collection structure while iterating over it).

---

## 1. Fail-Fast Iterators

Fail-Fast iterators immediately throw a `ConcurrentModificationException` if they detect that the collection has been modified (elements added, removed, or cleared) by anyone other than the iterator itself.

*   **Behavior:** Checks for modifications (via `modCount`) every time you call `next()`.
*   **Collections:** `ArrayList`, `HashMap`, `HashSet`, `Vector`.
*   **Mechanism:** Uses a `modificationCount` counter. If `iterator.expectedModCount != list.modCount`, it throws an exception.
*   **Goal:** To prevent unpredictable behavior instantly (Fail Fast).

### Example of Failure
```java
List<String> list = new ArrayList<>();
list.add("A");
list.add("B");

Iterator<String> it = list.iterator();
while(it.hasNext()) {
    String s = it.next();
    if(s.equals("A")) {
        list.remove("A"); // CRASH! ConcurrentModificationException
    }
}
```

### How to Fix?
Use the iterator's own remove method:
```java
it.remove(); // Safe
```

---

## 2. Fail-Safe Iterators (Weakly Consistent)

Fail-Safe iterators do **NOT** throw `ConcurrentModificationException`. They guarantee traversal of elements as they existed upon iterator construction, and may (or may not) reflect subsequent modifications.

*   **Behavior:** Works on a **copy** of the underlying data OR uses a tolerance mechanism to handle changes without crashing.
*   **Collections:** `CopyOnWriteArrayList`, `ConcurrentHashMap`.
*   **Mechanism:**
    *   **CopyOnWriteArrayList:** The iterator holds a reference to the *original* array snapshot. Any modification creates a *new* array, so the iterator continues reading the old one undisturbed.
    *   **ConcurrentHashMap:** uses a complex traversal that permits concurrent updates.
*   **Goal:** To allow concurrent read/write without locking or crashing.

### Example of Safety
```java
List<String> list = new CopyOnWriteArrayList<>();
list.add("A");
list.add("B");

Iterator<String> it = list.iterator(); // Snapshot taken here
while(it.hasNext()) {
    String s = it.next();
    if(s.equals("A")) {
        list.add("C"); // Works fine. But Iterator might NOT see "C".
    }
}
```

---

## Comparison Table

| Feature | Fail-Fast Iterator | Fail-Safe (Weakly Consistent) Iterator |
| :--- | :--- | :--- |
| **Exception** | Throws `ConcurrentModificationException` | No Exception thrown |
| **Memory** | Low overhead (uses original collection) | High overhead (might copy entire array) |
| **Consistency** | Strictly consistent check | Weakly consistent (might iterate old data) |
| **Examples** | `ArrayList`, `HashMap`, `HashSet` | `CopyOnWriteArrayList`, `ConcurrentHashMap` |
| **Use Case** | Single-threaded or strictly Synchronized code | Multi-threaded concurrent environments |

