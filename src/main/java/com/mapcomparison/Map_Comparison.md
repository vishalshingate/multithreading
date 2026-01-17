# Map Comparison: HashMap vs Hashtable vs SynchronizedMap vs ConcurrentHashMap

This is a classic interview topic. The key differences revolve around **Thread Safety**, **Performance**, and **Null handling**.

---

## 1. Comparison Table

| Feature | HashMap | Hashtable | SynchronizedMap | ConcurrentHashMap |
| :--- | :--- | :--- | :--- | :--- |
| **Thread Safe?** | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes |
| **Null Key/Value** | ✅ Allowed | ❌ NPE | ✅ Allowed | ❌ NPE |
| **Performance** | 🚀 Fast | 🐢 Slow | 🐢 Slow | 🚀 Fast (Multi-threaded) |
| **Locking Mechanism** | None | Method-level `synchronized` | Object-level `synchronized(mutex)` | CAS + Node/Segment Locking |
| **Iterator Type** | Fail-Fast | Fail-Safe (Enumeration) | Fail-Fast | Fail-Safe (Deeply consistent) |
| **Legacy?** | No | Yes (Java 1.0) | No | No (Java 1.5) |

---

## 2. Detailed Breakdown

### A. HashMap (Not Thread Safe)
*   **Use when:** Single-threaded apps or read-only maps.
*   **Why fast?** No synchronization overhead.
*   **Nulls:** Allows 1 null key and multiple null values.

### B. Hashtable (Legacy - Do Not Use)
*   **Use when:** Never (unless maintaining ancient legacy code).
*   **Mechanism:** Every method (`put`, `get`, `remove`) is `synchronized`.
*   **Bottleneck:** Only **one** thread can access the map at a time. If Thread A is reading, Thread B cannot write.
*   **Nulls:** Throws `NullPointerException` for both keys and values.

### C. Collections.synchronizedMap(map)
*   **Use when:** You need to wrap a regular Map (like LinkedHashMap or TreeMap) to make it thread-safe quickly.
*   **Mechanism:** Wraps the original map in a wrapper class that uses a `synchronized(mutex)` block for every method call.
*   **Bottleneck:** Similar to Hashtable, it locks the **entire map object** for any operation. This causes high contention.
*   **Nulls:** Depends on the backing map (If HashMap, nulls are allowed).

### D. ConcurrentHashMap (The Modern Choice)
*   **Use when:** High-concurrency environments.
*   **Mechanism:**
    *   **Java 7:** Used "Segment Locking". Divided map into 16 segments. 16 threads could write simultaneously.
    *   **Java 8+:** Uses **CAS (Compare-And-Swap)** and **synchronized(Node)**. It only locks the specific **Bucket** (index of array) being written to.
    *   Reads (`get`) are **lock-free**!
*   **Nulls:** Strictly forbids null keys and values (to prevent ambiguity in concurrent scenarios). 'Is the value null, or is the key missing?' - `ConcurrentHashMap` avoids this confusion.

---

## 3. Summary: Which one to pick?

1.  **Thread Safety Needed?**
    *   **No:** Use `HashMap`.
    *   **Yes:** Go to step 2.

2.  **Concurrency Level?**
    *   **High (Many threads reading/writing):** Use `ConcurrentHashMap`. It scales much better.
    *   **Low, or need to verify Order (TreeMap):** Use `Collections.synchronizedMap()`.

