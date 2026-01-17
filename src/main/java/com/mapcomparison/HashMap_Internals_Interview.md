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

