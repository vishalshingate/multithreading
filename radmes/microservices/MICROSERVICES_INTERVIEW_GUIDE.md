# Microservices Architecture Interview Guide (5+ Years Experience)

This guide covers key considerations, design patterns, and challenges when building microservices, tailored for senior developers.

## 1. Core Design Principles & Considerations

When designing a microservices architecture, you must consider the following:

### a. Service Granularity (How small is "micro"?)
*   **Bounded Context (DDD):** Align services with business domains, not just technical layers.
*   **Single Responsibility Principle:** Each service should do one thing well.
*   **Independence:** Services must be independently deployable and scalable.

### b. Database Strategy
*   **Database per Service:** The golden rule. Ensures loose coupling.
*   **Shared Database:** Anti-pattern (creates tight coupling), but valid for specific legacy migration phases.
*   **Data Consistency:** How to handle distributed transactions (SADT - Availability vs. Consistency).

## 2. Communication Patterns

### a. Synchronous vs. Asynchronous
*   **Synchronous (REST/gRPC):** Simple, real-time, but introduces coupling and latency. Use for external-facing APIs.
*   **Asynchronous (Messaging - Kafka/RabbitMQ):** Decoupled, scalable, meaningful for eventual consistency. Use for internal service-to-service communication.

### b. API Gateway vs. Service Mesh
*   **API Gateway (Zuul, Spring Cloud Gateway):** Entry point for clients. Handles routing, auth, rate limiting, and aggregation.
*   **Service Mesh (Istio, Linkerd):** Infrastructure layer for service-to-service communication (sidecar pattern). Handles observability, mTLS, and traffic splitting without code changes.

## 3. Distributed Transactions & Data Consistency

Since we don't have ACID across services, how do we ensure consistency?

*   **Saga Pattern:**
    *   **Choreography:** Events trigger actions in other services (decentralized).
    *   **Orchestration:** A central coordinator limits the flow (centralized).
*   **Two-Phase Commit (2PC):** Generally avoided due to blocking nature and performance impact.
*   **Event Sourcing & CQRS:** Storing state as a sequence of events and separating Read/Write models.

## 4. Resilience & Fault Tolerance

How do we prevent cascading failures?

*   **Circuit Breaker (Resilience4j):** Fails fast when a downstream service is down to prevent resource exhaustion.
*   **Bulkhead Pattern:** Isolating resources (thread pools) so one failing service doesn't take down the entire system.
*   **Retry & Exponential Backoff:** Handling transient failures.
*   **Rate Limiting:** Protecting services from being overwhelmed.

## 5. Observability & Monitoring

Debugging distributed systems is hard. You need the "Three Pillars of Observability":

*   **Logging:** Centralized logging (ELK Stack - Elasticsearch, Logstash, Kibana) with Correlation IDs to trace requests across services.
*   **Metrics:** Monitoring health, latency, throughput (Prometheus, Grafana, Micrometer).
*   **Tracing:** Distributed tracing to visualize the request path (Zipkin, Jaeger, Spring Cloud Sleuth/Micrometer Tracing).

## 6. Security

*   **OAuth2 & OIDC:** Standard for authorization and authentication.
*   **JWT (JSON Web Tokens):** Stateless authentication token passed between services.
*   **mTLS:** Mutual TLS for securing service-to-service communication.

## 7. Deployment & Infrastructure

*   **Containerization:** Docker to package applications with dependencies.
*   **Orchestration:** Kubernetes (K8s) for managing containers, scaling, and self-healing.
*   **CI/CD:** Automated pipelines for testing and deployment (Jenkins, GitLab CI, GitHub Actions).
*   **Config Management:** Externalized configuration (Spring Cloud Config, Consul, K8s ConfigMaps).

## 8. Common Interview Scenarios

**Q: How do you handle a scenario where one service is slow?**
*   *Answer:* Implement timeouts, circuit breakers, and ensure asynchronous communication where possible to avoid blocking threads.

**Q: How do you migrate a Monolith to Microservices?**
*   *Answer:* Strangler Fig Pattern – gradually replace specific functionality with new microservices until the monolith is gone.

**Q: How do you handle authentication in Microservices?**
*   *Answer:* Centralized Identity Provider (IdP) issues JWTs. The API Gateway validates specific claims, and services can trust the token signature or re-validate.

**Q: What is the difference between Orchestration and Choreography in Sagas?**
*   *Answer:* Orchestration has a central "conductor" telling services what to do (easier to manage, tighter coupling). Choreography relies on services listening to events (looser coupling, harder to track flow).

**Q: Idempotency in Microservices?**
*   *Answer:* Essential for retries. Operations should produce the same result if executed multiple times (e.g., using a unique request ID to check if an operation was already processed).

