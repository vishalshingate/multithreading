# Tricky Questions & Answers

This document contains answers to tricky and scenario-based interview questions covering Java, Spring Boot, React, and Architecture.

---

## **Java Core & Memory Management**

### 1) What is the size of heap memory? Can it be changed?
**Answer:**
-   **Default Size:** The default initial size is often 1/64th of physical memory, and the maximum is 1/4th of physical memory (this varies by JVM version and OS).
-   **Can it be changed?:** Yes. You can configure it using JVM flags:
    -   `-Xms<size>`: Initial heap size (e.g., `-Xms512m`).
    -   `-Xmx<size>`: Maximum heap size (e.g., `-Xmx4g`).

### 2) What is a memory leak in Java - OutOfMemoryError?
**Answer:**
-   **Memory Leak:** Occurs when objects are no longer being used by the application but are still referenced by something (like a static collection, listener, or thread), preventing the Garbage Collector (GC) from reclaiming their memory.
-   **OutOfMemoryError (OOM):** The result of a memory leak (or simply needing more memory than allocated). It happens when the Heap is full, and GC cannot free up enough space for new objects.
    -   *Analogy*: A leak is a dripping tap filling a bucket; OOM is the bucket overflowing.

### 3) Why Large Static Variables Can Cause Memory Leaks in Java and how to avoid it?
**Answer:**
-   **Why:** Static variables are associated with the **class** (stored in the Metaspace/Method Area) rather than an instance. They act as **GC Roots**. As long as the ClassLoader that loaded the class is alive (usually the entire app lifecycle), the static variable and everything it references will **never** be garbage collected.
-   **Avoidance:**
    1.  Avoid static collections (`List`, `Map`) unless necessary.
    2.  If used, clean them up explicitly when no longer needed.
    3.  Use `WeakReference` or `SoftReference` for caches so the GC can reclaim them if memory is low.

### 4) When does JVM run the Garbage Collector process?
**Answer:**
The JVM runs GC non-deterministically, but typically triggers when:
1.  **Eden Space is Full**: New objects are allocated in Eden. If it fills up, a Minor GC is triggered.
2.  **Old Gen is Full**: If the Old Generation fills up (or meets a threshold), a Major/Full GC runs.
3.  **System.gc()**: Called programmatically (suggests, but doesn't guarantee execution).
4.  **CPU Idle**: Some GCs run background phases when CPU load is low.

### 5) Can you use/catch any classes inside a catch block in Java?
**Answer:**
No. You can only catch classes that extend `java.lang.Throwable` (i.e., `Exception` or `Error`). Trying to catch a non-Throwable class results in a compile-time error.

### 6) Ways to break Singleton Pattern in Java
**Answer:**
A standard Singleton can be broken via:
1.  **Reflection**: Access private constructor `setAccessible(true)` and create a new instance. (Fix: Throw exception in constructor if instance exists).
2.  **Serialization/Deserialization**: Deserializing creates a new instance. (Fix: Implement `readResolve()` returning the singleton instance).
3.  **Cloning**: `clone()` creates a copy. (Fix: Override `clone()` and throw `CloneNotSupportedException`).
4.  **Multiple ClassLoaders**: Different ClassLoaders can load the same class twice.

### 7) When using wait, notify - which thread will get notify?
**Answer:**
-   `notify()`: The JVM selects **one** thread arbitrarily from the wait set on that object. There is no guarantee which one (it is not necessarily FIFO).
-   `notifyAll()`: Wakes up **all** waiting threads. They then compete for the lock.

### 8) Does Java Streams consume memory?
**Answer:**
Streams themselves are predominantly **stateless**.
-   They don't store data; they operate on the source.
-   However, **Intermediate Operations** like `sorted()` or `distinct()` are **stateful** and may require buffering all elements into memory to perform the sort/deduplication, potentially consuming significant memory.
-   Streams use lazy evaluation, so memory is not consumed until the terminal operation runs.

### 9) Using Optional write a code to get the correct length of String, If String is null print as 0
**Answer:**
```java
String input = null; // or "hello"
int length = Optional.ofNullable(input)
                     .map(String::length)
                     .orElse(0);
System.out.println(length);
```

---

## **Spring Boot & Architecture**

### 10) Can we have more than one Spring container in our Spring Boot application?
**Answer:**
Yes.
-   **Hierarchy**: Spring supports a parent-child context hierarchy.
-   **Example**: In a traditional Spring MVC app, you might have a Root Context (Service/DAO) and a Servlet Context (Controllers).
-   **Boot**: While Spring Boot typically flattens this into one context, you can manually create multiple contexts (e.g., separating modules or micro-frontends loaded in one backend).

### 11) Why is constructor injection recommended over setter or field injection?
**Answer:**
1.  **Immutability**: Dependencies can be declared `final`.
2.  **No Partial State**: Ensures the bean is fully initialized with required dependencies before use (prevents NullPointerExceptions).
3.  **Testing**: Easy to use in unit tests without reflection (just pass mocks into the constructor).
4.  **Circular Dependency Detection**: Fails fast at startup if a cycle exists (field injection might hide it until runtime).

### 12) Will we get error if we have below 2 handler methods for below request: `/customer/abc`
**Mappings:**
1. `/customer/abc`
2. `/customer/{abc}`

**Answer:**
No error.
Spring MVC uses the **"Most Specific Match"** rule.
-   A literal match (`/customer/abc`) takes precedence over a variable/pattern match (`/customer/{abc}`).
-   Request `/customer/abc` will hit handler 1.
-   Request `/customer/xyz` will hit handler 2.

### 13) Cyclic dependency in Spring, will we get error?
```java
@Component Class A { @Autowired B b; }
@Component Class B { @Autowired A a; }
```
**Answer:**
-   **Field/Setter Injection**: Spring **can** usually resolve this using 3-level caching. It creates an early reference (bean not fully initialized) and injects it. **No Error** (mostly).
-   **Constructor Injection**: **Yes, Error**. `BeanCurrentlyInCreationException`. Spring cannot instantiate A without B, and B without A. To fix, use `@Lazy` on one constructor parameter.

### 14) Where to add `@Transactional` annotation in Spring Boot? Service or Repository?
**Answer:**
**Service Layer.**
-   **Atomicity**: Services typically orchestrate multiple Repository calls (business unit). The entire unit must succeed or fail together.
-   **Session Scope**: Keeping the transaction open in the Service allows `LazyLoading` of entities. If `Transactional` is on the Repository, the session closes immediately after the DB call, causing `LazyInitializationException` in the Service layer.

---

## **Scenario & React Integration**

### 15) How would you handle huge file uploads in React + Spring Boot? (Chunked Upload)
**Concept:**
Don't send the whole file. Slice it in the browser, send chunks, and merge on the server.

**React (Frontend):**
1.  Use `File.slice(start, end)` to break the file.
2.  Loop and POST chunks: `/upload/chunk?id=123&index=0`.

**Spring Boot (Backend):**
1.  Receive chunk bits (`MultipartFile`, `chunkIndex`, `uploadId`).
2.  Append bytes to a temp file on disk: `RandomAccessFile.seek(offset)`.
3.  When `chunkIndex == totalChunks`, finalize the file.

### 16) Data flow is unidirectional in React. How to pass data from child to parent?
**Answer:**
**Callback Functions.**
1.  **Parent** defines a function: `const handleData = (data) => { setState(data) }`.
2.  **Parent** passes this function as a prop to Child: `<Child onUpdate={handleData} />`.
3.  **Child** calls the prop: `props.onUpdate("New Value")`.

### 17) How to handle multiple clients (10) accessing overlapping data/logic?
**Answer:**
This is a concurrency and architecture problem.
1.  **Caching (Redis)**: Store common/overlapped data in a distributed cache. If Client A computes it, Client B reads from cache.
2.  **Locking**: If modification is required, use Optimistic Locking (`@Version`) for DB updates, or Distributed Locks (Redis/Zookeeper) for critical sections.
3.  **Pub/Sub**: If clients need real-time updates on shared changes, use WebSockets (STOMP) or Kafka to broadcast changes to interested clients.

### 18) How to validate user if logged out but token is still "valid" (JWT)?
**Problem:** JWTs are stateless and self-contained. You can't "delete" them server-side until they expire.
**Solutions:**
1.  **Token Blacklist (Denylist)**: When user logs out, save the JWT (or its JTI - ID) in Redis with a TTL (time-to-live) equal to the remaining life of the token.
    -   *Filter Logic*: On every request, check redis: `if (redis.exists(token)) throw Unauthorized`.
2.  **Short Expiry + Refresh Tokens**: Make Access Token live only 5-15 mins.
    -   On logout, revoke the Refresh Token in the DB.
    -   The user still has access for max 15 mins (Acceptable trade-off), but cannot renew access once the short token expires.

