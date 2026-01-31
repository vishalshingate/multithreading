# Securing Spring Boot Actuator Endpoints

Spring Boot Actuator provides production-ready features like health checks, metrics, and env info. By default, these can expose sensitive information if not properly secured.

## 1. Default Behavior
*   **Without Spring Security**: 
    *   Only `/actuator/health` and `/actuator/info` are exposed over HTTP by default.
    *   All other endpoints (beans, env, metrics, etc.) are disabled/hidden by default.
*   **With Spring Security on classpath**: 
    *   Startups often assume Actuator is secured by default. 
    *   In recent Spring Boot versions, if Spring Security is present, **all** endpoints are secured by default **except** `health`.
    *   You usually need to explicitly configure access rules.

## 2. Exposing Endpoints
In `application.properties`:
```properties
# Expose only health and info (Default)
management.endpoints.web.exposure.include=health,info

# Expose EVERYTHING (Dangerous in production!)
management.endpoints.web.exposure.include=*

# Exclude specific endpoints
management.endpoints.web.exposure.exclude=env,beans
```

## 3. Securing Actuator with Spring Security

If you expose sensitive endpoints like `/actuator/env` or `/actuator/heapdump`, you **must** restrict access.

### Configuration Example
```java
@Configuration
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Allow public access to health check (often needed by K8s/Load Balancers)
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                //.requestMatchers("/actuator/health").permitAll() // Alternative syntax
                
                // Require ADMIN role for all other actuator endpoints
                .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
                
                // Standard app security
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults()); // Use Basic Auth for simplicity
            
        return http.build();
    }
}
```

## 4. Scenario-Based Interview Questions & Answers

### Scenario A: Cloud Load Balancers & Kubernetes Liveness Probes
**Q: We deployed our app to Kubernetes. The pod keeps crashing because the Liveness Probe fails with a 401 Unauthorized error. We are using Spring Security.**
**A:** 
*   **The Problem (Why it fails):** Load Balancers (AWS ALB, Nginx) and Kubernetes Liveness/Readiness probes make simple HTTP GET requests to check if your app is alive. These requests are usually **anonymous** (they do not carry an Authorization header or Session cookie). 
*   **The Conflict:** If you have `http.anyRequest().authenticated()` in your security config, Spring Security intercepts these anonymous health checks and rejects them with `401 Unauthorized` (or redirects to a login page).
*   **The Consequence:** The Load Balancer receives a non-200 response, marks the instance as "Unhealthy," and stops sending traffic to it. In Kubernetes, the kubelet will kill and restart the pod, leading to a crash loop.
**Fix:** You must "filter out" or exclude this specific endpoint from the authentication requirement. By using `.requestMatchers("/actuator/health").permitAll()`, you tell Spring Security to allow these specific requests to pass through the security filter chain without checking for credentials.

### Scenario B: Public Internet vs Internal Network
**Q: We want our Ops team to access `/actuator/metrics` and `/actuator/loggers` to debug issues, but we don't want these exposed to the public internet users.**
**A:** 
1.  **Network Level:** Configure the firewall or Reverse Proxy (Nginx/AWS ALB) to block external access to `/actuator/**`.
2.  **Port Separation:** Spring Boot can listen on a separate port for management endpoints. 
    *   Property: `management.server.port=8081`
    *   The main app runs on 8080 (public), and actuator runs on 8081 (internal/VPN only).

### Scenario C: Unintentional Data Leak
**Q: A developer enabled `management.endpoints.web.exposure.include=*` to debug a property issue. Now, security scanning reports that database passwords are visible.**
**A:** This exposes the `/actuator/env` endpoint, which dumps all environment variables and properties.
**Fix:** 
1.  Never use `*` in production.
2.  Spring Boot automatically tries to sanitize keys named "password", "secret", "key", replacing values with `******`.
3.  If you have custom sensitive keys (e.g., `my.api.token`), configure `management.endpoint.env.keys-to-sanitize=token,apikey`.

### Scenario D: Custom Health Indicators
**Q: The `/actuator/health` returns `UP`, but users cannot save data because the disk is full. How do we reflect this in the health check?**
**A:** By default, Spring checks DB and Disk Space. If the disk is full, it should already be DOWN. However, if you rely on a specific 3rd party API, you should write a custom Health Indicator.
```java
@Component
public class ThirdPartyApiHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        if (checkApiConnection()) {
            return Health.up().build();
        }
        return Health.down().withDetail("Error", "Service Unreachable").build();
    }
}
```

### Scenario E: Auditing Sensitive Actions
**Q: We allowed the ADMIN to change log levels at runtime using `/actuator/loggers`. How do we track who changed what?**
**A:** Actuator itself creates audit events. 
1.  Ensure `httptrace` or `auditevents` endpoint is enabled (if needed).
2.  Better approach: Implement an `AuditEventRepository` bean. Spring Security publishes authentication events, and Actuator publishes audit events. You can capture these and log them to a secure audit log or database.

