# Spring Annotations & Design Patterns Mapping

Spring Framework is essentially a giant collection of Design Patterns applied to real-world problems. Here is a definitive guide to which annotation applies which pattern.

---

## 1. Creational Patterns

| Annotation | Design Pattern | Explanation |
| :--- | :--- | :--- |
| **`@Component`** (and `@Service`, `@Repository`) | **Singleton** (Default) | By default, Spring creates only **one instance** of the bean per container. |
| **`@Scope("prototype")`** | **Prototype** | Instructs Spring to create a **new instance** every time the bean is requested. |
| **`@Configuration` + `@Bean`** | **Factory Method** | The `@Bean` method logic acts as a factory. You define *how* to create the object, and Spring calls your factory method. |
| **`@Builder`** (Lombok) | **Builder** | Often used in Spring DTOs/Entities to construct complex objects step-by-step. |
| **`ApplicationContext`** | **Abstract Factory** | The context itself is a mega-factory that creates families of related beans. |

---

## 2. Structural Patterns

| Annotation | Design Pattern | Explanation |
| :--- | :--- | :--- |
| **`@Transactional`** | **Proxy** | Spring wraps your class in a **Dynamic Proxy**. The proxy opens a transaction, calls your method, and then commits/rolls back. |
| **`@Cacheable`** | **Proxy / Decorator** | The proxy intercepts the call, checks the Cache (Redis/Map), and if found, returns it without executing your method. |
| **`@Async`** | **Proxy** | The proxy intercepts the call and submits the task to a Thread Pool, returning immediately. |
| **`@Autowired`** (Field/Setter) | **Dependency Injection** | While strictly a principle (DIP), structurally it composes objects (Association/Aggregation) automatically. |

---

## 3. Behavioral Patterns

| Annotation | Design Pattern | Explanation |
| :--- | :--- | :--- |
| **`@Qualifier` / `@Primary`** | **Strategy** | Allows selecting a specific algorithm (implementation) of an interface at runtime. (e.g., choosing `PayPalService` vs `CreditCardService`). |
| **`@EventListener`** | **Observer** | Implements the pub-sub model. When an event is published via `ApplicationEventPublisher`, method annotated with `@EventListener` triggers automatically. |
| **`@RequestMapping`** | **Front Controller / Command** | `DispatcherServlet` acts as the Front Controller. It maps the URL (Command) to the specific `@Controller` method. |
| **`@RestControllerAdvice`** | **Chain of Responsibility** | When an exception occurs, it bubbles up until a matching Exception Handler catches it (Global Error Handling). |
| **`JdbcTemplate` / `RestTemplate`** | **Template Method** | Defines the skeleton of an operation (e.g., Open Connection -> Execute -> Close Connection) but lets you override specific steps (RowMapping). |

---

## 4. Detailed Breakdown

### A. The Proxy Pattern (The Magic behind AOP)
Whenever you see "Magic" in Spring (Transactions, Auditing, Caching), it is almost always the **Proxy Pattern**.
*   **Annotations:** `@Transactional`, `@Async`, `@Cacheable`, `@Retryable`.
*   **How it works:** Spring creates a wrapper class that looks like your class.
    ```java
    // What you wrote
    public void save() { repo.save(); }
    
    // What Spring runs (The Proxy)
    public void save() {
        transactionManager.begin(); // Aspect Logic
        super.save();               // Your Logic
        transactionManager.commit(); // Aspect Logic
    }
    ```

### B. The Strategy Pattern (Dependency Injection)
*   **Annotations:** `@Autowired`, `@Qualifier`.
*   **How it works:**
    *   **Context:** `OrderService` needs to process payment.
    *   **Strategy:** `PaymentGateway` interface.
    *   **Concrete Strategies:** `PayPal`, `Stripe`.
    *   Spring injects the specific strategy based on configuration.

### C. The Factory Pattern
*   **Annotations:** `@Bean`.
*   **How it works:**
    ```java
    @Configuration
    class AppConfig {
        @Bean // Factory Method
        public S3Client s3Client() {
            return S3Client.builder().region("us-east-1").build();
        }
    }
    ```
    Spring calls this method to manufacture the `S3Client` bean.

---

## 5. DTOs: Which Patterns are used?

When working with **DTOs (Data Transfer Objects)**, multiple patterns play a role depending on how you construct and map them.

### A. The Data Transfer Object (DTO) Pattern
*   **Origin:** Martin Fowler (Patterns of Enterprise Application Architecture).
*   **Goal:** Reduce the number of network calls by aggregating data into a single object, or simply to **decouple** the database entity from the API contract.
*   **Structure:** Pure POJO (Plain Old Java Object) with getters/setters and no logic.

### B. The Builder Pattern
*   **Annotation:** `@Builder` (Lombok).
*   **Use Case:** Constructing complex DTOs without a massive constructor.
    ```java
    UserDTO dto = UserDTO.builder()
                         .name("Alice")
                         .email("alice@test.com")
                         .build();
    ```

### C. The Adapter / Mapper Pattern
*   **Concept:** Converting an **Entity** (DB World) to a **DTO** (API World).
*   **Tools:** MapStruct, ModelMapper.
*   **Pattern:** This is effectively an **Adapter Pattern** (making the Entity interface compatible with the DTO expected structure) or purely object mapping.

```java
// Mapper (Adapter)
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User entity); // Adapts Entity -> DTO
}
```
