# Spring Boot Interview Questions and Answers (5+ Years Experience)

This document covers core concepts, internals, and advanced scenarios often asked in senior Java/Spring Boot interviews.

## 0. Fundamentals & Basics

### Q1: What is Spring Boot and how does it differ from Spring Framework?
**Answer:**
*   **Spring Framework** provides the core features (DI, AOP, TX management) but requires significant manual configuration (XML or JavaConfig).
*   **Spring Boot** is an extension of Spring that focuses on "convention over configuration". It provides:
    1.  **Auto-configuration**: Automatically configures beans based on classpath.
    2.  **Standalone**: Runs as a generic Java jar with embedded servers (Tomcat/Jetty).
    3.  **Opinionated**: Provides starter dependencies to simplify build configuration.

### Q2: What are "Starters" in Spring Boot?
**Answer:**
Starters are a set of convenient dependency descriptors that you can include in your application. They simplify Maven/Gradle configuration.
*   Instead of searching for compatible versions of 10 different libraries for a web app, you just add `spring-boot-starter-web`, which pulls in Spring MVC, Jackson, Tomcat, and Validation API with compatible versions.
*   Examples: `spring-boot-starter-data-jpa`, `spring-boot-starter-test`, `spring-boot-starter-security`.

### Q3: How can you change the default port of a Spring Boot application?
**Answer:**
By default, the embedded server starts on port 8080. You can change it in multiple ways (in order of precedence):
1.  **Command Line Argument**: `java -jar app.jar --server.port=8081`
2.  **`application.properties`**: `server.port=8081`
3.  **OS Environment Variable**: `SERVER_PORT=8081`

### Q4: What is the difference between `application.properties` and `application.yml`?
**Answer:**
Both are used for configuration.
*   **Properties**: Standard key-value format (`server.port=8080`). Good for simple, flat structures.
*   **YAML**: Hierarchical format, more readable for complex configurations with lists or nested keys.
    ```yaml
    server:
      port: 8080
    ```
    Spring Boot supports both out of the box.

### Q5: What is Dependency Injection (DI) and Inversion of Control (IoC)?
**Answer:**
*   **IoC (Inversion of Control)**: It is a principle where the control of object creation and management is transferred from the programmer to a container (Spring Context).
*   **DI (Dependency Injection)**: It is a design pattern used to implement IoC. Instead of an object creating its dependencies (e.g., `new Service()`), the container "injects" them at runtime (e.g., via Constructor or `@Autowired`). This makes code loosely coupled and testable.

## 1. Core Spring Boot Internals

### Q1: What happens internally when you annotate a class with `@SpringBootApplication`?
**Answer:**
`@SpringBootApplication` is a convenience annotation that combines three other annotations:
1.  **`@Configuration`**: Marks the class as a source of bean definitions.
2.  **`@EnableAutoConfiguration`**: This is the most critical part. It tells Spring Boot to start adding beans based on classpath settings, other beans, and various property settings. It uses `MATE-INF/spring.factories` (in older versions) or `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (in newer versions) to load auto-configuration classes.
3.  **`@ComponentScan`**: Tells Spring to look for other components, configurations, and services in the current package and sub-packages.

### Q2: How does Spring Boot Auto-Configuration work? How can you exclude a specific auto-configuration?
**Answer:**
Auto-configuration classes are ordinary Spring Configuration classes marked with `@Configuration`. They use conditional annotations (like `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`) to determine if a bean should be registered.
*   **Mechanism:** When the application starts, Spring Boot checks the classpath. For example, if `H2` is on the classpath and no `DataSource` bean is manually defined, `DataSourceAutoConfiguration` kicks in and configures an in-memory database.
*   **Exclusion:** You can exclude a specific auto-configuration using the `exclude` attribute:
    ```java
    @SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
    public class MyApplication { ... }
    ```
    Or via `application.properties`: `spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration`

### Q3: Explain the Spring Bean Lifecycle.
**Answer:**
The container manages the lifecycle of the bean:
1.  **Instantiation**: The generic definition of the bean is found and the object is instantiated (constructor called).
2.  **Populate Properties**: Dependencies are injected.
3.  **Pre-Initialization**: `BeanPostProcessor.postProcessBeforeInitialization()` is called.
4.  **AfterPropertiesSet**: If the bean implements `InitializingBean`, `afterPropertiesSet()` is called. Custom `init-method` is also executed here. `@PostConstruct` annotated methods run here.
5.  **Post-Initialization**: `BeanPostProcessor.postProcessAfterInitialization()` is called. (AOP proxies are often created here).
6.  **Ready to use**: The bean is now in the application context.
7.  **Destruction**: When the context closes, `@PreDestroy` is called, then `DisposableBean.destroy()`, then custom `destroy-method`.

---

## 2. Data Access & Transactions

### Q4: How do you handle the "Lost Update" problem in a Spring Boot application?
**Answer:**
The Lost Update problem occurs when two transactions read the same row and update it, overwriting each other's changes.
*   **Optimistic Locking (Recommended for most web apps):** Add a `@Version` field to your JPA entity.
    ```java
    @Entity
    public class Product {
        @Id
        private Long id;
        @Version
        private Integer version; // Hibernate manages this
    }
    ```
    If two threads try to update the same version, one will fail with `ObjectOptimisticLockingFailureException`.
*   **Pessimistic Locking:** Use database locks via `@Lock(LockModeType.PESSIMISTIC_WRITE)` on your repository method. This locks the row in the DB until the transaction commits.

### Q5: Explain Transaction Propagation levels, specifically `REQUIRED` vs `REQUIRES_NEW`.
**Answer:**
*   **`REQUIRED` (Default):** If an active transaction exists, the current method joins it. If not, a new one is created. If the inner method throws an exception, the *entire* transaction (outer and inner) marks for rollback.
*   **`REQUIRES_NEW`:** Always suspends the current transaction and creates a completely new, independent transaction. If the inner transaction fails, the outer transaction is *not* unaffected (unless the exception is caught). If the outer transaction fails later, the inner one (already committed) does *not* roll back.

### Q6: What is the generic N+1 Select problem in Hibernate/JPA and how to solve it?
**Answer:**
It happens when you fetch a list of entities (1 query) and then iterate over them to access a lazily loaded relationship, causing N additional queries.
**Solutions:**
1.  **Join Fetch:** Use JPQL `JOIN FETCH` (e.g., `SELECT e FROM Employee e JOIN FETCH e.department`).
2.  **Entity Graphs:** Use `@EntityGraph` annotation on the repository method to specify which paths to fetch eagerly.
3.  **Batch Sizes:** Set `@BatchSize` or `spring.jpa.properties.hibernate.default_batch_fetch_size` to fetch child entities in batches (e.g., WHERE id IN (...)) instead of one by one.

---

## 3. Web & Microservices

### Q7: How do you handle exceptions globally in Spring Boot?
**Answer:**
Using `@ControllerAdvice` (or `@RestControllerAdvice`) combined with `@ExceptionHandler`.
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
}
```
This separates error handling logic from the business logic in controllers.

### Q8: What is the difference between Constructor Injection and Field Injection (`@Autowired`)? Why is Constructor Injection recommended?
**Answer:**
*   **Field Injection:** Putting `@Autowired` directly on the field. Easy to write but makes testing harder (need reflection to set fields) and hides dependencies.
*   **Constructor Injection:** Dependencies are passed via the constructor.
    *   **Immutability:** Fields can be `final`.
    *   **Testing:** Easy to instantiate the class in unit tests by passing mocks into the constructor.
    *   **Fail-fast:** Code won't compile or app won't start if dependencies are missing (avoids `NullPointerException` later).

### Q9: Design a scenario for using Redis Cache in Spring Boot.
**Answer:**
**Scenario:** A high-traffic e-commerce Product Details Page.
1.  **Setup:** Add `spring-boot-starter-data-redis`. Configure host/port in properties.
2.  **Enable:** Add `@EnableCaching` to the main class.
3.  **Usage:**
    ```java
    @Service
    public class ProductService {
        @Cacheable(value = "products", key = "#id")
        public Product getProductById(Long id) {
             // Simulate expensive DB call
             return repository.findById(id).orElseThrow();
        }

        @CacheEvict(value = "products", key = "#product.id")
        public void updateProduct(Product product) {
             repository.save(product);
        }
    }
    ```
4.  **Considerations:** Set a TTL (Time To Live) to avoid stale data. Handle serialization (ensure objects implement `Serializable` or use JSON serializers).

### Q10: Difference between `@Component`, `@Repository`, `@Service`, and `@Controller`?
**Answer:**
Technically, `@Repository`, `@Service`, and `@Controller` are meta-annotations of `@Component`. They all register beans.
*   **`@Component`**: Generic stereotype for any Spring-managed component.
*   **`@Repository`**: Indicates a DAO. It enables **persistence exception translation** (translating raw DB exceptions like `SQLException` into Spring's `DataAccessException` hierarchy).
*   **`@Service`**: Indicates the service layer (Business Logic). Currently adds no extra behavior but documents intent and may be used for AOP (pointcuts).
*   **`@Controller`**: Indicates a web controller (handling HTTP requests).

### Q11: Explain the Circuit Breaker pattern.
**Answer:**
In microservices, if Service A calls Service B and Service B is down or slow, Service A consumes threads waiting for B, potentially crashing the whole system (Cascading Failure).
A Circuit Breaker (like Resilience4j) wraps the call.
1.  **Closed State:** Calls go through normally.
2.  **Open State:** If failure rate exceeds a threshold (e.g., 50%), the circuit opens. Calls fail immediately (Fast Fail) without hitting the external service.
3.  **Half-Open:** After a timeout, it allows limited calls to check if Service B is back up.
It is usually implemented using annotations like `@CircuitBreaker(name = "serviceB", fallbackMethod = "fallback")`.

### Q12: How to secure a Spring Boot application?
**Answer:**
Using **Spring Security**.
1.  Add `spring-boot-starter-security`.
2.  Create a `SecurityFilterChain` bean.
3.  **Authentication:** Verify identity (Login). Can use In-Memory, JDBC, LDAP, or JWT (Stateless).
4.  **Authorization:** Verify permissions. Use `HttpSecurity` to configure URL rules (e.g., `.requestMatchers("/admin/**").hasRole("ADMIN")`) or Method Security (`@PreAuthorize("hasRole('ADMIN')")`).
5.  **Protection:** CSRF protection (enabled by default), CORS configuration.

## 4. Performance & monitoring

### Q13: What is Spring Boot Actuator?
**Answer:**
It provides production-ready features to monitor and manage the application. It exposes endpoints like:
*   `/actuator/health`: Application health status.
*   `/actuator/metrics`: CPU, memory, HTTP request metrics.
*   `/actuator/env`: Environment properties.
*   `/actuator/beans`: All loaded beans.
**Security Note:** Should always be secured in production as it exposes internal details.

### Q14: Threading in Spring Boot Web Server?
**Answer:**
Spring Boot usually uses **Tomcat** (embedded). Tomcat uses a thread pool. By default, it can handle 200 concurrent threads (`server.tomcat.threads.max=200`).
*   Requests are mapped 1-to-1 with threads (Thread-per-request model).
*   If all threads are busy, requests queue up.
*   For high concurrency with blocking I/O, **Spring WebFlux** (Reactive stack) is preferred over standard Spring MVC.

## 5. Important Annotations

### Q15: What is the difference between `@Controller` and `@RestController`?
**Answer:**
*   **`@Controller`**: Used for traditional Spring MVC controllers that return a view (e.g., JSP, Thymeleaf). To return JSON, you must add `@ResponseBody` to the method.
*   **`@RestController`**: A convenience annotation that combines `@Controller` and `@ResponseBody`. Every request handling method automatically serializes the return object into the HTTP response body (usually JSON).

### Q16: What does `@Transactional` do?
**Answer:**
It defines the scope of a database transaction.
*   If a method is annotated with `@Transactional`, Spring creates a proxy.
*   When the method is called, a transaction is started (or joined).
*   If the method completes successfully, the transaction is committed.
*   If a `RuntimeException` is thrown, the transaction is rolled back. (Note: Checked exceptions do **not** trigger rollback by default unless configured with `rollbackFor`).

### Q17: Difference between `@RequestParam` and `@PathVariable`?
**Answer:**
*   **`@RequestParam`**: Extracts values from query parameters (e.g., `/users?id=123` -> `@RequestParam Long id`).
*   **`@PathVariable`**: Extracts values from the URI path itself (e.g., `/users/123` -> `@GetMapping("/users/{id}")` -> `@PathVariable Long id`).

### Q18: Difference between `@Bean` and `@Component`?
**Answer:**
*   **`@Component`** (and `@Service`, `@Repository`): Class-level annotation. Spring auto-detects these classes via Component Scanning and creates beans. You don't control the instantiation logic.
*   **`@Bean`**: Method-level annotation used within a `@Configuration` class. You explicitly write the logic to instantiate and configure the object. Useful for third-party libraries where you cannot add `@Component` to the source code.

### Q19: What is the purpose of `@ConfigurationProperties`?
**Answer:**
It binds external configuration properties (from `application.properties` or `yaml`) to a Java bean.
```java
@ConfigurationProperties(prefix = "app.mail")
@Component
public class MailConfig {
    private String host;
    private int port;
    // getters and setters
}
```
This is type-safe and cleaner than injecting individual values using `@Value`.

### Q20: How to resolve ambiguity when multiple beans of the same type exist (`@Primary` vs `@Qualifier`)?
**Answer:**
If you have two implementations of an interface (e.g., `PaymentService` -> `PaypalService`, `StripeService`).
*   **`@Primary`**: Marks one bean as the default. If `PaymentService` is requested, the `@Primary` one is injected.
*   **`@Qualifier("beanName")`**: Used at the injection point to specify exactly which bean to inject. It takes precedence over `@Primary`.

## 6. Hibernate & JPA

### Q21: What is the difference between JPA and Hibernate?
**Answer:**
*   **JPA (Java Persistence API)**: It is a **specification** (standard interface) for ORM in Java. It defines the rules (annotations like `@Entity`, `EntityManager` interface) but provides no implementation.
*   **Hibernate**: It is an **implementation** of the JPA specification. It uses the JPA standard but also offers proprietary features (like Hibernate Criteria API, extended caching) that are not part of JPA.

### Q22: Explain the Hibernate Entity Lifecycle (Object States).
**Answer:**
1.  **Transient**: The object is created (`new Student()`) but not yet associated with a Hibernate Session (EntityManager). It has no representation in the DB.
2.  **Persistent**: The object is associated with a Session (e.g., after `persist()` or `find()`). Changes to the object are automatically synced to the DB upon commit (Dirty Checking).
3.  **Detached**: The session is closed or `detach()` is called. The object exists in Java memory with an ID, but changes are not tracked by Hibernate.
4.  **Removed**: The object is scheduled for deletion from the DB (`remove()`).

### Q23: What is the difference between First Level Cache and Second Level Cache?
**Answer:**
*   **First Level Cache (L1)**: Associated with the **Session** (Transaction). It is enabled by default and cannot be disabled. If you query the same entity twice in the same transaction, Hibernate returns the cached instance.
*   **Second Level Cache (L2)**: Associated with the **SessionFactory** (Application scope). It is optional (needs configuration, e.g., using EhCache or Redis). It caches data across multiple transactions.

### Q24: What is `LazyInitializationException` and how to fix it?
**Answer:**
It occurs when you try to access a lazy-loaded relationship (e.g., `employee.getAddress()`) *after* the Hibernate Session has closed.
**Fixes:**
1.  **Initialize inside the transaction**: Access the collection while the transaction is active.
2.  **Join Fetch**: Use JPQL `JOIN FETCH` to load the data eagerly in the initial query.
3.  **Open Session In View (OSIV)**: Keep the session open until the view is rendered (enabled by default in Spring Boot, but often considered an anti-pattern due to performance risks).

### Q25: Explain `FetchType.LAZY` vs `FetchType.EAGER`.
**Answer:**
*   **LAZY**: The related data is loaded from the database only when you explicitly access the getter method. This is the default for `@OneToMany` and `@ManyToMany` (collections).
*   **EAGER**: The related data is loaded immediately along with the parent entity (usually via a JOIN). This is the default for `@ManyToOne` and `@OneToOne`.
*   **Best Practice**: Prefer **LAZY** to avoid performance issues (loading too much data) and use `JOIN FETCH` when you specifically need the related data.

### Q26: What does `CascadeType` do?
**Answer:**
It defines how state changes propagate from a parent entity to child entities.
*   **PERSIST**: If parent is saved, child is saved.
*   **MERGE**: If parent is updated, child is updated.
*   **REMOVE**: If parent is deleted, child is deleted.
*   **ALL**: Applies all operations.
*   **Example**: `@OneToMany(cascade = CascadeType.ALL)` on `Order` -> `LineItems`. If you save the Order, all LineItems are saved automatically.
