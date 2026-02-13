# Comprehensive Interview Questions & Answers

This document consolidates a wide range of interview questions covering Core Java (Java 8+), Spring Boot, Microservices, Databases, System Design, and Algorithms.

---

## 1. Core Java & Java 8 Features

### **Q: What are the main features of Java 8?**
**Answer:**
*   **Lambda Expressions:** Enable functional programming.
*   **Functional Interfaces:** Interfaces with a single abstract method.
*   **Stream API:** Parallel and sequential operations on collections.
*   **Optional Class:** To handle `NullPointerException` gracefully.
*   **Default & Static Methods in Interfaces:** Allows adding methods without breaking implementation.
*   **Date and Time API (`java.time`):** Immutable and thread-safe date/time handling.
*   **Nashorn JavaScript Engine:** Execute JS code from Java.

### **Q: What is a Functional Interface? Give examples.**
**Answer:**
A functional interface is an interface that contains exactly one abstract method. It can have any number of default or static methods.
**Examples:**
*   `Runnable` (`run()`)
*   `Callable` (`call()`)
*   `Comparator` (`compare()`)
*   `Predicate<T>` (`test()`)
*   `Consumer<T>` (`accept()`)
*   `Supplier<T>` (`get()`)
*   `Function<T, R>` (`apply()`)

### **Q: What are Lambda Functions?**
**Answer:**
Lambdas are anonymous functions (no name, no return type declaration, no access modifiers) used to implement Functional Interfaces concisely.
Example: `(a, b) -> a + b` instead of creating an anonymous class.

### **Q: Why is Comparable NOT a Functional Interface when Comparator IS?**
**Answer:**
*   **Comparator (`compare(T o1, T o2)`)** is a functional interface because it's usually passed as behavior (strategy pattern) to sort methods and meets the single abstract method rule.
*   **Comparable (`compareTo(T o)`)** describes the fundamental nature/class of an object (natural ordering). It is typically implemented by the class itself (e.g., `String`, `Integer`), not as a standalone behavior function. While technically it has one abstract method, conceptually it defines object state comparison rather than a functional action.

### **Q: What is the return type of `Predicate<T>`?**
**Answer:**
The return type of the abstract method `test(T t)` in `Predicate<T>` is `boolean`.

### **Q: From a list of integers, find and print the frequency of each element using streams.**
**Answer:**
```java
List<Integer> list = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
Map<Integer, Long> frequencyMap = list.stream()
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
System.out.println(frequencyMap);
```

### **Q: Switch case enhancements (Java 12+)?**
**Answer:**
Modern switch expressions allow:
*   Arrow syntax (`->`) preventing fall-through.
*   Returning values directly from switch.
*   Multiple labels in one case.
```java
String result = switch(day) {
    case MONDAY, FRIDAY -> "Work";
    case SATURDAY, SUNDAY -> "Rest";
    default -> "Normal";
};
```

---

## 2. Collections & Data Structures

### **Q: Difference between HashMap, SynchronizedMap, and ConcurrentHashMap?**
**Answer:**
1.  **HashMap:** Not thread-safe, fast, allows one null key.
2.  **SynchronizedMap (`Collections.synchronizedMap()`):** Thread-safe by locking the entire map object. Slower concurrency.
3.  **ConcurrentHashMap:** Thread-safe, highly performant. Uses **bucket-level locking** (segment locking in older versions, CAS + synchronized in Java 8+). It allows concurrent reads and safe updates without locking the whole map. Does not allow null keys or values.

### **Q: What is Fail-Fast Iterator?**
**Answer:**
Iterators that throw `ConcurrentModificationException` if the collection is modified structurely (add/remove) while iterating (except via the iterator's own `remove` method). Examples: `ArrayList`, `HashMap` iterators.

### **Q: StringBuilder vs StringBuffer?**
**Answer:**
*   **StringBuffer:** Synchronized (thread-safe), slower. Use in multi-threaded environments.
*   **StringBuilder:** Non-synchronized (not thread-safe), faster. Use in single-threaded strings manipulation (standard choice).

### **Q: How to implement a Doubly Linked List?**
**Answer:**
Should implement a `Node` class with `prev`, `next`, and `data`. The list class maintains `head` and `tail` pointers.
```java
class Node {
    int data;
    Node prev, next;
    public Node(int data) { this.data = data; }
}
```

---

## 3. OOPs & Design Patterns

### **Q: What are the 4 pillars of OOPs?**
**Answer:**
1.  **Encapsulation:** Hiding internal state/implementation details.
2.  **Inheritance:** Acquiring properties of a parent class.
3.  **Polymorphism:** Performing a single action in different ways (Overloading & Overriding).
4.  **Abstraction:** Hiding complex implementation details and showing only functionality.

### **Q: Abstract Class vs Interface?**
**Answer:**
*   **Interface:** Fully abstract (pre-Java 8), supports multiple inheritance, defines "what it can do". Variables are implicitly `public static final`.
*   **Abstract Class:** Can have state (instance variables) and constructors. Supports partial implementation. Defines "what it is".

### **Q: Singleton Design Pattern implementation?**
**Answer:**
Ensures a class has only one instance.
```java
public class Singleton {
    private static volatile Singleton instance;
    private Singleton() {} // Private constructor
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) { // Double-checked locking
                if (instance == null) instance = new Singleton();
            }
        }
        return instance;
    }
}
```

---

## 4. Multithreading & Concurrency

### **Q: Explain Executor Framework.**
**Answer:**
A framework in `java.util.concurrent` to decouple task submission from execution. It uses thread pools to manage worker threads efficiently.
*   **Interfaces:** `Executor`, `ExecutorService`.
*   **Implementations:** `ThreadPoolExecutor`.
*   **Factories:** `Executors.newFixedThreadPool()`, `newCachedThreadPool()`, etc.

### **Q: How to handle Deadlocks?**
**Answer:**
*   **Avoid Nested Locks:** Don't lock multiple resources if possible.
*   **Lock Ordering:** Always acquire locks in a consistent order.
*   **Lock Timeout:** Use `tryLock()` with timeout instead of indefinite waiting.

### **Q: When producers generate events faster than consumers can process, how to handle this?**
**Answer:**
This is a **Backpressure** problem.
1.  **Buffering:** Use a queue (like Kafka) to temporarily store events.
2.  **Scaling Consumers:** Add more consumer instances.
3.  **Throttling/Rate Limiting:** Slow down the producer (if possible).
4.  **Sampling/Dropping:** Drop non-critical events if the buffer is full.

---

## 5. Spring Boot & Microservices

### **Q: How to achieve high performance in REST APIs?**
**Answer:**
*   **Caching:** Use Redis/Memcached for frequent data.
*   **Asynchronous Processing:** Use `CompletableFuture` or `@Async`.
*   **Database Optimization:** Indexing, connection pooling (HikariCP).
*   **Pagination:** Never return full datasets.
*   **Compression:** Enable GZIP.
*   **CDN:** Serve static assets via CDN.

### **Q: HTTP Status Codes: Created vs No Content?**
**Answer:**
*   **201 Created:** When a resource is successfully created (e.g., after POST).
*   **204 No Content:** Request processed successfully, but no content to return (e.g., successful DELETE or PUT without response body).

### **Q: Scenario: You receive 50,000 products in a POST API request body. Will you process synchronously?**
**Answer:**
**No.** Processing 50k records synchronously will timeout the HTTP request.
**Solution:**
1.  Accept the request, validate basic format.
2.  Push the payload (or reference to file) to a **Queue** (Kafka/RabbitMQ).
3.  Return **202 Accepted** immediately with a `processId`.
4.  Process asynchronously via a consumer.
5.  Client polls status or receives a Webhook callback upon completion.

### **Q: What if some records fail in batch processing?**
**Answer:**
*   **Batch Update:** Do not fail the whole batch. Track success/failures individually.
*   **Dead Letter Queue (DLQ):** Send failed records to a DLQ for manual inspection or retry.
*   **Response:** Provide a report: "49,900 success, 100 failed (with reason)".

### **Q: Scenario: API latency increased from 2ms to 80s. How to debug?**
**Answer:**
1.  **Check Metrics/Dashboards (Grafana/Prometheus):** Is it CPU, Memory, or DB load?
2.  **Distributed Tracing (Zipkin/Jaeger):** Identify which span (service or DB call) is taking time.
3.  **DB Analysis:** Check for locked tables, missing indexes, or slow queries using slow query logs.
4.  **Thread Dumps:** Check for thread starvation or deadlocks.
5.  **External Dependencies:** Is a 3rd party API down or slow?

### **Q: Spring Security in Microservices?**
**Answer:**
*   Usually implemented via **OAuth2 / OIDC** (e.g., Keycloak, Auth0).
*   **Gateway Level:** API Gateway validates JWT tokens.
*   **Service Level:** Services check roles/scopes extracted from the JWT.

---

## 6. Database & SQL

### **Q: DB Optimization Techniques?**
**Answer:**
*   **Indexing:** Create indexes on columns used in `WHERE`, `JOIN`, `ORDER BY`.
*   **Normalization vs De-normalization:** De-normalize for read-heavy operations if needed.
*   **Query Optimization:** Avoid `SELECT *`, use proper joins.
*   **Read Replicas:** Separate Read and Write operations.
*   **Caching:** Level 2 Cache (Hibernate) or external Cache (Redis).

### **Q: Connect two tables in optimize way?**
**Answer:**
Use **INNER JOIN** over cross joins (Cartesian products). Ensure the joining columns are **Indexed**.

### **Q: Design DB with O(1) Insertion/Deletion?**
**Answer:**
This suggests a Key-Value store or Hash-based structure (NoSQL) rather than a B-Tree Relational DB. Alternatively, a Heap file organization where records are appended to the end (Insert O(1)), but deletion is complex without marking/tombstones.

---

## 7. Algorithms & Coding

### **Q: Array: Push 0s to back, keep positives in front (same order).**
**Answer:**
Two-pointer approach.
```java
public void moveZeroes(int[] nums) {
    int insertPos = 0;
    for (int num : nums) {
        if (num != 0) nums[insertPos++] = num;
    }
    while (insertPos < nums.length) {
        nums[insertPos++] = 0;
    }
}
```

### **Q: Count identical string ("aaa" or "bbb") replacement.**
**Answer:**
Iterate through string. If `s[i] == s[i-1] == s[i-2]`, increment replacement count and conceptually change `s[i]` to break the sequence.

### **Q: Reverse first N elements of Queue.**
**Answer:**
1.  Dequeue first N elements and push to Stack.
2.  Pop from Stack and Enqueue back to Queue.
3.  Dequeue `(Size - N)` remaining elements and Enqueue them back.

### **Q: Max Profit Stock (Buy Once).**
**Answer:**
```java
int minPrice = Integer.MAX_VALUE;
int maxProfit = 0;
for (int price : prices) {
    if (price < minPrice) minPrice = price;
    else if (price - minPrice > maxProfit) maxProfit = price - minPrice;
}
return maxProfit;
```

---

## 8. Testing & Process

### **Q: JUnit and Mockito usage?**
**Answer:**
*   **JUnit:** For writing test cases (`@Test`, `@BeforeEach`).
*   **Mockito:** To mock external dependencies (Service, Repository) using `@Mock` and `@InjectMocks`. Use `when(...).thenReturn(...)` to simulate behavior without hitting real DB.

### **Q: Production Break: How to identify, Analyze, Fix?**
**Answer:**
1.  **Identify:** Alerts (PagerDuty), User reports, Monitoring Dashboards.
2.  **Contain:** Rollback to previous version if caused by deployment, or enable Feature Flags to disable faulty component.
3.  **Analyze:** Logs (Splunk/ELK), Tracing.
4.  **Fix:** Develop hotfix -> Test -> Deploy.
5.  **RCA:** Root Cause Analysis document to prevent recurrence.

### **Q: AWS Services Used?**
**Answer:**
*(Common answers)*
*   **Compute:** EC2, Lambda, EKS (Kubernetes).
*   **Storage:** S3.
*   **Database:** RDS, DynamoDB.
*   **Formatting:** SQS, SNS.
*   **Monitoring:** CloudWatch.
