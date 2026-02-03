# Spring Boot Scenario-Based Deep Dives

This guide covers real-world production scenarios, troubleshooting techniques, and architectural decisions that distinguish senior developers.

---

## 1. Scaling Pods: "Stateful usage in a Stateless architecture"
**Problem:** A Spring Boot app works perfectly in local/single-node but fails or behaves inconsistently when scaled to multiple pods in Kubernetes.

### Symptoms
- **Stale Data:** User updates profile on Pod A, but Pod B (hit on next request) shows old data.
- **Duplicate Jobs:** A `@Scheduled` task to send daily emails runs 3 times if you have 3 pods.
- **Session Loss:** User logs in on Pod A; next request hits Pod B and they are logged out.
- **Connection Spikes:** Each pod has `max-connections=50`. Scaling to 10 pods suddenly slams the DB with 500 connections.

### Solutions
- **Distributed Cache:** Use Redis or Memcached for application caching instead of local `ConcurrentHashMap`.
- **Distributed Locking:** Use **ShedLock** or Spring Integration for `@Scheduled` tasks to ensure only one pod runs a task.
- **Distributed Sessions:** Use **Spring Session Redis** to store session data in a central store.
- **Service Mesh/Sidecars:** Use tools like Istio for advanced routing or connection pooling/throttling at the infrastructure level.

---

## 2. Duplicate Bean Registration in Multi-Module Projects
**Problem:** Startup fails with `BeanDefinitionOverrideException` or you see "two beans of type X found".

### Root Causes
- **Overlapping @ComponentScan:** Module A scans `com.example`. Module B also scans `com.example`. Both pick up the same classes.
- **Inadvertent Root Scanning:** Placing the `@SpringBootApplication` class in a package like `com` (too high), causing it to scan every library on the classpath.
- **Explicit @Import + Scanning:** You manually `@Import(MyConfig.class)` while it's already in a package being component-scanned.

### Solutions
- **Package Hygiene:** Be explicit. Use `@ComponentScan(basePackages = "com.myapp.module.a")`.
- **Conditional Loading:** Use `@ConditionalOnMissingBean` in library configurations to allow the main app to override them without conflicts.
- **Exclusions:** Use `excludeFilters` on `@ComponentScan` if you must avoid specific classes.

---

## 3. Why Custom Exception Handlers Sometimes Don't Trigger
**Problem:** You have a `@ControllerAdvice` but the API still returns a default 500 or 403 page.

### Root Causes
- **Filter Exceptions:** Spring Security happens in **Filters**, which are outside the `DispatcherServlet` scope. `@ControllerAdvice` only catches exceptions *inside* the DispatcherServlet.
- **Response Committed:** If you've already started writing the JSON response body and *then* an exception happens (e.g., during serialization), the HTTP status cannot be changed.
- **Internal Calls (AOP):** If method A calls method B (annotated with `@Transactional` or custom logic) within the same class, the Proxy is bypassed.

### Solutions
- **AuthenticationEntryPoint:** For Security exceptions, implement `AuthenticationEntryPoint` or `AccessDeniedHandler`.
- **ErrorController:** Implement a global `ErrorController` (inheriting from `BasicErrorController`) to catch anything the DispatcherServlet missed.

---

## 4. Hibernate: Unexpected Queries & N+1 Problem
**Problem:** One simple API call triggers dozens or hundreds of SQL queries.

### The "N+1" Problem
- **Scenario:** You fetch 10 `Orders`. Each `Order` has 1 `Customer` (Lazy).
- **Queries:** 1 SELECT for Orders + 10 SELECTs for each Customer when accessed.
- **Solution:** Use **Entity Graphs** or `JOIN FETCH` in your Repository query.

### OSIV (Open Session In View)
- Spring Boot enables `spring.jpa.open-in-view=true` by default.
- It keeps the DB connection open until the JSON is finished rendering.
- **Risk:** Developers accidentally trigger lazy-loading in the Controller/DTO mapping, causing hidden N+1 queries.
- **Solution:** Set `spring.jpa.open-in-view=false` to force `LazyInitializationException` in dev, pushing queries into the Service/Repo layer.

---

## 5. Debugging Deadlocks in Spring Boot
**Problem:** Application is unresponsive; some API calls hang indefinitely.

### Detection
- **Thread Dumps:** Run `jstack <PID>` or use **VisualVM**. Look for the "Found one Java-level deadlock" message.
- **Actuator:** Check `/actuator/threaddump`.

### Common Causes
- **DB Deadlock:** Two transactions updating Table A and Table B in different orders.
- **Pool Exhaustion:** Thread A waits for a DB connection from a pool that is empty because all other threads are waiting for a resource Thread A holds.

---

## 6. @Cacheable Not Caching
**Problem:** Logic inside the cached method runs every time.

### Root Causes
- **Self-Invocation:** `this.methodName()` ignores the Spring Proxy. Use constructor injection to inject "self" or move the method to a different bean.
- **Private Methods:** Spring AOP cannot proxy `private` methods.
- **Missing @EnableCaching:** You forgot to put this on a `@Configuration` class.

---

## 7. Reloading Configs Without Restart (@RefreshScope)
**Problem:** You changed a property in Git/Config Server and want the app to pick it up live.

### How it works
- **@RefreshScope:** Beans marked with this are actually **Proxies**.
- When `/actuator/refresh` is hit, the internal instance is destroyed.
- On the next call, a new instance is created with the new property values.
- **Note:** This requires **Spring Cloud Context**.

---

## 8. Logging: Async Logging for Production
**Problem:** High traffic causes the app to slow down because it's waiting for disk I/O to write logs.

### Solution
- **Logback AsyncAppender:** Wraps your `FileAppender`. It uses a bounded queue.
- **Log4j2 LMAX Disruptor:** An even faster lock-free inter-thread communication library for logging.
- **Why?** Writing to disk is orders of magnitude slower than memory. Async logging ensures the business logic thread stays fast.

---

## 9. Handling Partial Failures (Resilience4j)
**Problem:** Service A calls Service B. Service B is slow/down, causing Service A to hang and eventually crash (Cascading Failure).

### Patterns
- **Circuit Breaker:** If failure rate > 50%, "open" the circuit. Future calls fail immediately (Fast Fail) without hitting Service B.
- **Bulkhead:** Limit Service B calls to 10 concurrent threads. If they are busy, other parts of the app still work.
- **Rate Limiter:** Prevent Service A from overwhelming Service B.

---

## 10. @Value and @ConfigurationProperties Failures
- **@Value:** Fails if property is missing (unless default provided `${prop:default}`) or if used on `static` fields.
- **@ConfigurationProperties:** Fails silently if **Setters** are missing (for standard beans). Use `@Validated` to catch binding errors at startup.

---

## 11. Metric Explosion vs. Distributed Tracing
**Problem:** App crashes with OOM when metrics are enabled.

### High Cardinality (Metric Explosion)
- **Bad Practice:** `meterRegistry.counter("requests", "userId", user.getId()).increment();`
- If you have 1M users, Prometheus will have to store 1M separate time-series objects.
- **Metrics** are for **Aggregation** (Avg latency, 99th percentile).
- **Distributed Tracing (Tracing)** is for **Investigation** (What happened to User 123's request?).
- **Solution:** Use tags for low-cardinality data (Region, Status Code) and Tracing (Zipkin/Jaeger) for high-cardinality data (User ID, Order ID).
