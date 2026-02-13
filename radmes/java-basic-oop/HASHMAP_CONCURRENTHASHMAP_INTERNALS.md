# HashMap & ConcurrentHashMap Internals (JDK 7 vs JDK 8+)

A concise breakdown of how these maps work under the hood, including version-specific behaviors.

---

## Quick Reference
- **HashMap**: Not thread-safe; array of buckets; collisions -> list (7) or list/tree (8+); resize on load factor (default 0.75).
- **ConcurrentHashMap**: Thread-safe without global lock; JDK 7 uses segmented locks; JDK 8+ uses CAS + bin-level locking; weakly consistent iterators; disallows null keys/values.

---

## HashMap Internals

### Layout & Collision Handling
- Array of buckets sized to a power of two (for cheap `index = hash & (n-1)`).
- **JDK 7**: Buckets hold singly-linked lists (`Entry`). Collisions append at head.
- **JDK 8+**: Buckets hold `Node` list; if a bucket gets too dense, it treeifies to a red-black tree for lookup O(log n).

### Treeification (JDK 8+)
- Thresholds: `TREEIFY_THRESHOLD = 8`, `UNTREEIFY_THRESHOLD = 6`.
- Treeification only happens when table size >= `MIN_TREEIFY_CAPACITY = 64`; otherwise it resizes first to spread keys.

#### Why wait until capacity 64?
If a bucket has too many collisions (>= 8) but the total array size is small (< 64), immediate treeification is not the best move.
1. **Resizing is better**: For small arrays, resizing (doubling the array size) is more effective because it redistributes the keys into new buckets, reducing the collision chain length naturally.
2. **Memory Overhead**: Tree nodes (`TreeNode`) are about twice the size of standard nodes. Converting to trees is expensive and changes the memory footprint.
3. **Avoidance**: We prefer to use array resizing to handle high load factors. Treeification is reserved as a fallback for when the array is already "large enough" but collisions persist (often due to a poor `hashCode` implementation or malicious keys).

### Resizing
- Trigger: `size > capacity * loadFactor` (default loadFactor = 0.75, initial capacity 16 if unspecified).
- **JDK 7**: Resize + rehash; head insertion during transfer could cause cycle under concurrent unsynchronized access (classic infinite-loop bug).
- **JDK 8+**: Resizes by splitting each bucket: entries either stay at index `i` or move to `i + oldCap` based on the extra hash bit. This avoids full rehash of each key.

### Hash Mixing (simplified)
- 7: `hash(key)` with supplemental shift/xor to spread higher bits.
- 8+: Uses `spread(hashCode) = h ^ (h >>> 16)` to mix upper bits into lower bits.

### Complexity
- Lookup/insert/remove average O(1); worst-case:
  - 7: O(n) per bucket due to linked list.
  - 8+: O(log n) when treeified, otherwise O(n) for that bucket.

### Iterators
- Fail-fast (best-effort): throw `ConcurrentModificationException` if structurally modified (non-iterator) during iteration.
- Not safe under concurrent writes without external synchronization.

### Custom Objects as Keys
Can we use custom objects as keys in a HashMap? **Yes.**

To do this correctly, you **MUST** follow these rules:
1.  **Override `hashCode()`**: So that equal objects produce the same hash and land in the same bucket.
2.  **Override `equals()`**: To distinguish between different objects in the same bucket (collisions).
3.  **Immutability (Highly Recommended)**: If the state of your object changes after being used as a key, its `hashCode` will change. The map will look in the wrong bucket next time, and your entry will be "lost" inside the map.

**Example:**
```java
public final class User { // Final to prevent inheritance
    private final String id; // Final for immutability
    private final String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(name, user.name);
    }
}
```

---

## ConcurrentHashMap Internals

### Null Policy
- Null keys/values are forbidden to avoid ambiguity in concurrent lookups.

### Iterators
- Weakly consistent: reflect some (not necessarily all) updates made after iterator creation; never throw CME.

### What does "Weakly Consistent" mean?
Unlike "Fail-Fast" iterators (which throw `ConcurrentModificationException` if the collection changes during iteration), a **Weakly Consistent** iterator:
1. **Never** throws `ConcurrentModificationException`.
2. Guarantees to traverse elements as they existed upon construction.
3. **May** (but is not guaranteed to) reflect modifications (inserts/removes) that happen *after* the iterator was created.
4. Guaranteed to go through the collection **exactly once** for each element present at start (unless removed).

**Analogy**: Imagine reading a book while someone else is editing the later chapters. You will definitely read the original chapters. You *might* see their edits if you haven't reached that page yet, but the book won't explode in your hands.

### JDK 7 Design (Segmented Locks)
- Structure: Fixed-size array of `Segment` (default 16). Each segment is a mini-HashMap with its own `ReentrantLock` and table.
- Operations:
  - `get`: Lock-free; volatile reads of segment and buckets.
  - `put/remove`: Lock only the target segment; resize per-segment.
- Concurrency: Up to segment-count writers can proceed; readers are mostly lock-free.

### JDK 8+ Design (Lock Striping via Bins + CAS)
- Structure: Single table (`Node[]`).
- `get`: Fully lock-free; reads volatile table + nodes.
- `put`: Uses CAS to init bins; otherwise synchronized on bin head for that index; resizes with cooperative threads.
- Resizing: Uses `ForwardingNode` to mark moved bins; threads help transfer.
- Collision handling: Linked list -> tree bin (red-black) when threshold (8) met and table large enough, similar to HashMap.
- Additional ops: `compute*`, `merge`, `forEach`, `reduce` use built-in fork-join parallelism where possible.

### Contention Model
- Fine-grained: Only one bin lock at a time (vs segment lock in 7). CAS reduces contention for empty bins.
- Readers stay non-blocking.

### Complexity
- Average O(1) for `get/put/remove`; worst-case O(log n) for treeified bins.

---

## Deep Dive: ConcurrentHashMap Locking (JDK 8+)

Unlike JDK 7 which used ~16 "Segments" (locks), JDK 8+ uses **Lock Striping** on the individual bin heads. This means the theoretical concurrency level is equal to the number of bins (table size).

### The "Put" Operation Flow
1. **Spread Hash**: Calculate hash using `spread(key.hashCode())`.
2. **Loop (Infinite Loop)**:
   - **Case 1: Bin is Empty**:
     - Use **CAS** (Compare-And-Swap) to insert the new node.
     - **Constraint**: No `synchronized` lock. Extremely fast for sparse maps.
     - If CAS fails (other thread beat us), retry loop.
   - **Case 2: Resizing in Progress (Hash == MOVED)**:
     - The node found is a `ForwardingNode` (hash = -1).
     - **Action**: The current thread joins the effort and **helps resize** (transfers data) before continuing. It doesn't block waiting for resize to finish, it contributes work.
   - **Case 3: Collision (Bin Occupied)**:
     - **Lock**: `synchronized (f)` where `f` is the first node in the bucket (the head).
     - **Action**: Traverse list/tree to update value or append new node.
     - **After Unlock**: Check if bin needs treeification (count >= 8).

### Diagram: JDK 8+ Put Logic

```text
       [ Start put(key, val) ]
                  |
        (Calculate Hash & Index)
                  |
                  v
       +-----------------------+
       |   Read Node at Index  | <---( Volatile Read )
       +-----------------------+
                  |
        +---------+------------+
        |                      |
    (Is Null?)           (Is Not Null?)
        |                      |
        v                      v
  +-----------+         +-------------+
  |    CAS    |         | Check Hash  |
  | (No Lock) |         +-------------+
  +-----+-----+                |
        |               +------+------+
     (Success?)         |             |
     /      \       (MOVED?)      (Normal Node?)
   Yes       No         |             |
    |        |          v             v
 [Done]    (Retry)  [Help Resize]  [Synchronize(Node)]
                                      |
                                  +---+---+
                                  | Update|
                                  | List/ |
                                  | Tree  |
                                  +-------+
```

### Why this is better?
1. **Granularity**: Only the specific bucket being modified is locked.
2. **Empty Bins**: No locking at all (CAS).
3. **Reads**: `get()` is completely lock-free (volatile reads), never blocks.

---

## Practical Differences & Guidance
- Use **HashMap** for single-threaded or externally synchronized contexts.
- Use **ConcurrentHashMap** for multi-threaded access where higher read scalability and safe mutations are needed.
- Avoid using `Collections.synchronizedMap(new HashMap<>())` for high-contention cases; it serializes all access on one lock.
- For predictable iteration order, use `LinkedHashMap` (single-threaded) or wrap with explicit locks for concurrency.

---

## Version Comparison Snapshot

| Topic | HashMap JDK 7 | HashMap JDK 8+ | ConcurrentHashMap JDK 7 | ConcurrentHashMap JDK 8+ |
| --- | --- | --- | --- | --- |
| Collision form | Linked list | List -> Tree (RB) after threshold | Segments of lists | Single table: list -> tree |
| Resize | Rehash all; head insertion (risk in races) | Split by high bit; safer | Per-segment | Global table with cooperative transfer |
| Concurrency | None | None | Segment locks (ReentrantLock) | Bin-level synch + CAS; readers lock-free |
| Iterators | Fail-fast | Fail-fast | Weakly consistent | Weakly consistent |
| Null keys/values | Allowed | Allowed | Disallowed | Disallowed |

---

## Tiny Visuals (ASCII)

HashMap bucket (JDK 8+ treeified):
```
index i -> [RB Tree of Node<K,V>]
index j -> [Node] -> [Node] -> null
```

ConcurrentHashMap JDK 8+ put (simplified):
```
compute hash -> bin index
if bin empty: CAS place Node (no lock)
else if bin is ForwardingNode: help resize, retry
else synchronized on bin head: insert/update; treeify if needed
```

---

## Common Interview Pointers & Answers

### 1. HashMap treeification: Why wait until capacity >= 64?
- **Thresholds**: `TREEIFY_THRESHOLD = 8`, `UNTREEIFY_THRESHOLD = 6`, and `MIN_TREEIFY_CAPACITY = 64`.
- **Rationale**: Tree nodes (`TreeNode`) are roughly twice the size of standard nodes. Converting to a tree is expensive in terms of memory. If the array is small (< 64), it's more efficient to just **resize** (double the capacity). Resizing redistributes keys into new buckets, which naturally reduces collision chain lengths without the memory overhead of a Red-Black tree.

### 2. Why is HashMap unsafe concurrently?
- **Lost Updates**: If two threads call `put()` for different keys that hash to the same bucket, they might both see the same "next" node and attempt to link their new node to it. One thread will overwrite the other's work.
- **Infinite Loop (JDK 7)**: During resize, JDK 7 used "head-insertion" to move entries. In a concurrent race, pointers can be corrupted to form a **circular dependency** (e.g., Node A.next = B and Node B.next = A). A subsequent `get()` on that bucket will enter an infinite loop, spiking CPU to 100%.
- **Data Corruption (JDK 8+)**: While JDK 8+ uses "tail-insertion" to prevent the infinite loop bug, it is still not thread-safe and can suffer from corrupted tree structures or incorrect size counts.

### 3. ConcurrentHashMap null policy rationale?
- **Ambiguity**: In a concurrent environment, if `map.get(key)` returns `null`, you don't know if the key is missing or if the value is actually `null`. 
- **Race Condition**: In a simple `HashMap`, you would call `map.containsKey(key)` to check. However, in `ConcurrentHashMap`, the map could be changed by another thread *between* your `get()` and `containsKey()` calls. To prevent this "check-then-act" race condition, Doug Lea decided to disallow `null` entirely.

### 4. Fail-Fast vs. Weakly Consistent Iterators?
- **Fail-Fast (`HashMap`)**: Uses a `modCount`. If the map is structurally modified while iterating (excluding `Iterator.remove()`), it throws `ConcurrentModificationException`. This is a "best-effort" protection against concurrent bugs.
- **Weakly Consistent (`CHM`)**: Designed for high concurrency. It traverses elements as they existed at the start of iteration. It does not throw CME and *may* (but is not guaranteed to) reflect modifications that happen during iteration.

### 5. JDK 7 CHM Segmentation vs. JDK 8+ CAS Locking?
- **JDK 7 (Segmented Locks)**: Uses an array of `Segment`s (usually 16), each extending `ReentrantLock`. Since locks are at the segment level, only 16 threads can write concurrently if they land in different segments.
- **JDK 8+ (Bin Locking)**: Scraps segments for a single `Node[]`. It uses **CAS** to insert into empty bins and **synchronized** on the **head node** of a bin only when collisions occur. This moves the lock granularity from the segment level to the individual bin level, allowing for much higher write concurrency (scaling with table size).

---

## 7. Deep Dive: How Cooperative Resizing Works (Step-by-Step)
This is the "magic" that allows `ConcurrentHashMap` to resize without a global lock.

1.  **The Trigger**: A thread finds that `size > threshold` (0.75 * capacity). It allocates a `nextTable` (double size).
2.  **The Stride (Work Chunks)**: The transfer is split into chunks called "strides" (default min 16 bins).
    *   The index `transferIndex` keeps track of the boundary.
    *   Example: Transferring 64 bins.
        *   Thread A claims bins `[48-63]`.
        *   Thread B (trying to `put` implies it wants to write) sees a resize is active. It claims bins `[32-47]`.
3.  **The Transfer Loop**:
    *   The thread locks the **Bin Head** (synchronized).
    *   It splits the list/tree for that bin into `lowNode` (stays at index `i`) and `highNode` (moves to `i + n`).
    *   It writes these directly to the `nextTable`.
4.  **The ForwardingNode**:
    *   Once a bin is fully moved, the thread places a `ForwardingNode` (hash = -1) in the **Old Table** at that index.
    *   This node contains a pointer to the `nextTable`.
5.  **Handling Late Arrivals**:
    *   **Readers**: If `get()` hits a `ForwardingNode`, it redirects to `nextTable`.
    *   **Writers**: If `put()` hits a `ForwardingNode`, the writer realizes, "I can't write here, this bin is moving! I will go help transfer other bins instead of blocking."

### Visual: Cooperative Resizing
```text
Old Table (Size 16)
[Bin 0] ... [Bin 14] [ForwardingNode]--> Points to New Table (Size 32)
              ^
              |
Thread A is resolving collisions here.

Thread B comes to write to Bin 15.
Thread B sees ForwardingNode at Bin 15.
Thread B checks `transferIndex`.
Thread B claims Bin 0-15 to help transfer.
```

---

## 8. LinkedHashMap: The "Ordered" Cousin

**Question:** What are the special properties of LinkedHashMap and why does it get used in specific scenarios?
**Answer:**
`LinkedHashMap` extends `HashMap` but adds a **doubly-linked list** running through all its entries. This maintains iteration order.

### Special Properties:
1.  **Insertion Access Order (Default)**: Iterating gives keys in the order they were inserted.
2.  **Access Order (Optional)**: If constructed with `accessOrder = true`, iterating gives keys in the order they were **last accessed** (Get/Put).
    *   **Real World Use Case (LRU Cache):** This is the foundation of a "Least Recently Used" cache. By overriding `removeEldestEntry()`, you can automatically delete the oldest accessed item when the map grows too big.

```java
// LRU Cache Example (keeps last 100 accessed items)
Map<Integer, String> lruCache = new LinkedHashMap<>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
        return size() > 100;
    }
};
```

---

## 9. Custom Objects as Keys
**Question:** Can we make HashMap keys as custom objects? Give a real-time example.
**Answer:**
Yes, but you **MUST** override both `equals()` and `hashCode()` correctly. If you don't, individual instances with the same data will be treated as different keys because the default `Object.hashCode()` uses the memory address.

### Rules:
1.  **Immutable**: Keys should ideally be immutable (or at least their `hashCode` fields shouldn't change). If a key's hash changes after it's in the map, you can never find it again (State corruption).
2.  **Consistency**: `hashCode` must return the same value for the same object properties. `equals` must be consistent with `hashCode`.

### Enterprise Example: Composite Key
In a banking app, you might need to cache User Privileges based on `Region` + `Role`.

```java
public final class UserKey {
    private final String userId; // e.g., "U1001"
    private final String region; // e.g., "APAC"

    public UserKey(String userId, String region) {
        this.userId = userId;
        this.region = region;
    }

    // HashCode combines all significant fields
    @Override
    public int hashCode() {
        return Objects.hash(userId, region);
    }

    // Equals checks content, not memory address
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserKey)) return false;
        UserKey that = (UserKey) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(region, that.region);
    }
}

// Usage
Map<UserKey, List<String>> privilegeCache = new HashMap<>();
privilegeCache.put(new UserKey("U1001", "APAC"), Arrays.asList("VIEW_ACCOUNTS", "APPROVE_LOANS"));
```

---

## 10. Common Interview Deep Dives

### Q1: Why does treeification wait until capacity >= 64? (The "64 Rule")
If the `size` of the map is small (e.g., 32), and one bucket has 8 items (collision), Java prefers to **resize the array** (to 64) rather than **treeify** that bucket.
*   **Reason:** Resizing (doubling buckets) is usually faster and spreads the keys out. Treeification is computationally expensive and is a "last resort" for when the array is already large enough but collisions persist.

### Q2: Why is HashMap unsafe concurrently (specifically JDK 7 Infinite Loop)?
In JDK 7, `resize()` transfers entries by inserting them at the **head** of the new bucket list (reversing the order).
*   **Scenario:** Two threads try to resize at the same time. Thread A gets suspended while moving a node. Thread B finishes. Thread A wakes up and continues moving nodes that have already been moved/reversed by B.
*   **Result:** A circular reference (`A.next -> B`, `B.next -> A`) is created in the linked list. The next `get()` call enters an infinite loop, spiking CPU to 100%.
*   **JDK 8 Fix:** JDK 8 uses "tail insertion" (maintains order) during resize, eliminating the loop possibility, though data corruption (lost updates) is still possible.

### Q3: ConcurrentHashMap Null Policy
**Question:** Why does `ConcurrentHashMap` throw Exception on `put(key, null)` or `put(null, value)`?
**Answer:** Ambiguity in concurrent environments.
*   In `HashMap`, `get(key)` returning `null` could mean:
    1.  The key is missing.
    2.  The key exists but the value is `null`.
    *   (You verify this with `containsKey()`).
*   In `ConcurrentHashMap`, between the call to `get()` and `containsKey()`, another thread might have removed the key. The result of `containsKey` is useless by the time you get it. Therefore, `null` is banned so that a return value of `null` **always** means "Key not found".

### Q4: Fail-Fast vs. Weakly Consistent Iterators
*   **Fail-Fast (HashMap/ArrayList):** If the collection changes (add/remove) while you are iterating, it immediately throws `ConcurrentModificationException`. It uses a `modCount` variable to detect changes.
*   **Weakly Consistent (ConcurrentHashMap):** Iterators are designed to survive concurrent modification. They might reflect the state of the map when the iterator was created, or they *might* (but are not guaranteed to) reflect updates made after creation. They **never** throw `ConcurrentModificationException`.

### Q5: JDK 7 Segmented Locking vs JDK 8+ CAS
*   **JDK 7:** Used "Lock Stripping". The map was divided into 16 "Segments" (mini-HashMaps). A write to key in Segment 1 only locked Segment 1. Segment 2 was free. (Concurrency Level = 16).
*   **JDK 8+:** Removed Segments. Now locks are even more granular—at the **Bucket (Bin) Head**.
    *   If writing to a new empty bin: Use **CAS** (Compare-And-Swap) (No lock).
    *   If writing to an existing bin (collision): Lock only that single **Bin Node** (synchronized).
    *   Result: Millions of concurrent writers possible if hashes are well distributed.

---
