# What is ThreadLocal?

`ThreadLocal` is a special Java class that allows you to create variables that can only be read and written by the **same thread**. 

If two threads access the same code reference to a `ThreadLocal` variable, each thread will see **its own independent copy**.

### Analogy: Hotel Rooms
*   **Static Variable:** The Hotel Lobby. Everyone shares it. If one person moves the sofa, everyone sees it moved.
*   **ThreadLocal:** A Hotel Room. Even though every room has a "Bathroom", my bathroom is private to me. If I put my toothbrush in it, you don't see it in your room.

---

## Real World Industry Uses

### 1. User Context / Authentication (e.g., Spring Security)
In a web application, thousands of users hit the same server. How does the server know "Method X is running for User A" without passing `(User user)` as a parameter to every single function?

**Solution:**
Frameworks like Spring Security store the authenticated user details in a `ThreadLocal`.
*   **Request Start:** Filter extracts Token -> saves `User` to `ThreadLocal`.
*   **Service Layer:** `SecurityContextHolder.getContext().getAuthentication()` reads from `ThreadLocal`.
*   **Request End:** Filter clears `ThreadLocal`.

### 2. Transaction Management (e.g., Hibernate / Spring @Transactional)
When you annotate a method with `@Transactional`, multiple database calls inside that method must use the **exact same DB Connection** to be part of the same transaction.

**Solution:**
Spring opens a connection and stores it in a `ThreadLocal`.
1.  `repo.save(user)` -> Checks `ThreadLocal` -> Finds connection -> Uses it.
2.  `repo.save(order)` -> Checks `ThreadLocal` -> Finds SAME connection -> Uses it.
3.  Commit.

### 3. Thread-Unsafe Legacy Classes (e.g., SimpleDateFormat)
Before Java 8, `SimpleDateFormat` was not thread-safe. If two threads used the same static formatter, it would crash or produce wrong dates.

**Solution:**
Instead of creating a new object every time (expensive), developers used `ThreadLocal` to give each thread its own private formatter. (Note: In Java 8+, use `DateTimeFormatter` which is thread-safe).

---

## 🛑 The "Memory Leak" Danger

In modern web servers (Tomcat/Jetty), threads are **re-used** (Thread Pools). 
*   If Thread-1 processes Request A (User: Alice) and saves "Alice" to `ThreadLocal`.
*   Request A finishes, but you **forget** to call `remove()`.
*   Thread-1 is returned to the pool.
*   Later, Thread-1 picks up Request B (User: Bob).
*   If Request B logic tries to "get generic user", it might accidentally find "Alice" still sitting in the `ThreadLocal`!

**Best Practice:**
Always use `try-finally` and call `remove()` in the `finally` block or use a Servlet Filter to ensure cleanup.

