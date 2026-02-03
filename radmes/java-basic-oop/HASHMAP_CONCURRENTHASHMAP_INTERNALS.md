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

## Common Interview Pointers
- HashMap resize and treeification thresholds; why treeification waits until capacity >= 64.
- Why HashMap is unsafe concurrently (possible lost updates, infinite loop bug in 7 during resize).
- ConcurrentHashMap null policy rationale.
- Differences between fail-fast and weakly consistent iterators.
- JDK 7 CHM segmentation vs JDK 8+ CAS + bin locking.
- How resizing works in CHM (forwarding nodes, cooperative transfer).
