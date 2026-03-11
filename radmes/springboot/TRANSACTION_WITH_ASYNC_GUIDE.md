# Guide: Invoking Async Methods After Transaction Commit

When combining `@Transactional` (database operations) with `@Async` (external calls like emails), you must be careful about **when** the async method is triggered.

## The Problem: Race Conditions & Rollbacks
If you call an `@Async` method directly inside a `@Transactional` block:

```java
@Transactional
public void placeOrder(Order order) {
    orderRepository.save(order); // 1. DB Insert (Not committed yet)
    emailService.sendConfirmation(order); // 2. Async call starts immediately
} // 3. Transaction Commits here
```

### Risk 1: Race Condition (Data Visibility)
The `sendConfirmation` method runs in a *different thread*. If it tries to read the order from the database immediately, it might **find nothing** because the main transaction hasn't committed yet (Isolation Level: Read Committed).

### Risk 2: False Positive Emails
If the `placeOrder` method fails *after* calling `sendConfirmation` (e.g., an exception occurs at step 3), the transaction rolls back. The order is *not* saved, but the email **was already sent**.

---

## Solution 1: `TransactionSynchronizationManager` (Programmatic)
You can register a callback to run code *only after* the current transaction successfully commits.

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private EmailService emailService;

    @Transactional
    public void placeOrder(Order order) {
        orderRepository.save(order);

        // Register a synchronization callback
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // This runs ONLY if the transaction commits successfully
                emailService.sendConfirmation(order);
            }
        });
    }
}
```

---

## Solution 2: Spring Events + `@TransactionalEventListener` (Declarative)
This is the cleaner, more "Spring-like" approach. You publish an event, and a listener handles the email sending *after commit*.

### Step 1: Define an Event
```java
public record OrderCreatedEvent(Order order) {}
```

### Step 2: Publish the Event
```java
@Service
public class OrderService {
    @Autowired private ApplicationEventPublisher publisher;

    @Transactional
    public void placeOrder(Order order) {
        orderRepository.save(order);
        // Publish event immediately
        publisher.publishEvent(new OrderCreatedEvent(order));
    }
}
```

### Step 3: Listen with `@TransactionalEventListener`
The listener will wait until the phase you specify (default is `AFTER_COMMIT`).

```java
@Component
public class OrderEventListener {

    @Autowired private EmailService emailService;

    // Use phase = TransactionPhase.AFTER_COMMIT (Default)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async // Run in background thread
    public void handleOrderCreated(OrderCreatedEvent event) {
        emailService.sendConfirmation(event.order());
    }
}
```

### Advantages of Events:
1.  **Decoupling**: The `OrderService` doesn't need to know about email logic.
2.  **Safety**: The email is never sent if the DB transaction rolls back.
3.  **Performance**: The email sending happens asynchronously *after* the critical DB lock is released.

---

## Summary
| Feature | Direct Async Call | Transaction Synchronization | `@TransactionalEventListener` |
| :--- | :--- | :--- | :--- |
| **Execution Time** | Immediately | After Commit | After Commit |
| **Rollback Safety** | ❌ Unsafe (Email sent on failure) | ✅ Safe | ✅ Safe |
| **Data Visibility** | ❌ Race Condition Possible | ✅ Data is Committed | ✅ Data is Committed |
| **Complexity** | Low | Medium | Low (Decoupled) |
# Guide: Calling `@Async` from a Normal Method

You asked: **"Can we call an Async method inside a normal method and will it work?"**

The short answer is: **YES**, but with one major catch called **"Self-Invocation"**.

## The Rule of Thumb
1.  **If the `@Async` method is in a DIFFERENT Bean (Service):**
    *   ✅ **IT WORKS.** The method runs in a separate thread.
    *   Spring injects a proxy of the other bean, so the interception happens correctly.

2.  **If the `@Async` method is in the SAME Class:**
    *   ❌ **IT DOES NOT WORK.** The method runs in the **same thread** (synchronously).
    *   You are calling `this.method()`, which bypasses the Spring Proxy. Spring never gets a chance to intercept the call and spin up a new thread.

---

## Scenario 1: The "Self-Invocation" Problem (Doesn't Work)

When you call a method within the same class, you are bypassing the proxy.

```java
@Service
public class ReportService {

    // Normal Method
    public void generateReport() {
        System.out.println("1. Generating Report...");
        // PROBLEM: Calling 'this.sendEmail()'. The Proxy is bypassed!
        sendEmail(); 
        System.out.println("3. Report Done.");
    }

    @Async
    public void sendEmail() {
        // This runs on the SAME thread as generateReport()!
        System.out.println("2. Sending Email..."); 
    }
}
```
**Output:**
```
1. Generating Report...
2. Sending Email... (BLOCKS here until finished)
3. Report Done.
```

---

## Scenario 2: The Solution (Separate Beans)

Move the async method to a different component. Now `ReportService` calls the `EmailService` *Project*, which handles the async logic.

```java
@Service
public class ReportService {

    @Autowired
    private EmailService emailService; // Spring injects a Proxy

    public void generateReport() {
        System.out.println("1. Generating Report...");
        // CORRECT: Calling a method on the Proxy
        emailService.sendEmail(); 
        System.out.println("3. Report Done.");
    }
}

@Service
public class EmailService {
    @Async
    public void sendEmail() {
        // Runs on a separate thread (e.g., task-1)
        System.out.println("2. Sending Email..."); 
    }
}
```
**Output:**
```
1. Generating Report...
3. Report Done.
2. Sending Email... (Happens in background)
```

## Solution 3: Self-Injection (The Hack)
If you absolutely *must* keep them in the same class, you can inject the class into itself (`@Lazy` is required to avoid circular dependency cycles).

```java
@Service
public class ReportService {

    @Autowired @Lazy
    private ReportService self; // Injecting the Proxy of this class

    public void generateReport() {
        // Call the method on the 'self' proxy, not 'this'
        self.sendEmail(); 
    }

    @Async
    public void sendEmail() { ... }
}
```


