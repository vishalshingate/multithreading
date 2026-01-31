# Spring Boot Actuator: Guide & Interview Questions

## 1. What is Spring Boot Actuator?
**Spring Boot Actuator** is a sub-project of Spring Boot that provides production-ready features to help you monitor and manage your application. It exposes operational information via HTTP endpoints or JMX beans.

**Key capabilities:**
- **Health Checks:** Is the app up? Are the database and message broker connected?
- **Metrics:** CPU usage, memory usage, request counts (integrated with Micrometer).
- **Info:** Git commit details, build version.
- **Runtime Management:** Change log levels without restarting, view thread dumps, view heap dumps.

---

## 2. How to Configure Actuator

### Step 1: Add Dependency
In `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Step 2: Enable/Disable Endpoints (in `application.properties` or `application.yml`)
By default, only `/actuator/health` and `/actuator/info` are exposed over HTTP.

**Expose all endpoints (NOT recommended for production without security):**
```properties
management.endpoints.web.exposure.include=*
```

**Expose specific endpoints:**
```properties
management.endpoints.web.exposure.include=health,info,metrics,loggers
```

### Step 3: Security Configuration
For production, you must secure sensitive endpoints (like `/env`, `/beans`, `/heapdump`).

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
            .requestMatchers(EndpointRequest.to("health", "info")).permitAll() // Public
            .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN") // Secured
            .anyRequest().authenticated()
    ).httpBasic(withDefaults());
    return http.build();
}
```

### Step 4: Custom Base Path
```properties
management.endpoints.web.base-path=/monitor
# Result: /monitor/health
```

---

## 3. Senior Software Engineer Interview Questions

### Conceptual & Architecture
1.  **Q:** Actuator exposes sensitive data (beans, env vars). How do you secure it in a production environment?
    *   **Expectation:** Discuss `Spring Security`, excluding sensitive endpoints from public access, using a separate management port (`management.server.port`) which is not exposed to the public load balancer.

2.  **Q:** How does Actuator integrate with external monitoring systems like Prometheus or Grafana?
    *   **Expectation:** Mention **Micrometer** (the facade over monitoring systems). Actuator allows Micrometer to export metrics to Prometheus (`/actuator/prometheus`).

3.  **Q:** Explain the difference between `Liveness` and `Readiness` probes in Kubernetes and how Actuator supports them.
    *   **Expectation:** `Liveness` (restart if dead) vs `Readiness` (stop sending traffic if busy/loading). Actuator exposes `/actuator/health/liveness` and `/actuator/health/readiness`.

### Implementation & Customization
4.  **Q:** We need a custom health check for our specific 3rd party API. How do you implement it?
    *   **Expectation:** Implement the `HealthIndicator` interface, override `health()`, and return `Health.up()` or `Health.down()`.

5.  **Q:** How would you change the log level of a specific package from `INFO` to `DEBUG` at runtime without restarting the application?
    *   **Expectation:** Use the `/actuator/loggers/{packageName}` endpoint. Send a `POST` request with the configured level.

6.  **Q:** What is the purpose of the `@Endpoint` annotation?
    *   **Expectation:** Creating **custom actuator endpoints**. Using `@ReadOperation` (GET), `@WriteOperation` (POST), `@DeleteOperation`.

7.  **Q:** Your application is running out of memory. Which Actuator endpoint would you use to diagnose the issue immediately?
    *   **Expectation:** `/actuator/heapdump` to download a hprof file and analyze it in tools like verify Eclipse MAT or VisualVM. `/actuator/threaddump` for stuck threads.

### Advanced / Scenarios
8.  **Q:** If `management.endpoints.web.exposure.include=*` is dangerous, why do developers use it, and what is the specific risk of the `/env` endpoint?
    *   **Explanation:**
        *   **Why used:** It allows developers to quickly inspect all beans, configuration properties, mappings, and metrics during development without whitelisting each endpoint individually.
        *   **Risks:**
            *   **Data Leakage:** The `/env` endpoint exposes all environment variables and system properties. This often includes sensitive cloud credentials (AWS keys), database passwords (if not masked), and third-party API keys.
            *   **RCE (Remote Code Execution):** This is the critical "Senior" level detail. If `/env` is writable (POST/PUT), attackers can modify properties like `logging.config` to point to a malicious remote file, or exploit "Gadget Chains" in libraries (like SnakeYAML) via specific property injections. Historically, endpoints like `/jolokia` combined with `/env` have led to full server compromise.

9.  **Q:** How can you exclude specific dependencies from the Health check (e.g., you don't want the app to go "DOWN" just because an optional Redis cache is down)?
    *   **Explanation:**
        *   **Disable Auto-configuration:** Set `management.health.redis.enabled=false` in properties. The app will no longer check Redis state for the overall status.
        *   **Health Groups (Spring Boot 2.3+):** Create a custom group (e.g., "readiness") that includes only critical components.
            ```properties
            management.endpoint.health.group.readiness.include=db,diskSpace
            # Redis is omitted, so it won't affect this group's status
            ```
        *   **Custom Health Indicator:** Implement a wrapper around the Redis check that catches exceptions and returns `Health.up().withDetail("redis", "down-but-ignored").build()` instead of `Result.down()`.

10. **Q:** Can you run Actuator on a different port than the main application? Why would you do that?
    *   **Explanation:**
        *   **Configuration:** Use `management.server.port=8081`.
        *   **Security segmentation:** This is a best practice. You can configure your firewall (AWS Security Groups, K8s Ingress) to block external traffic to port 8081 completely, while allowing port 8080 (application traffic) to the public.
        *   **Separation of Concerns:** Ensures that heavy load on the main application threads (port 8080) does not prevent monitoring tools from scraping metrics on port 8081.

