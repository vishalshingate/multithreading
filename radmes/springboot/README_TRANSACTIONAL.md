# Deep Dive: How Spring `@Transactional` Works Internally

This guide explains the internal mechanics of Spring's declarative transaction management using the `@Transactional` annotation.

## High-Level Overview

Spring does not "magic" transactions into your code. Instead, it uses **AOP (Aspect-Oriented Programming)** and the **Proxy Pattern** to wrap your actual bean with a proxy that handles the transaction lifecycle.

When you call a method annotated with `@Transactional`, you are actually calling a method on a **Spring Proxy**, not your actual class instance.

---

## The Internal Mechanics (Step-by-Step)

### 1. The Proxy Pattern
Spring creates a proxy wrapper around your bean. This proxy holds a reference to the actual bean (the target).

*   **JDK Dynamic Proxy:** Used if your class implements an Interface.
*   **CGLIB Proxy:** Used if your class does NOT implement an interface (common in Spring Boot classes).

**Visualization:**
```text
Caller  ---->  [ Proxy (Spring) ]  ---->  [ Actual Service Bean ]
```

### 2. The Transaction Interceptor
The proxy uses a `TransactionInterceptor` (an AOP Advice) to intercept the method call. This interceptor contains the logic to start, commit, or rollback transactions.

### 3. Execution Flow

1.  **Caller invokes method:** `libraryEventsService.processLibraryEvent(...)`
2.  **Proxy intercepts:** Scanning the `@Transactional` configuration (propagation, isolation, read-only).
3.  **Transaction Manager Request:** The proxy asks the `PlatformTransactionManager` (e.g., `JpaTransactionManager`) to open a new transaction (or join an existing one).
4.  **Database Connection:** The Transaction Manager binds a JDBC Connection/Hibernate Session to the **current thread** (using `ThreadLocal`).
5.  **Method Execution:** The proxy calls the actual method on your target bean: `processLibraryEvent(...)`.
6.  **Outcome Handling:**
    *   **Success:** If the method returns without exception, the proxy asks the Transaction Manager to **COMMIT**.
    *   **Failure:** If an **unchecked exception** (`RuntimeException` or `Error`) is thrown, the proxy asks the Transaction Manager to **ROLLBACK**.
    *   *Note:* By default, Checked Exceptions do *not* trigger rollback unless specified (`rollbackFor = Exception.class`).
7.  **Cleanup:** The connection is unbound from the thread and closed (returned to the pool).

---

## Key Components

| Component | Role |
| :--- | :--- |
| **`PlatformTransactionManager`** | The central interface (e.g., `DataSourceTransactionManager`, `JpaTransactionManager`) responsible for `getTransaction()`, `commit()`, and `rollback()`. |
| **`TransactionInterceptor`** | The AOP advice that wraps the method call. It catches exceptions and determines if a rollback is needed. |
| **`ThreadLocal`** | Used by `TransactionSynchronizationManager` to store the connection/session so that all repository calls in the same thread use the *same* physical DB connection. |

---

## Common Pitfalls (Interview Favorites)

### 1. Self-Invocation (The "This" Issue)
Calls within the same class do **not** go through the proxy.

*   **Scenario:**
    ```java
    public class MyService {
        public void methodA() {
            methodB(); // Internal call
        }

        @Transactional
        public void methodB() { ... }
    }
    ```
*   **Problem:** Calling `methodA()` (no transaction) which calls `methodB()` internally will **BYPASS** the transaction on `methodB()`. `methodB()` will run without a transaction because `this.methodB()` calls the target object directly, skipping the Spring Proxy.
*   **Fix:** Self-inject the bean or move `methodB` to a different service.

### 2. Private Methods
`@Transactional` only works on **public** methods.
*   **Reason:** CGLIB/JDK Proxies cannot override/intercept private methods. The annotation will be ignored silently.

### 3. Checked Exceptions
By default, rollback only happens on `RuntimeException`.
*   **Problem:** If you throw `throw new IOException()`, the transaction commits!
*   **Fix:** Always use `@Transactional(rollbackFor = Exception.class)` or catch and wrap in a RuntimeException.

### 4. Transaction Propagation
Understand how transactions interact.
*   `REQUIRED` (Default): Use existing or create new.
*   `REQUIRES_NEW`: Suspend current, create a completely new independent transaction.
*   `MANDATORY`: Fail if no transaction exists.

---

## Read-Only Transactions (`@Transactional(readOnly = true)`)
*   **Optimization:** Hints to the JPA/Hibernate provider that we won't modify data.
*   **Effect:** Hibernate turns off "Dirty Checking" (scanning objects for changes), which improves performance for fetch operations.

