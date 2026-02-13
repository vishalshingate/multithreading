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

---

## 7. Deep Dive: How `@Async` Works Under the Hood

When you add `@EnableAsync` and annotate a method with `@Async`, Spring performs "magic" using **Spring AOP (Aspect Oriented Programming)**.

### The Internal Flow
1.  **Bean Post-Processing**:
    - During startup, the `AsyncAnnotationBeanPostProcessor` scans all beans.
    - If it finds a bean with `@Async` methods, it **wraps that bean in a Proxy** (usually a CGLIB proxy).

2.  **Method Interception**:
    - When you call `myService.asyncMethod()`, you are actually calling the **Proxy**, not the real object.
    - The Proxy delegates the call to an `AsyncExecutionInterceptor`.

3.  **Task Submission**:
    - The interceptor wraps your method logic into a `Callable` or `Runnable`.
    - It submits this task to the configured `Executor` (e.g., `ThreadPoolTaskExecutor`).

4.  **Immediate Return**:
    - The main thread returns immediately without waiting for the task to finish.
    - If the method returns `CompletableFuture`, the interceptor returns a new `Future` that will be completed later by the background thread.

### Diagram
```text
Caller Thread
   |
   | calls method()
   v
[ Spring Proxy ] <--- Intercepts call
   |
   | Uses AsyncExecutionInterceptor
   | Submits task to ThreadPool
   | Returns immediately
   v
(Caller continues...)

       [ Thread Pool (Worker Thread) ]
              |
              | Executes actual logic
              v
       (Real Service Object)
```

### Why internal calls fail?
This explains the "Self-Invocation" pitfall.
- If `methodA()` calls `this.methodB()` (where `methodB` is async), it calls the method directly on the **target class instance**, bypassing the **Spring Proxy**.
- Without the Proxy, there is no interception, so the method runs strictly synchronously on the same thread.

---

## 8. How to Fix Self-Invocation (Calling Async Correctly)

If you need `methodA()` to call `methodB()` (which is async) within the same class, you must ensure the call goes through the **Spring Proxy**.

### Option 1: Move to a Separate Service (Recommended)
This is the cleanest approach. Separation of concerns usually dictates that if a task is big enough to be async, it might belong in its own component.

```java
@Service
public class OrderService {
    @Autowired
    private EmailService emailService; // Separate bean

    public void placeOrder() {
        // ... logic ...
        emailService.sendEmail(); // Works! Goes through Proxy.
    }
}
```

### Option 2: Self-Injection (The Workaround)
You can inject the bean into itself. You **must** use `@Lazy` to avoid a circular dependency error during startup.

```java
@Service
public class MyService {

    @Autowired
    @Lazy // specific annotation to break circular cycle
    private MyService self;

    public void mainMethod() {
        System.out.println("Main: " + Thread.currentThread().getName());
        // Call the method on the 'self' proxy, not 'this'
        self.asyncMethod(); 
    }

    @Async
    public void asyncMethod() {
        System.out.println("Async: " + Thread.currentThread().getName());
    }
}
```

### Option 3: Manual Context Lookup (Last Resort)
Retrieve the bean from `ApplicationContext` manually.

```java
@Service
public class MyService {
    @Autowired
    private ApplicationContext context;

    public void process() {
        MyService proxy = context.getBean(MyService.class);
        proxy.asyncMethod();
    }
}
```
