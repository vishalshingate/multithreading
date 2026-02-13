# HashMap Internals: Interview Guide

This guide explains how `HashMap` works internally in Java 8+, focusing on `put()` and `get()` operations. This is one of the most critical interview topics for Java developers.

---

## 1. Internal Structure (The "Bucket Array")

Internally, a HashMap is an **Array of Nodes** (often called a "Table").

```java
transient Node<K,V>[] table;
```

Each `Node` object contains 4 things:
1.  **int hash**: The hash code of the key.
2.  **K key**: The actual key object.
3.  **V value**: The value object.
4.  **Node next**: Reference to the next node (for Linked List).

### Visual Diagram: The Array

```text
Index:  0    1    2    3    4    ...  15
      [null|null|Node|null|Node|... |null]
                  |         |
                  v         v
                (Key A)   (Key B) --> (Key C)
```
*   Each index is called a **Bucket**.
*   If multiple keys land in the same bucket, they form a **Linked List** (or **Red-Black Tree**).

---

## 2. PUT Operation Flow (`map.put(K, V)`)

When you call `put("Key", "Value")`, the following steps happen:

**Step 1: Hashing**
*   Calculate `key.hashCode()`.
*   Apply **High-Bit Spreading** (XOR Shift) to ensure randomness.
    *   Internal: `(h = key.hashCode()) ^ (h >>> 16)`

**Step 2: Index Calculation**
*   Calculate the bucket index using bitwise AND:
    *   `index = (n - 1) & hash` (Where `n` is array size).
    *   *Note:* This is efficient logic for modulo operator `%`.

**Step 3: Check Bucket**
*   **Case A: Bucket is Empty (null)**
    *   Create a new Node and place it there.
*   **Case B: Bucket is NOT Empty (Collision)**
    *   This means another Key is already sitting here.
    *   Compare the **hash** and **equals()** of the existing key.
    *   **If Key exists:** Update the `value`.
    *   **If Key is new:**
        *   **Traverse the List/Tree:** Go to the end.
        *   **Insert:** Add new Node at the end.
        *   **Treeify Check:** If the List size > 8 (TREEIFY_THRESHOLD), convert the Linked List into a **Red-Black Tree** for O(log n) performance.

**Step 4: Resize Check**
*   If `size > capacity * loadFactor` (default 16 * 0.75 = 12), trigger **Resize**.
    *   Double the array size.
    *   Re-hash every single entry to new positions.

### Visual Flow Code
```java
public V put(K key, V value) {
    if (table is empty) resize();
    
    int index = (n - 1) & hash(key);
    Node p = table[index];

    if (p == null) {
        table[index] = newNode(hash, key, value, null);
    } else {
        if (p.key.equals(key)) {
            p.value = value; // Update
        } else {
            // Handle Collision (Walk List or Tree)
            p.next = newNode(...);
        }
    }
}
```

---

## 3. GET Operation Flow (`map.get(K)`)

When you call `get("Key")`, steps are simpler:

**Step 1: Hashing & Index**
*   Calculate `hash` and `index` (Same as PUT).

**Step 2: Lookup**
*   Go to `table[index]`.
*   **If bucket is null:** Return `null`.
*   **If bucket has Node:**
    1.  Compare `node.hash == hash`.
    2.  Compare `node.key == key` (Reference check `==`) OR `node.key.equals(key)`.
    3.  If Match: Return `node.value`.
    4.  If No Match: Move to `node.next` and repeat.

---

## 4. Key Interview Concepts

### A. Collision Handling (Java 8 vs Java 7)
*   **Collision:** Two different keys hash to the same index.
*   **Java 7:** Used a Linked List. Worst case lookup was **O(n)**. This was a security risk (Hash DoS Attack).
*   **Java 8:** Uses Linked List initially. If list grows > 8, it becomes a **Red-Black Tree**. Worst case lookup involves **O(log n)**.

### B. Why is size always power of 2?
*   Default size is 16.
*   It allows the index calculation to use efficient Bitwise operations: `(n - 1) & hash`.
*   If size was not power of 2, we would have to use modulo `%`, which is slower.

### C. The Contract (HashCode & Equals)
*   If you override `equals()`, you **MUST** override `hashCode()`.
*   If `a.equals(b)` is true, `a.hashCode()` MUST equal `b.hashCode()`.
*   HashMap relies entirely on this. If `.equals()` says true but hashCodes are different, the map will look in the wrong bucket and return null.

### D. Complexity
| Operation | Average | Worst Case (Java 8) |
| :--- | :--- | :--- |
| `get()` | O(1) | O(log n) |
| `put()` | O(1) | O(log n) |

---

## 5. Using Custom Objects as Keys

Yes, you can use any custom object as a key in a `HashMap`. However, to ensure the map functions correctly, you **must** follow these rules:

### A. The Contract: `hashCode()` and `equals()`
If you use a custom object as a key, you must override both methods:
1.  **`hashCode()`**: Determines which bucket the key goes into. If two keys are "equal," they must have the same hash code.
2.  **`equals()`**: Used to find the exact key within a bucket during a collision.

### B. Immutability (The Gold Standard)
It is highly recommended to make your custom key classes **immutable**:
- Declare fields as `private final`.
- Do not provide setters.
- **Why?** If the state of a key changes *after* it has been put into the map, its `hashCode` will likely change. The next time you try to `get()` it, the map will look in the wrong bucket, and the object will be "lost" inside the map.

### C. Example of a Correct Custom Key
```java
public final class UserKey {
    private final String userId;

    public UserKey(String userId) {
        this.userId = userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserKey)) return false;
        UserKey that = (UserKey) o;
        return Objects.equals(userId, that.userId);
    }
}
```

### D. Real-World Enterprise Scenarios

In high-scale enterprise applications, you rarely use raw Strings as keys. Custom objects are used to handle **Multi-tenancy**, **Caching**, and **Grouping**.

#### 1. Multi-Tenant Session Cache
In a SaaS application, a `userId` might be unique only *within* a specific `tenantId`. Using a custom key ensures that User 101 from "Company A" is different from User 101 from "Company B".

```java
public final class TenantUserKey {
    private final String tenantId;
    private final String userId;

    public TenantUserKey(String tenantId, String userId) {
        this.tenantId = tenantId;
        this.userId = userId;
    }
    // equals() and hashCode() using both fields
}

// Usage in an Auth Service
Map<TenantUserKey, UserSession> sessionCache = new ConcurrentHashMap<>();
```

#### 2. Resource Throttling / Rate Limiting
If you need to limit how many requests a user can make to a *specific* API endpoint, you combine them into a key.

```java
public final class ThrottlingKey {
    private final String userId;
    private final String apiEndpoint;
    // ...
}

// Store the request count for that specific user on that specific endpoint
Map<ThrottlingKey, AtomicInteger> requestCounts = new HashMap<>();
```

#### 3. Data Aggregation (Complex Grouping)
When processing millions of records (e.g., in a financial system), you might need to group totals by `Region`, `Branch`, and `Currency`. Instead of nested maps, a single `HashMap<FinancialKey, Double>` is much faster.

```java
public final class FinancialKey {
    private final String region;
    private final String branchId;
    private final String currency;
    // ... equals and hashcode ...
}
```

---

## 6. LinkedHashMap: Why it's a Special Choice

`LinkedHashMap` extends `HashMap` and provides two major advantages that make it suitable for specific high-performance and predictable scenarios.

### A. Maintains Order
Unlike `HashMap` (which is unordered) and `TreeMap` (which is sorted by key value), `LinkedHashMap` maintains a **Doubly-Linked List** of all its entries.
- **Insertion Order (Default):** Iterating the map returns elements in the exact same order they were inserted.
- **Access Order:** If you initialize it with `new LinkedHashMap<>(16, 0.75f, true)`, it moves any element you `get()` to the **end** of the list.

### B. The LRU Cache (Least Recently Used)
Because of its "Access Order" property, `LinkedHashMap` is the foundation of almost every manual LRU cache in Java.
- You can override `removeEldestEntry()` to automatically delete the oldest item when the map reaches a certain size.

```java
// Example of a 100-item LRU Cache
Map<K, V> lruCache = new LinkedHashMap<K, V>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > 100;
    }
};
```

### C. Faster Iteration
- **HashMap Iteration:** O(Capacity). You have to check every single bucket in the array.
- **LinkedHashMap Iteration:** O(Size). You simply follow the linked list pointers from head to tail. If you have a map with 10 elements but an array size of 10,000, `LinkedHashMap` will be significantly faster to loop through.

### Summary Comparison
| Property | HashMap | LinkedHashMap | TreeMap |
| :--- | :--- | :--- | :--- |
| **Ordering** | None | Insertion or Access Order | Sorted (Natural or Comparator) |
| **Data Structure** | Array + List/Tree | Array + List/Tree + **Doubly Linked List** | Red-Black Tree |
| **Search Time** | O(1) | O(1) | O(log n) |
| **Null Keys** | Allowed | Allowed | Disallowed |

