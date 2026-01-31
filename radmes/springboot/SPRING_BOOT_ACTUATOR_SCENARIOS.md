# Spring Boot Actuator: Senior Developer Scenario-Based Questions

This guide covers advanced scenarios and best practices for using Spring Boot Actuator in production environments.

---

### Scenario 1: Securing Actuator Endpoints in Production
**Question**: You are moving a microservice to production. How would you ensure that sensitive Actuator endpoints (like `/heapdump`, `/env`, or `/beans`) are not exposed to the public internet while still allowing the monitoring team to access them?

**Answer**:
1.  **Management Port**: Change the management port to be different from the main application port using `management.server.port=8081`. This allows firewalling the management port at the infrastructure level.
2.  **Spring Security**: Use Spring Security to protect the `/actuator` path. Restrict access to a specific role (e.g., `ROLE_ADMIN`).
    ```java
    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/actuator/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        );
        return http.build();
    }
    ```
3.  **Exposure Control**: Use `management.endpoints.web.exposure.include` to only expose what is absolutely necessary.

---

### Scenario 2: Custom Health Indicator for External Dependencies
**Question**: Your service depends on a legacy TCP-based service that doesn't have a REST API. How do you integrate its availability into your Spring Boot application's `/health` endpoint?

**Answer**: Implement the `HealthIndicator` interface or extend `AbstractHealthIndicator`.
```java
@Component
public class LegacyServiceHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        if (checkTcpConnection("legacy-service-host", 1234)) {
            return Health.up().withDetail("service", "Legacy TCP Service is reachable").build();
        }
        return Health.down().withDetail("error", "Cannot reach Legacy TCP Service").build();
    }

    private boolean checkTcpConnection(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
```

---

### Scenario 3: Real-time Business Metrics (KPIs)
**Question**: The product owner wants to see the number of successful currency conversions per minute on a Grafana dashboard. How would you implement this using Actuator and Micrometer?

**Answer**: Inject a `MeterRegistry` into your service and use a `Counter`.
```java
@Service
public class ConversionService {
    private final Counter conversionCounter;

    public ConversionService(MeterRegistry registry) {
        this.conversionCounter = Counter.builder("business.conversions.total")
            .description("Total number of successful currency conversions")
            .tag("status", "success")
            .register(registry);
    }

    public void convert(...) {
        // Business logic...
        conversionCounter.increment();
    }
}
```
Then, expose the `/actuator/prometheus` endpoint for Prometheus to scrape.

---

### Scenario 4: Dynamic Logging Adjustment
**Question**: A production issue is occurring that only happens under specific load. You cannot reproduce it locally. How can Actuator help you debug this without restarting the application?

**Answer**: Use the `/actuator/loggers` endpoint.
1.  **View Loggers**: `GET /actuator/loggers/com.example` to see the current levels.
2.  **Change Level**: Send a `POST` request to change the level of a specific package to `DEBUG` or `TRACE` instantly.
    ```bash
    curl -X POST -H "Content-Type: application/json" \
         -d '{"configuredLevel": "DEBUG"}' \
         http://localhost:8080/actuator/loggers/com.example.service
    ```

---

### Scenario 5: Troubleshooting Memory Leaks
**Question**: One of your pods is consistently hitting its memory limit and getting OOMKilled. How do you use Actuator to identify the cause?

**Answer**:
1.  **Heap Dump**: Use `GET /actuator/heapdump`. This triggers a JVM heap dump and downloads it as a `.hprof` file.
2.  **Analysis**: Load the file into **Eclipse MAT (Memory Analyzer Tool)** or **VisualVM** to find the "Leak Suspects" and see which objects are consuming the most memory.
3.  **Caution**: Be careful, as triggering a heap dump on a large JVM can pause the application (STW - Stop The World).

---

### Scenario 6: CI/CD Information in /info
**Question**: How do you make the `/actuator/info` endpoint display the Git commit hash and build version automatically?

**Answer**:
1.  **Build Info**: Add `springBoot { buildInfo() }` to your `build.gradle`.
2.  **Git Info**: Add the `gradle-git-properties` plugin to your project.
3.  **Enable**: Ensure `info` endpoint is exposed and the `git` and `build` info contributors are enabled (enabled by default in Spring Boot 3).

---

### Scenario 7: Readiness vs Liveness Probes
**Question**: What is the difference between `readiness` and `liveness` state in Actuator/Kubernetes, and how do you use them differently?

**Answer**:
- **Liveness (`/actuator/health/liveness`)**: Indicates if the app is "alive". If this fails, Kubernetes restarts the pod. Use this for unrecoverable states (e.g., deadlock).
- **Readiness (`/actuator/health/readiness`)**: Indicates if the app is ready to "accept traffic". If this fails, Kubernetes stops sending traffic to the pod. Use this for startup initialization or if a mandatory external service is temporarily down.
- **Config**: Enable them via `management.endpoint.health.probes.enabled=true`.

---

## Actuator Endpoints Reference

### Default Exposure (Crucial for Interviews)
*   **Web (HTTP)**: By default, ONLY the **`health`** endpoint is exposed. (Note: In Spring Boot 2.4 and older, `info` was also exposed by default).
*   **JMX**: By default, **ALL** endpoints are exposed.
*   **Enabled vs Exposed**: An endpoint can be *enabled* (the bean exists) but not *exposed* (accessible via a port). Most endpoints are *enabled* by default but not *exposed* over Web. The `shutdown` endpoint is the only one *disabled* by default.

Use `management.endpoints.web.exposure.include=*` to expose all (not recommended for production).

| Endpoint      | Description                                                                 | Property to Enable/Disable (if not default) |
| :------------ | :-------------------------------------------------------------------------- | :------------------------------------------ |
| `auditevents` | Lists audit events (like authentication success/failure).                   | `management.endpoint.auditevents.enabled`   |
| `beans`       | Returns a complete list of all Spring beans in your application.            | `management.endpoint.beans.enabled`          |
| `caches`      | Exposes available caches.                                                   | `management.endpoint.caches.enabled`         |
| `conditions`  | Shows the conditions that were evaluated on configuration and auto-config. | `management.endpoint.conditions.enabled`     |
| `configprops` | Displays a collated list of all `@ConfigurationProperties`.                 | `management.endpoint.configprops.enabled`    |
| `env`         | Exposes properties from the Spring `Environment`.                           | `management.endpoint.env.enabled`            |
| `health`      | Shows application health information.                                       | Enabled by default.                         |
| `httpexchanges`| Displays HTTP exchange information (request-response).                     | `management.endpoint.httpexchanges.enabled`  |
| `info`        | Displays arbitrary application info.                                        | Enabled by default.                         |
| `loggers`     | Shows and modifies the configuration of loggers in the application.        | `management.endpoint.loggers.enabled`       |
| `metrics`     | Shows ‘metrics’ information for the current application.                   | `management.endpoint.metrics.enabled`       |
| `mappings`    | Displays a collated list of all `@RequestMapping` paths.                   | `management.endpoint.mappings.enabled`      |
| `prometheus`  | Exposes metrics in a format that can be scraped by a Prometheus server.     | Requires Micrometer Prometheus dependency.  |
| `scheduledtasks`| Displays the scheduled tasks within your application.                     | `management.endpoint.scheduledtasks.enabled`|
| `sessions`    | Allows retrieval and deletion of user sessions from Spring Session.          | `management.endpoint.sessions.enabled`      |
| `shutdown`    | Lets the application be gracefully shutdown.                                | `management.endpoint.shutdown.enabled=true` (Default: false) |
| `threaddump`  | Performs a thread dump.                                                     | `management.endpoint.threaddump.enabled`    |
| `heapdump`    | Returns a heap dump file.                                                   | `management.endpoint.heapdump.enabled`      |

### Exposure Configuration
To expose endpoints over HTTP (Web):
```properties
# Expose specific endpoints
management.endpoints.web.exposure.include=health,info,metrics

# Expose all endpoints (Use with caution!)
management.endpoints.web.exposure.include=*

# Exclude specific endpoints
management.endpoints.web.exposure.exclude=env,beans
```
