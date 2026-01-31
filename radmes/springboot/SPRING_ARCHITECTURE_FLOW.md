# Spring Boot Architecture & Best Practices

This document explains the standard flow of data between Spring components and key concepts like Idempotency and Locking.

---

## 1. Component Communication Flow

In a standard Spring Boot application, logic is divided into layers to ensure separation of concerns.

### Layer Architecture
1.  **Controller (`@RestController`)**: Handles HTTP requests, input validation, and returns responses. It should **not** contain business logic.
2.  **Service (`@Service`)**: Contains the business logic, transaction management, and coordinates between different repositories or external services.
3.  **Repository (`@Repository`)**: Handles data access and persistence logic (Spring Data JPA).
4.  **Component (`@Component`)**: A generic stereotype for any Spring-managed bean. Often used for utility classes or cross-cutting concerns that don't fit into the other layers.

### Flow Breakdown
- **Controller to Service**: The Controller injects the Service. It translates API models (DTOs) to domain objects and calls service methods.
- **Service to Repository**: The Service injects one or more Repositories to perform CRUD operations on the database.
- **Service to Component**: Services can inject generic `@Component` beans for tasks like hashing, external API calls, or specific calculations.

---

## 2. Idempotency in Your API

**Idempotency** means that performing the same operation multiple times has the same result as performing it once.

### Does your code have Idempotency?
Looking at your `CurrencyExchangeController`:

*   **GET** (`/currency-exchange/from/{from}/to/{to}`): **Idempotent**. Reading data doesn't change state.
*   **PUT** (`/currency-exchange/{id}`): **Idempotent**. In your implementation, you replace the existing resource with the provided body. Calling it 10 times with the same body results in the same state.
*   **PATCH** (`patchExchange`): **Idempotent**. Since you are setting specific fields (e.g., `setFrom`), repeating the call sets it to the same value.
*   **DELETE** (`deleteExchange`): **Idempotent**. The first call deletes the record. Subsequent calls find nothing to delete, and the final state (record gone) remains the same.
*   **POST** (`/currency-exchange`): **NOT Idempotent**. Each POST call typically creates a *new* record. If you send the same POST request twice, you might end up with two records unless you have a unique constraint on `{from, to}`.

---

## 3. Optimistic Locking vs @Version

### What is Optimistic Locking?
Optimistic Locking is a **strategy** used to prevent "lost updates" when two users try to update the same record simultaneously. It assumes that conflicts are rare. Instead of locking the row when reading (Pessimistic), it checks if the record was modified by someone else *only at the moment of saving*.

### What is @Version?
`@Version` is the **JPA annotation** used to implement Optimistic Locking.

| Feature | Description |
| :--- | :--- |
| **Mechanics** | When a record is read, the `version` value is also read. When saving, JPA executes: `UPDATE ... WHERE id=? AND version=?`. |
| **Success** | If the version matches, the update succeeds and the version is incremented. |
| **Failure** | If another process changed the record, the version in the DB will be different. JPA throws an `OptimisticLockingFailureException`. |

### Key Difference
- **Optimistic Locking** is the conceptual approach to concurrency.
- **`@Version`** is the technical tool/field used by Hibernate/JPA to track changes and enforce that strategy.

---

## 4. @Transactional and Concurrency

When you put `@Transactional` on a method, Spring ensures that the entire method runs within a single database transaction.

### What happens with concurrent calls?
If two threads call the same `@Transactional` method at the same time:

1.  **Isolation**: Both threads will start their own separate database transactions.
2.  **Concurrency**: The code inside the method will execute concurrently in both threads. `@Transactional` does **not** act like a `synchronized` block; it doesn't stop other threads from entering the method.
3.  **Database Locking**:
    - If both threads try to **update the same row**, the database will typically place a write lock. The second transaction will wait for the first one to either `COMMIT` or `ROLLBACK`.
    - Once the first transaction commits, the second one proceeds.

### The "Lost Update" Problem
Without locking strategies, the second transaction might overwrite the changes made by the first one (since it might have read the "old" data before the first transaction committed).

**This is why you use Optimistic Locking (@Version):**
- With `@Version`, when the second transaction finally tries to commit its update, JPA will detect that the version number in the database has already been incremented by the first transaction.
- It will then throw an `OptimisticLockingFailureException`, preventing the data from being silently overwritten.

---

## 5. Lifecycle of @Configuration and @Bean

A common question is: **When does Spring Boot actually instantiate the beans defined in a `@Configuration` class?**

### The Startup Sequence
1.  **Context Refresh**: When you run `SpringApplication.run()`, Spring starts the "Application Context Refresh" process.
2.  **Configuration Processing**: Spring first identifies all classes annotated with `@Configuration`. It creates a "proxy" of these classes (using CGLIB) to ensure that if one `@Bean` method calls another, the same singleton instance is returned.
3.  **Bean Definition Registration**: Spring scans the `@Bean` methods and registers their metadata (Bean Definitions) but doesn't create the objects yet.
4.  **Instantiation (The "When")**: 
    - By default, all beans are **Singletons** and are **Eagerly** initialized. This means Spring creates them as the application starts up.
    - Spring instantiates all non-lazy singletons during the **Final Stage** of the context refresh (`finishBeanFactoryInitialization`).
    - This happens **before** the application is fully started and ready to handle requests.

### Exceptions to the Rule
- **`@Lazy`**: If you annotate a `@Bean` with `@Lazy`, Spring will only instantiate it the first time it is actually needed (injected) in another bean.
- **`@Scope("prototype")`**: A new instance is created every time the bean is requested from the container.

---

## 6. Best Practices Summary
- **Keep Controllers Thin**: Only handle Request/Response.
- **Business Logic in Services**: Always wrap database operations in `@Transactional` service methods.
- **Use DTOs**: Avoid exposing your `@Entity` directly in the Controller layer to prevent accidental over-posting or leaking internal sensitive fields.
