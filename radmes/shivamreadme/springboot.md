# Spring Boot Interview Questions & Answers (5+ Years Experience)

This document covers key concepts and deep-dive answers for Spring Boot, focused on architecture, internals, and best practices suitable for a senior developer role.

---

## 1. Spring Core

### Spring Container & Its Role
The **Spring Container** is the core of the Spring Framework. It is responsible for instantiating, configuring, and managing the lifecycle of application objects (beans). It uses the configuration metadata (XML, Java Annotations, or Java Code) to assemble the objects.
- **BeanFactory**: The simplest container providing basic DI support.
- **ApplicationContext**: An extension of BeanFactory that adds enterprise-specific features like event publication, i18n, and integration with AOP. In Spring Boot, `AnnotationConfigServletWebServerApplicationContext` is commonly used for web apps.

### IOC & DI, Bean Life Cycle
- **IoC (Inversion of Control)**: A design principle where the control of object creation and dependency management is transferred from the application code to the container.
- **DI (Dependency Injection)**: The pattern used to implement IoC. Dependencies are "injected" into objects rather than objects creating them.
- **Bean Life Cycle**:
    1.  **Instantiation**: Container creates the bean instance.
    2.  **Populate Properties**: Dependencies are injected.
    3.  **BeanNameAware / BeanFactoryAware**: If implemented, these methods are called.
    4.  **Pre-Initialization (BeanPostProcessor)**: `postProcessBeforeInitialization()` is called.
    5.  **Initialization**: `@PostConstruct`, `InitializingBean.afterPropertiesSet()`, or custom init-method.
    6.  **Post-Initialization (BeanPostProcessor)**: `postProcessAfterInitialization()` is called (e.g., for AOP proxies).
    7.  **Usage**: Bean is ready for use.
    8.  **Destruction**: `@PreDestroy`, `DisposableBean.destroy()`, or custom destroy-method when the container shuts down.

### Bean Scope (`@Scope`)
Defines the lifecycle and visibility of a bean.
- **singleton** (Default): One instance per Spring IoC container.
- **prototype**: A new instance each time it is requested.
- **request**: One instance per HTTP request (Web only).
- **session**: One instance per HTTP session (Web only).
- **application**: One instance per `ServletContext` (Web only).
- **websocket**: One instance per WebSocket lifecycle (Web only).

### Different Ways to Inject Beans
1.  **Constructor Injection** (Recommended): Ensures immutability and that the bean is never in a partially initialized state. Best for mandatory dependencies.
2.  **Setter Injection**: Good for optional dependencies or if circular dependencies need to be resolved.
3.  **Field Injection** (`@Autowired` on field): Not recommended due to testing difficulties (hard to mock without reflection) and tight coupling with the container.

### `@SpringBootApplication`
A convenience annotation that combines:
- `@Configuration`: Marks the class as a source of bean definitions.
- `@EnableAutoConfiguration`: Tells Spring Boot to start adding beans based on classpath settings, other beans, and various property settings.
- `@ComponentScan`: Tells Spring to look for other components, configurations, and services in the `com.example` package.

### Aspect Oriented Programming (AOP)
**Difference between JoinPoint and Pointcut**:
- **JoinPoint**: A specific point during the execution of a program, such as the execution of a method or the handling of an exception. In Spring AOP, a JoinPoint always represents a method execution. It's *where* an action *can* happen.
- **Pointcut**: A predicate (expression) that matches JoinPoints. It determines *where* advice (code) should be applied.
    - *Analogy*: A restaurant menu has many dishes (JoinPoints). You order specific ones (Pointcut).

---

## 2. Database / Persistence Framework

### ORM Frameworks (Hibernate)
Object-Relational Mapping (ORM) maps Java objects to database tables. Hibernate is the most popular implementation of JPA (Java Persistence API). It handles impedance mismatch, caching, and transaction management.

### Lazy vs Eager Loading
- **Eager Loading**: Related entities are fetched immediately with the parent. Can lead to fetching too much data.
- **Lazy Loading**: Related entities are fetched only when accessed. Can lead to `LazyInitializationException` if accessed outside the transaction/session scope.

### How to Avoid `LazyInitializationException`
This exception occurs when accessing a lazy-loaded collection/proxy after the Hibernate Session has closed.
**Solutions**:
1.  **Open Session in View (OSIV)**: Keeps the session open until the view renders. Enabled by default in Spring Boot (`spring.jpa.open-in-view=true`). *Cons*: Performance heavy, database connections held longer.
2.  **`JOIN FETCH`**: Use JPQL/HQL to fetch the required association in the initial query.
    ```sql
    SELECT u FROM User u JOIN FETCH u.roles WHERE u.id = :id
    ```
3.  **Entity Graphs (`@EntityGraph`)**: Define which attributes to load at runtime.
4.  **Hibernate.initialize()**: Manually initialize the proxy within the transaction service layer.
5.  **DTO Projection**: Select only the data needed into a DTO, avoiding entity management entirely.

### First-level (Session) and Second-level (SessionFactory) Cashing
- **First-Level Cache (L1)**: Enabled by default. Associated with the `Session`. Caches objects within the current transaction. If you request the same entity twice in the same transaction, DB is hit only once.
- **Second-Level Cache (L2)**: Optional and pluggable (e.g., EhCache, Redis). Associated with the `SessionFactory`. Caches objects across sessions/transactions. Useful for read-heavy data that doesn't change often.

### Transaction Management
Spring supports:
- **Programmatic Transaction Management**: Using `TransactionTemplate`. distinctive but rare.
- **Declarative Transaction Management**: Using `@Transactional`.
    - **Propagation**: Defines how transactions relate (e.g., `REQUIRED` (default), `REQUIRES_NEW`, `NESTED`).
    - **Isolation**: Defines visibility of changes (e.g., `READ_COMMITTED`, `REPEATABLE_READ`).
    - **Rollback Rules**: By default, Spring only rolls back for `RuntimeException` (unchecked) and Errors. It does **not** rollback for checked exceptions.
        - **To rollback for checked exceptions**: Use the `rollbackFor` attribute.
        ```java
        @Transactional(rollbackFor = { SQLException.class, MyCheckedException.class })
        public void myMethod() throws MyCheckedException { ... }
        ```

    - **Where to use `@Transactional`?**
        - **Service Layer (Recommended)**:
            - **Atomicity**: Service methods often combine multiple DAO/Repository calls to perform a single unit of work (e.g., money transfer: debit A, credit B). If one fails, the entire business operation must rollback.
            - **Session Scope**: The Hibernate Session is bound to the transaction context. Keeping the transaction open in the Service layer allows efficient Lazy Loading of related entities within the business logic.
        - **Why not DAO/Repository Layer?**: Too granular. A single Service method calling three DAO methods would result in three separate transactions, breaking atomicity if the 2nd or 3rd fails.
        - **Why not Controller Layer?**: Controllers should be concerned with HTTP request/response handling, not database transaction management. Also, keeps transactions open for too long (during serialization/network I/O).

### Hibernate Optimization Techniques
1.  **Use Lazy Loading** carefully to avoid fetching entire graphs.
2.  **Batch Fetching**: Configure `hibernate.jdbc.batch_size` to group SQL inserts/updates.
3.  **Fetch Joins**: Use `JOIN FETCH` in JPQL to initialize lazy associations in a single query.
4.  **Second-Level Cache**: For read-heavy, static data.
5.  **Projections**: Select only needed columns instead of full entities.

### N+1 Query Problem
Occurs when fetching a list of parent entities (1 query) and then accessing a lazy-loaded collection for *each* parent (N queries). Total N+1 queries.
**Solution**: Use `JOIN FETCH` in JPQL/HQL or Entity Graphs to fetch the association in the initial query.

### Query Tuning
- Analyze execution plans (`EXPLAIN`).
- Use appropriate indexes.
- Avoid unnecessary joins.
- Use pagination.
- Retrieve only required columns (Projections).

### Spring Data JPA vs. Spring Data MongoDB
- **Spring Data JPA**: Abstraction over JPA (Hibernate). Uses `Repository` interface. Generates SQL. Ideal for relational data with complex transactions and joins.
- **Spring Data MongoDB**: Abstraction for MongoDB. Uses `MongoRepository`. Generates JSON-based queries. Ideal for unstructured/semi-structured data, high write throughput, and flexible schemas.

---

## 3. Controller / REST

### Idempotent HTTP Methods
An idempotent method can be called multiple times without changing the result beyond the initial application.
- **Idempotent**: `GET`, `PUT`, `DELETE`, `HEAD`, `OPTIONS`.
- **Non-Idempotent**: `POST` (creates a new resource each time), `PATCH` (usually not idempotent, depends on implementation).

### PUT vs. PATCH
- **PUT (Full Update)**: Replaces the target resource with the request payload. If the resource contains fields A, B, and C, and you send a PUT with only A, fields B and C should technically be removed or set to null. It is **Idempotent**.
- **PATCH (Partial Update)**: Applies partial modifications to a resource. Only the fields present in the request are updated. Changes are merged into the existing resource. It is **Non-Idempotent** (though often implemented as idempotent).
    - **Why Non-Idempotent?**: While setting a value (e.g., `{"status": "active"}`) is idempotent, PATCH operations *can* be dynamic. For example, if a PATCH request specifies "increment 'views' by 1" or "append 'item' to list", executing it multiple times yields different results (views=1 vs views=2), breaking idempotency.

### Richardson Maturity Model (RMM)
Levels of REST compliance:
- **Level 0**: The Swamp of POX. Single URI, single method (usually POST) for everything (e.g., SOAP/XML-RPC style).
- **Level 1**: Resources. Multiple URIs, but maybe still single method.
- **Level 2**: HTTP Verbs. Structurally using GET, POST, PUT, DELETE correctly.
- **Level 3**: Hypermedia Controls (HATEOAS). API responses include links to related actions/resources, driving the application state.

### Creating REST Endpoints in Spring Boot
Use `@RestController` (combines `@Controller` and `@ResponseBody`).
Define methods with mapping annotations (`@GetMapping`, `@PostMapping`, etc.).
Return domain objects or `ResponseEntity` for more control over status codes and headers.

### Global Exception Handling
Use `@ControllerAdvice` (or `@RestControllerAdvice`) to define a global class for exception handling.
Inside, define methods annotated with `@ExceptionHandler(ExceptionType.class)`.
This separates error handling logic from business logic.

### Are REST Endpoints Multithreaded?
Yes. The Servlet container (Tomcat/Jetty) maintains a thread pool. Each incoming request is assigned a thread from the pool. Therefore, `Controller` beans (which are singletons) must be **stateless** to be thread-safe. Do not store request-specific state in instance variables.

### DispatcherServlet Initialization
The `DispatcherServlet` is the "Front Controller" of Spring MVC.
1.  **Context Loading**: When Spring Boot starts, the `DispatcherServlet` is registered as a Servlet.
2.  **`onRefresh()`**: When the servlet initializes, it calls `onRefresh()`, which triggers `initStrategies()`.
3.  **Strategy Initialization**: It populates its delegate components from the `ApplicationContext`:
    -   `MultipartResolver` (File uploads)
    -   `LocaleResolver`
    -   `HandlerMapping` (Maps URL to Controller)
    -   `HandlerAdapter` (Executes the Controller)
    -   `ViewResolver` (Resolves String view names to artifacts)
    -   `HandlerExceptionResolver`

### How Interceptors Work
Interceptors (`HandlerInterceptor`) intercept requests **before** they reach the controller, **after** the controller executes, and **after** the view is rendered.
- **vs. Filters**: Filters (Servlet API) run before the Servlet. Interceptors (Spring Context) run inside the Servlet and have access to the application context.
- **Lifecycle Methods**:
    1.  `preHandle()`: Before controller. Return `true` to proceed, `false` to stop.
    2.  `postHandle()`: After controller, before view rendering. Can manipulate the ModelAndView.
    3.  `afterCompletion()`: After view rendering. Used for cleanup.

### How to Implement an Interceptor
1.  **Create the Interceptor Class**: Implement `HandlerInterceptor`.
    ```java
    @Component
    public class MyInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // Authentication/Logging logic
            return true; // true = proceed to controller, false = stop
        }
    }
    ```
2.  **Register the Interceptor**: Implement `WebMvcConfigurer` and override `addInterceptors`.
    ```java
    @Configuration
    public class WebConfig implements WebMvcConfigurer {
        @Autowired
        private MyInterceptor myInterceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(myInterceptor).addPathPatterns("/api/**");
        }
    }
    ```

---

## 4. Spring Security

### How to Secure REST Endpoints?
1.  Add `spring-boot-starter-security`.
2.  Create a configuration class extending `WebSecurityConfigurerAdapter` (deprecated) or defining a `SecurityFilterChain` bean.
3.  Use `.authorizeRequests()` / `.authorizeHttpRequests()` to define access rules (e.g., `.antMatchers("/admin/**").hasRole("ADMIN")`).
4.  Configure authentication mechanism (Basic Auth, JWT, OAuth2).
5.  Use method-level security (`@PreAuthorize`) for finer control.

### JWT Token Consists Of?
A JSON Web Token (JWT) has three parts separated by dots (`.`):
1.  **Header**: Algorithm and token type (e.g., `{"alg": "HS256", "typ": "JWT"}`).
2.  **Payload (Claims)**: Data (subject, issuer, expiration, roles).
3.  **Signature**: Hash of (Header + Payload) using a secret key. ensures integrity.

### JWT Token Example
**Encoded Token** (3 parts):
`eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c`

**Decoded Data**:
1.  **Header**:
    ```json
    {
      "alg": "HS256",
      "typ": "JWT"
    }
    ```
2.  **Payload (Claims)**:
    ```json
    {
      "sub": "1234567890",
      "name": "John Doe",
      "iat": 1516239022,
      "roles": ["ROLE_USER", "ROLE_ADMIN"]
    }
    ```
3.  **Signature**:
    `HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)`
