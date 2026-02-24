# Zemoso Technologies - Senior Backend Engineer Interview Questions

This guide covers common interview questions found in Zemoso Technologies interviews for Senior Backend roles, focusing on DSA, Core Java, Spring Boot, SQL, and System Design.

## 1. Core Coding & Data Structures (DSA)

### 1.1 DFS / BFS Traversal
**Question:** Write code for Depth First Search (DFS) or Breadth First Search (BFS) traversal of a tree or graph.

**Answer (BFS - Level Order for Tree):**
```java
public void bfs(TreeNode root) {
    if (root == null) return;
    Queue<TreeNode> queue = new LinkedList<>();
    queue.add(root);
    while (!queue.isEmpty()) {
        TreeNode node = queue.poll();
        System.out.print(node.val + " ");
        if (node.left != null) queue.add(node.left);
        if (node.right != null) queue.add(node.right);
    }
}
```

**Answer (DFS - Recursive for Graph):**
```java
public void dfs(int node, boolean[] visited, List<List<Integer>> adj) {
    visited[node] = true;
    System.out.print(node + " ");
    for (int neighbor : adj.get(node)) {
        if (!visited[neighbor]) {
            dfs(neighbor, visited, adj);
        }
    }
}
```

### 1.2 Longest Substring Without Repeating Characters
**Question:** Find the length of the longest substring without repeating characters.

**Logic:** Uses the **Sliding Window** technique with a `HashSet` or `HashMap`.
**Code:**
```java
public int lengthOfLongestSubstring(String s) {
    Set<Character> set = new HashSet<>();
    int left = 0, right = 0, maxLength = 0;
    while (right < s.length()) {
        if (!set.contains(s.charAt(right))) {
            set.add(s.charAt(right++));
            maxLength = Math.max(maxLength, set.size());
        } else {
            set.remove(s.charAt(left++));
        }
    }
    return maxLength;
}
```

### 1.3 Median in a Stream
**Question:** Find the median from a data stream.

**Logic:** Use two heaps: A **Max-Heap** for the lower half numbers and a **Min-Heap** for the upper half numbers.
**Code:**
```java
PriorityQueue<Integer> lowerHalf = new PriorityQueue<>(Collections.reverseOrder()); // Max Heap
PriorityQueue<Integer> upperHalf = new PriorityQueue<>(); // Min Heap

public void addNum(int num) {
    lowerHalf.add(num);
    upperHalf.add(lowerHalf.poll());
    if (lowerHalf.size() < upperHalf.size()) {
        lowerHalf.add(upperHalf.poll());
    }
}

public double findMedian() {
    return lowerHalf.size() > upperHalf.size() ? lowerHalf.peek() : (lowerHalf.peek() + upperHalf.peek()) / 2.0;
}
```

### 1.4 Other Common Problems
- **Valid Parentheses:** Use a Stack. Push opening brackets, pop matching closing brackets. If empty at end, it's valid.
- **Three Sum:** Sort array. Iterate `i` from `0` to `n`. Use two pointers (`left`, `right`) to find pairs sum to `-nums[i]`.

---

## 2. Backend & Java Specifics

### 2.1 Core Java & OOP
- **Interface vs Abstract Class:**
  - **Interface:** 100% abstraction (before Java 8 defaults), multiple inheritance allowed, variables are `public static final`.
  - **Abstract Class:** Partial abstraction, single inheritance, can have state (instance variables) and constructors.
- **Polymorphism:**
  - **Compile-time:** Method Overloading.
  - **Runtime:** Method Overriding (Dynamic dispatch).

### 2.2 Collections Internals
- **HashMap:** Uses an array of Buckets (Node<K,V>). Key -> Hash -> Index. Handles collisions via Linked List (or Red-Black Tree if size > 8 since Java 8).
- **HashSet:** Internally uses a HashMap with a dummy Object as value.

### 2.3 Multithreading & Concurrency
- **CyclicBarrier:** Allows a set of threads to all wait for each other to reach a common barrier point. Reusable.
- **CountDownLatch:** Allows one or more threads to wait until a set of operations being performed in other threads completes. Not reusable.
- **Synchronization:** `synchronized` keyword (intrinsic lock) or `ReentrantLock`.

### 2.4 JVM Memory Model
- **Heap:** Objects storage. Divided into Young Gen (Eden, S0, S1) and Old Gen.
- **Stack:** Method frames, local primitives, reference variables.
- **Metaspace (Java 8+):** Class metadata, static variables (moved from PermGen).

---

## 3. Spring Boot Framework

### 3.1 IOC & DI
- **IOC (Inversion of Control):** Framework manages object lifecycle instead of the developer.
- **DI (Dependency Injection):** Pattern to implement IOC. Types: Constructor (Best), Setter, Field.

### 3.2 Singleton Pattern in Spring
Spring beans are **Singleton** by default (per container, not per JVM).
**Question:** `@Qualifier` vs `@Primary`?
- **@Primary:** Use when multiple beans of same type exist, and you want a default one.
- **@Qualifier:** Use to specifically request a bean by name at the injection point.

### 3.3 Hibernate
- **Mapping:** `@OneToMany`, `@ManyToOne`, `@ManyToMany`.
- **Configuration:** `application.properties` (dialect, ddl-auto, etc.).

---

## 4. SQL & Databases

### 4.1 3rd Highest Salary (No LIMIT)
**Question:** Find the 3rd highest salary without using `LIMIT` or `TOP`.

**Answer (Generic SQL):**
```sql
SELECT Salary 
FROM Employee e1 
WHERE 2 = (
    SELECT COUNT(DISTINCT Salary) 
    FROM Employee e2 
    WHERE e2.Salary > e1.Salary
);
```

### 4.2 SQL to Code Logic
**Question:** Write logic in SQL to translate from code logic (e.g., conditional updates).
**Answer:** Use `CASE WHEN`.
```sql
UPDATE Employee
SET Salary = CASE
    WHEN Department = 'IT' THEN Salary * 1.10
    WHEN Department = 'HR' THEN Salary * 1.05
    ELSE Salary
END;
```

---

## 5. Design & Architecture (Senior Level)

### 5.1 System Design (HLD/LLD)
**Scenario:** Food Delivery App (like Swiggy/Zomato).
- **Core Entities:** Users, Restaurants, Menu, Orders, DeliveryPartners.
- **ER Diagram Relationships:**
  - User 1:N Orders
  - Restaurant 1:N MenuItems
  - Order N:1 Restaurant
  - Order N:1 User
  - Order 1:1 Delivery (or 1:N for tracking history)

### 5.2 Microservices
- **Components:**
  - **API Gateway:** Entry point, routing, auth.
  - **Service Registry (Eureka):** Service discovery.
  - **Config Server:** Centralized config.
  - **Circuit Breaker (Resilience4j):** Fault tolerance.
- **Communication:**
  - **Sync:** REST (RestTemplate/WebClient), Feign used rarely now.
  - **Async:** Message Brokers (Kafka/RabbitMQ).

### 5.3 REST API Design
- **Versioning:** URL (`/v1/users`), Header (`X-API-VERSION`).
- **Idempotency:** GET, PUT, DELETE should be idempotent. POST is not.
- **Status Codes:** 200 (OK), 201 (Created), 400 (Bad Request), 401 (Unauth), 403 (Forbidden), 500 (Server Error).

