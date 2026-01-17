# TreeMap Under the Hood

`TreeMap` is a map implementation that keeps its entries **sorted** according to the natural ordering of its keys, or by a specific `Comparator` provided at creation time.

## 1. Internal Data Structure: Red-Black Tree

Unlike `HashMap` which uses an Array of Buckets + Linked Lists/Trees (Hashing), `TreeMap` uses a **Red-Black Tree**.

### What is a Red-Black Tree?
It is a type of **Self-Balancing Binary Search Tree (BST)**.
*   **Binary Search Tree:** For every node, all left descendants are smaller, and all right descendants are bigger.
*   **Self-Balancing:** It ensures the tree height remains `O(log n)` even if you insert sorted data (e.g., 1, 2, 3, 4, 5). A normal BST would become a Linked List (skewed) with `O(n)` search time in that case.

### Why not AVL Tree?
AVL trees are strictly balanced (faster lookups), but Red-Black trees are "loosely" balanced. This makes insertion/deletion faster in Red-Black trees because fewer rotations are needed to restore balance. This is why Java chose Red-Black Trees.

---

## 2. Key Characteristics

| Feature | Details |
| :--- | :--- |
| **Ordering** | Sorted (Key Natural Order or Comparator). |
| **Time Complexity** | **O(log n)** for `get`, `put`, `remove`, `containsKey`. |
| **Null Keys** | **NOT Allowed** (Throws NPE). It needs to compare keys. |
| **Null Values** | Allowed. |
| **Interfaces** | Implements `NavigableMap` (extends `SortedMap`). |
| **Thread Safety** | **No.** Use `Collections.synchronizedSortedMap()` or `ConcurrentSkipListMap`. |

---

## 3. How `put(K key, V value)` works

1.  **Root Check:** If tree is empty, create new Root (Black).
2.  **Traversal:** Start from Root.
    *   Compare `key` with `current.key`.
    *   If `key < current.key` -> Go Left.
    *   If `key > current.key` -> Go Right.
    *   If `key == current.key` -> Replace Value (Update).
3.  **Insertion:** If we hit a `null` child, insert the new **RED** node there.
4.  **Rebalancing (The Magic):**
    *   Since we inserted a RED node, we might violate Red-Black properties (e.g., "No two red nodes in a row").
    *   The algorithm performs **Color Flips** and **Tree Rotations** (Left/Right Rotate) to fix the tree structure and guarantee `O(log n)` height.

---

## 4. Visual Comparison

**HashMap (Unordered)**
```text
[0] -> "Apple"
[1] -> "Banana"
[2] -> "Zen"  <-- Random order based on HashCode
```

**TreeMap (Sorted)**
```text
      "Banana"
       /    \
  "Apple"  "Zen"
```
Order is always guaranteed: Apple -> Banana -> Zen.

---

## 5. When to use TreeMap?

1.  **Sorted Data:** When you need to iterate keys in alphabetical or numerical order.
2.  **Range Queries:** "Give me all users between Age 20 and 30". (`subMap(20, 30)`).
3.  **Nearest Neighbor:** "Who is the user with ID closest to 1005?" (`ceilingKey(1005)` / `floorKey(1005)`).

**Do NOT use TreeMap** if you just need fast lookup (`O(1)` vs `O(log n)`) and don't care about order. Use `HashMap` instead.

