# Qualys Interview Guide - Part 1: Core Coding & DSA

This document covers frequently asked Data Structures and Algorithms questions for Qualys Senior Software Engineer interviews.

---

## 1. Common Coding Questions

### A. Reverse an Array / String
**Problem:** Reverse a given array or string in-place.
**Approach:** Two-pointer technique.
```java
public void reverse(int[] arr) {
    int left = 0, right = arr.length - 1;
    while (left < right) {
        int temp = arr[left];
        arr[left] = arr[right];
        arr[right] = temp;
        left++;
        right--;
    }
}
```

### B. Find Duplicates in an Array
**Problem:** Find duplicate elements in an array.
**Approaches:**
1.  **HashSet:** Add elements to Set. If `add()` returns false, it's a duplicate. O(N) time, O(N) space.
2.  **Sorting:** Sort and check adjacent elements. O(N log N).
3.  **Floyd's Cycle Detection:** If values are in range [1, N], treat array as linked list.

**Code (HashSet Approach):**
```java
public Set<Integer> findDuplicates(int[] nums) {
    Set<Integer> seen = new HashSet<>();
    Set<Integer> duplicates = new HashSet<>();
    for (int num : nums) {
        if (!seen.add(num)) {
            duplicates.add(num);
        }
    }
    return duplicates;
}
```

### C. First & Last Occurrence in Sorted Array
**Problem:** Given sorted array `[1, 2, 2, 2, 3]`, find first and last index of `2`.
**Approach:** Binary Search (run twice).
1.  **First Occurrence:** If `mid` matches target, keep searching left (`high = mid - 1`) to see if there's an earlier one.
2.  **Last Occurrence:** If `mid` matches target, keep searching right (`low = mid + 1`).
**Time Complexity:** O(log N).

**Code:**
```java
public int[] searchRange(int[] nums, int target) {
    int first = findBound(nums, target, true);
    if (first == -1) return new int[]{-1, -1};
    int last = findBound(nums, target, false);
    return new int[]{first, last};
}

private int findBound(int[] nums, int target, boolean isFirst) {
    int low = 0, high = nums.length - 1, ans = -1;
    while (low <= high) {
        int mid = low + (high - low) / 2;
        if (nums[mid] == target) {
            ans = mid;
            if (isFirst) high = mid - 1; // Look left
            else low = mid + 1;         // Look right
        } else if (nums[mid] < target) {
            low = mid + 1;
        } else {
            high = mid - 1;
        }
    }
    return ans;
}
```

### D. Search in Rotated Sorted Array
**Problem:** Search for a target in `[4, 5, 6, 7, 0, 1, 2]`.
**Approach:** Modified Binary Search.
1.  Find `mid`.
2.  Check if left half `[low...mid]` is sorted.
    *   If sorted and target is in range, search left. Else search right.
3.  Else (right half is sorted), check if target is in range.
**Time Complexity:** O(log N).

**Code:**
```java
public int search(int[] nums, int target) {
    int low = 0, high = nums.length - 1;
    while (low <= high) {
        int mid = low + (high - low) / 2;
        if (nums[mid] == target) return mid;

        if (nums[low] <= nums[mid]) { // Left half is sorted
            if (nums[low] <= target && target < nums[mid])
                high = mid - 1;
            else
                low = mid + 1;
        } else { // Right half is sorted
            if (nums[mid] < target && target <= nums[high])
                low = mid + 1;
            else
                high = mid - 1;
        }
    }
    return -1;
}
```

### E. Matrix Diagonal / Transpose
**Transpose:** Swap `matrix[i][j]` with `matrix[j][i]`.
**Diagonal Sum:** Sum elements where `i == j` (Primary) and `i + j == n - 1` (Secondary).

**Code (Transpose):**
```java
public void transpose(int[][] matrix) {
    int n = matrix.length;
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) { // j starts from i+1 to simply swap upper triangle with lower
            int temp = matrix[i][j];
            matrix[i][j] = matrix[j][i];
            matrix[j][i] = temp;
        }
    }
}
```

### F. Palindrome Check
**String:** Two pointers (start and end), compare characters.
**Number:** Reverse the integer mathematically (`rev = rev * 10 + digit`) and compare.

**Code (String & Number):**
```java
// String Palindrome
public boolean isPalindrome(String s) {
    int i = 0, j = s.length() - 1;
    while (i < j) {
        if (s.charAt(i) != s.charAt(j)) return false;
        i++; j--;
    }
    return true;
}

// Number Palindrome
public boolean isPalindrome(int x) {
    if (x < 0) return false;
    int original = x, rev = 0;
    while (x != 0) {
        rev = rev * 10 + x % 10;
        x /= 10;
    }
    return original == rev;
}
```

### G. Two Threads Printing Even/Odd
**Problem:** Print 1, 2, 3... using two threads.
**Code:**
```java
class Printer {
    private int count = 1;
    private final int LIMIT;
    public Printer(int limit) { this.LIMIT = limit; }

    public synchronized void printOdd() {
        while (count <= LIMIT) {
            while (count % 2 == 0) wait();
            System.out.println("Odd: " + count++);
            notify();
        }
    }

    public synchronized void printEven() {
        while (count <= LIMIT) {
            while (count % 2 != 0) wait();
            System.out.println("Even: " + count++);
            notify();
        }
    }
}
```

---

## 2. DSA Concept Questions

### A. HashMap vs TreeMap
| Feature | HashMap | TreeMap |
| :--- | :--- | :--- |
| **Ordering** | No order (random based on hash). | Sorted order (Natural/Comparator). |
| **Implementation** | Hash Table (Bucket Array + Linked List/Red-Black Tree). | Red-Black Tree (Self-balancing BST). |
| **Time Complexity** | O(1) average for get/put. | O(log N) for get/put. |
| **Null Keys** | Allows 1 null key. | No null keys allowed (throws NPE). |

### B. Stack vs Queue
*   **Stack (LIFO - Last In First Out):**
    *   **Use Cases:** Recursion handling, Undo/Redo features, Syntax parsing (brackets).
*   **Queue (FIFO - First In First Out):**
    *   **Use Cases:** Job scheduling, Breadth-First Search (BFS), Message Buffers (Kafka/RabbitMQ).

### C. LinkedList vs ArrayList
| Feature | ArrayList | LinkedList |
| :--- | :--- | :--- |
| **Underlying Data** | Dynamic Array. | Doubly Linked List. |
| **Access (Get)** | O(1) - Random Access. | O(N) - Sequential Access. |
| **Insertion/Deletion** | Slow O(N) (shifting needed). | Fast O(1) (pointer change). |
| **Memory** | Less memory. | More memory (stores pointers). |

### D. How HashMap Works Internally
1.  **Hashing:** `hashCode()` is called on Key to calculate the bucket index (`hash % n`).
2.  **Collision:** If two keys map to the same bucket, they form a **Linked List**.
3.  **Java 8 Improvement:** If the list grows beyond 8 nodes (TREEIFY_THRESHOLD), it converts to a **Red-Black Tree** to improve search from O(N) to O(log N).
4.  **Equals:** `equals()` is used to find the specific key within the bucket.

### E. Design LRU Cache (Least Recently Used)
**Data Structure:** **HashMap + Doubly Linked List**.
*   **HashMap:** Stores `Key -> Node` for O(1) access.
*   **Doubly Linked List:** Maintains order. MRU (Most Recently Used) at head, LRU at tail.
*   **Logic:**
    *   **Get:** Node found? Move it to Head. Return value.
    *   **Put:**
        *   Exists? Update value, Move to Head.
        *   New? Create Node, Add to Head, Put in Map.
        *   Capacity Full? Remove Tail (LRU) from List and Map.

**Code (Using LinkedHashMap - Interview Shortcut):**
```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true); // true = access order
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

**Code (Custom Implementation):**
```java
class LRUCache {
    class Node {
        int key, value;
        Node prev, next;
        Node(int key, int value) { this.key = key; this.value = value; }
    }
    
    private Map<Integer, Node> map = new HashMap<>();
    private Node head, tail;
    private int capacity;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        head = new Node(0, 0); tail = new Node(0, 0); // Dummy nodes
        head.next = tail; tail.prev = head;
    }

    public int get(int key) {
        if (!map.containsKey(key)) return -1;
        Node node = map.get(key);
        remove(node);
        insert(node);
        return node.value;
    }

    public void put(int key, int value) {
        if (map.containsKey(key)) {
            remove(map.get(key));
        }
        if (map.size() == capacity) {
            remove(tail.prev);
        }
        insert(new Node(key, value));
    }

    private void remove(Node node) {
        map.remove(node.key);
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void insert(Node node) {
        map.put(node.key, node);
        Node headNext = head.next;
        head.next = node;
        node.prev = head;
        node.next = headNext;
        headNext.prev = node;
    }
}
```
