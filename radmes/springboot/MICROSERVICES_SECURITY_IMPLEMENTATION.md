# Implementing Spring Security in Microservices Architecture

This guide details how to implement a production-grade security layer for your specific project structure (`api-gateway`, `currency-exchange-service`, `currency-conversion-service`) using **OAuth2** and **OIDC**.

---

## 1. Architecture Overview

In a distributed system, we want "Centralized Authentication" and "Decentralized Authorization".

| Service | Role | Responsibility |
| :--- | :--- | :--- |
| **Identity Provider (IdP)** | Authentication Server | Standalone server (e.g., Keycloak, Okta, Auth0) that stores users and issues tokens. |
| **API Gateway** | **OAuth2 Client** | The single entry point. Handles user login (SSO), maintains the session, and passes the Access Token (JWT) downstream. |
| **Currency Services** | **Resource Server** | They do **not** provide login pages. They simply validate the JWT in the `Authorization` header and check permissions. |

---

## 2. API Gateway Implementation (`api-gateway`)

The Gateway is the "Bouncer". It ensures no unauthenticated traffic reaches your core services.

### A. Add Dependencies
Edit `api-gateway/build.gradle`:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client' # For "Login with..."
implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
```

### B. Configure Security Chain (WebFlux)
Since Spring Cloud Gateway is built on Reactive stack (WebFlux), we use `ServerHttpSecurity`.

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/header").permitAll() // Open endpoints
                .anyExchange().authenticated()       // Secure everything else
            )
            .oauth2Login(Customizer.withDefaults()); // Enable OAuth2 Login (redirect to IdP)
        return http.build();
    }
}
```

### C. Token Relay (Crucial Step)
By default, the Gateway logs you in but **does not** automatically pass the token to downstream services. You need the `TokenRelay` filter.

In `application.yml` (Gateway):
```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - TokenRelay # Automatically extracts Access Token and adds "Authorization: Bearer <token>" to downstream requests
      routes:
        - id: currency-exchange
          uri: lb://CURRENCY-EXCHANGE
          predicates:
            - Path=/currency-exchange/**
```

---

## 3. Currency Exchange Service (`currency-exchange-service`)

This service acts as a **Resource Server**. It trusts tokens issued by the IdP.

### A. Add Dependencies
Edit `currency-exchange-service/build.gradle`:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server' # For validating JWTs
```

### B. Configure Security
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll() // Allow monitoring
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

### C. Properties
Tell the service where to find the public keys to verify the JWT signature.

```properties
# application.properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://YOUR_ID_PROVIDER/realms/microservices
```

---

## 4. Currency Conversion Service (Advanced Scenario)

This service is unique because it is **both**:
1.  A **Resource Server** (it receives requests from Gateway).
2.  A **Client** (it calls `currency-exchange` using Feign).

### A. The Challenge
When `currency-conversion` calls `currency-exchange` using Feign, the `Authorization` header is **lost** by default. You must manually propagate it.

### B. Feign Client Interceptor
Create a configuration to intercept outgoing Feign requests and attach the current JWT.

```java
@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Get the token from the Security Context of the current incoming request
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            String tokenValue = jwtToken.getToken().getTokenValue();
            
            // Add it to the outgoing Feign request
            template.header("Authorization", "Bearer " + tokenValue);
        }
    }
}
```

---

## 5. Naming Server (`naming-server`)

The Eureka Server is usually internal, but it's best practice to secure it with **Basic Auth**.

1.  Add `spring-boot-starter-security`.
2.  Configure properties:
    ```properties
    spring.security.user.name=eureka
    spring.security.user.password=password
    ```
3.  Update clients (`api-gateway`, etc.) to use credentials:
    ```properties
    eureka.client.serviceUrl.defaultZone=http://eureka:password@localhost:8761/eureka/
    ```

---

## 6. Testing the Flow

1.  Open Browser -> `http://localhost:8765/currency-exchange/from/USD/to/INR` (Gateway).
2.  Gateway detects you are not logged in -> Redirects to IdP (e.g., Keycloak Login Page).
3.  You Login.
4.  IdP redirects back to Gateway with code -> Gateway gets Token.
5.  Gateway forwards request to `currency-exchange` with `Authorization: Bearer eyJhb...`.
6.  `currency-exchange` validates signature against IdP.
7.  Request succeeds.
