# Troubleshooting and Operations Guide

This document provides solutions for common issues and operational tasks.

---

## 1. Zipkin Traces Not Showing in UI

If you don't see traces in the Zipkin UI at `http://localhost:9411`, check the following:

### Dependencies (Spring Boot 3)
Ensure you have the following dependencies in your `build.gradle`:
```groovy
implementation 'io.micrometer:micrometer-tracing-bridge-brave'
implementation 'io.zipkin.reporter2:zipkin-reporter-brave'
```

### Configuration
Verify your `application.properties` or Docker environment variables:
- **Sampling Probability**: Ensure it's set to `1.0` (100%) for development.
  `management.tracing.sampling.probability=1.0`
- **Zipkin Endpoint**:
  `management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans`

---

## 2. MSSQL Logging in Docker

### Enable SQL Statement Logs in Spring Boot
To see every SQL statement being executed:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

---

## 3. Circuit Breaker Configuration (Resilience4j)

In `ApiGateWayConfiguration.java`, you set the name of the circuit breaker:
```java
.filters(f -> f.circuitBreaker(c -> c.setName("currencyExchangeCB")))
```

To set the actual **limits** (thresholds), you must add properties to `api-gateway/src/main/resources/application.properties`:

```properties
# Failure rate threshold in percentage (default is 50)
resilience4j.circuitbreaker.instances.currencyExchangeCB.failure-rate-threshold=50

# Wait duration in open state (before trying again)
resilience4j.circuitbreaker.instances.currencyExchangeCB.wait-duration-in-open-state=10s

# Sliding window size (number of calls to consider for threshold)
resilience4j.circuitbreaker.instances.currencyExchangeCB.sliding-window-size=10

# Timeout duration for the call
resilience4j.timelimiter.instances.currencyExchangeCB.timeout-duration=2s
```

---

## 4. General Tips
- **Network Discovery**: When running in Docker, use container names (e.g., `http://zipkin:9411`) instead of `localhost`.
- **Heap Space**: If services crash with `OutOfMemoryError`, increase the JVM heap in Docker:
  `environment: ["JAVA_OPTS=-Xmx512m -Xms256m"]`
