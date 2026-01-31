# Multithreading in Spring Boot

Spring Boot provides several ways to handle multithreading, abstracting away the complexity of manual thread management while allowing for high-performance, asynchronous execution.

---

## 1. Asynchronous Execution with `@Async`

The simplest way to run code in a background thread is by using the `@Async` annotation.

### How to Enable
1.  Add `@EnableAsync` to a configuration class.
2.  Annotate the method with `@Async`.

### Example: Email Notification Service
In an e-commerce platform, when an order is placed, you don't want the user to wait for the email to be sent before getting a confirmation.

```java
@Service
public class EmailService {

    @Async
    public void sendOrderConfirmation(String email) {
        // Simulate time-consuming email sending logic
        try { Thread.sleep(5000); } catch (InterruptedException e) {}
        System.out.println("Email sent to " + email + " on thread: " + Thread.currentThread().getName());
    }
}
```

---

## 2. Industry Example: Parallel API Aggregation

A common senior-level task is calling multiple microservices in parallel to aggregate data for a single response (e.g., a "User Dashboard" that calls Profile, Orders, and Notifications services).

### Using `CompletableFuture`
`CompletableFuture` (introduced in Java 8) is the standard for non-blocking asynchronous programming.

```java
@Service
public class DashboardService {

    private final ProfileClient profileClient;
    private final OrderClient orderClient;

    public UserDashboard getDashboardData(String userId) {
        // Start two tasks in parallel
        CompletableFuture<Profile> profileFuture = CompletableFuture.supplyAsync(() -> profileClient.getProfile(userId));
        CompletableFuture<List<Order>> ordersFuture = CompletableFuture.supplyAsync(() -> orderClient.getOrders(userId));

        // Wait for both to complete and combine results
        return CompletableFuture.allOf(profileFuture, ordersFuture)
            .thenApply(v -> new UserDashboard(profileFuture.join(), ordersFuture.join()))
            .join(); // Block at the end to return the combined result
    }
}
```

---

## 3. The `TaskExecutor` Interface

`TaskExecutor` is the foundational Spring interface for executing tasks asynchronously. It is a simplified version of `java.util.concurrent.Executor`.

### Common Implementations:
1.  **`ThreadPoolTaskExecutor`**: The most commonly used. Highly configurable (core pool size, max pool size, queue capacity).
2.  **`SimpleAsyncTaskExecutor`**: Does not reuse threads! It starts a new thread for every invocation. Use only for simple experiments.
3.  **`VirtualThreadTaskExecutor`**: (Spring Boot 3.2+) Leverages Java 21 Virtual Threads.

### Custom Configuration Example
```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // Threads kept alive
        executor.setMaxPoolSize(10);       // Max threads if queue is full
        executor.setQueueCapacity(100);    // Queue for pending tasks
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }
}
```

---

## 4. Virtual Threads (The Future)

If you are using **Java 21** and **Spring Boot 3.2+**, you can handle thousands of concurrent requests using lightweight Virtual Threads without the overhead of heavy OS threads.

### How to Enable
Just add this to `application.properties`:
```properties
spring.threads.virtual.enabled=true
```
This automatically configures the Tomcat web server and the `@Async` task executor to use Virtual Threads.

---

## 5. Key Interfaces and Classes to Know

| Feature | Interface/Class | Use Case |
| :--- | :--- | :--- |
| **Execution** | `TaskExecutor` | Generic task execution. |
| **Background Task**| `@Async` | Fire-and-forget or non-blocking method calls. |
| **Result Handling**| `CompletableFuture<T>`| Handling results of async operations and chaining them. |
| **Scheduling** | `TaskScheduler` | Running tasks at fixed intervals or specific times. |
| **Concurrent Map** | `ConcurrentHashMap` | Sharing thread-safe state between services. |

---

## 6. Best Practices & Pitfalls
- **Self-Invocation**: If a method in Class A calls another `@Async` method in Class A, the async behavior will **not** work because it bypasses the Spring Proxy.
- **Exception Handling**: `@Async` methods returning `void` cannot propagate exceptions back to the caller. Use an `AsyncUncaughtExceptionHandler`.
- **Thread Local Isolation**: Be careful when using `ThreadLocal` or Spring's `@RequestScope` beans in async threads, as they don't automatically transfer between threads.

