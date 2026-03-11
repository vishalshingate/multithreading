# Rapid7 Interview Guide - Hiring Manager & Technical Round

This guide is tailored for the **Senior Software Engineer** role at Rapid7, focusing on Scalability, Resilience, Security, and System Design.

---

## 1️⃣ Architecture

### How do you design scalable microservices?
**Key Principles:**
1.  **Decomposition:** Break down by business capability (e.g., User Service, Scan Engine, Reporting Service) to ensure loose coupling.
2.  **Database per Service:** Each microservice owns its data to prevent tight coupling at the DB layer.
3.  **Statelessness:** Services should be stateless to allow horizontal scaling (spinning up more instances behind a Load Balancer).
4.  **Asynchronous Communication:** Use Event-Driven Architecture (Kafka/RabbitMQ) for inter-service communication to decouple critical paths (e.g., Scan Request -> Queue -> Scan Engine).
5.  **Caching:** Implement multi-level caching (CDN, API Gateway, Redis) to reduce load on the database.
6.  **Observability:** Centralized Logging (ELK), Metrics (Prometheus/Grafana), and Distributed Tracing (Zipkin/Jaeger).

### How do you handle service failures?
1.  **Retries with Exponential Backoff:** For transient failures (network blips).
2.  **Circuit Breaker (Resilience4j):** Fail fast if a downstream service is down to prevent resource exhaustion.
3.  **Fallback Mechanisms:** Return cached data or a default response instead of throwing an error.
4.  **Dead Letter Queues (DLQ):** For async messaging, move failed messages to a DLQ for later analysis/replay.
5.  **Idempotency:** Ensure retrying a request doesn't cause side effects (e.g., charging a user twice).

### Explain Circuit Breaker pattern.
*   **Concept:** Prevents an application from repeatedly trying to execute an operation that's likely to fail.
*   **States:**
    *   **CLOSED:** Normal operation. Requests pass through.
    *   **OPEN:** Error threshold exceeded. Requests are blocked immediately (fail fast) without calling the downstream service.
    *   **HALF-OPEN:** After a timeout, allow a limited number of requests to test if the service has recovered. If successful -> CLOSED; if failed -> OPEN.
*   **Tools:** Resilience4j, Hystrix (deprecated).

### Explain Event-Driven Architecture.
*   **Core Idea:** Services communicate by emitting "events" (facts that happened) rather than calling each other directly.
*   **Components:** Producer (emits event), Broker (Kafka/RabbitMQ), Consumer (reacts to event).
*   **Benefits:** Decoupling, Scalability, Asynchronous processing.
*   **Example:** User waits for a report -> `ReportRequested` event -> Report Service picks it up -> Generates PDF -> Emits `ReportGenerated` -> Notification Service sends email.

### When do you use Synchronous vs Asynchronous communication?
*   **Synchronous (REST/gRPC):**
    *   Real-time response required (e.g., User Login, Search).
    *   Simple internal calls where latency is low.
    *   **Downside:** Blocking, tight coupling using HTTP.
*   **Asynchronous (Kafka/RabbitMQ):**
    *   Long-running tasks (e.g., Generating a vulnerability scan report).
    *   Decoupling systems (e.g., Order Placed -> Inventory Updated, Email Sent).
    *   Handling traffic spikes (Queue buffers the load).

---

## 2️⃣ System Design

### Design a Log Processing System (Like ELK/Splunk)
*   **Ingestion:** Agents (Filebeat/Fluentd) collect logs from servers.
*   **Buffering:** **Kafka** to handle high throughput and spikes.
*   **Processing:** **Logstash/Flink** to parse, filter, and enrich logs (e.g., add GeoIP).
*   **Storage:** **Elasticsearch** (Inverted Index) for fast text search. **S3/HDFS** for long-term cold storage.
*   **Visualization:** **Kibana/Grafana** for dashboards.
*   **Challenges:** High write volume, indexing latency, retention policies.

### Design Rate Limiter for APIs
*   **Placement:** API Gateway (Zuul/Kong) or Sidecar (Envoy).
*   **Algorithm:** **Token Bucket** (allows bursts) or **Leaky Bucket** (smooth rate).
*   **Storage:** **Redis** (Atomic counters, `INCR`, `EXPIRE`).
*   **Scalability:** Use Redis Cluster. Handle race conditions with Lua scripts.
*   **Response:** `HTTP 429 Too Many Requests`. Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`.

### Design URL Shortener (TinyURL)
*   **Core:** Map long URL <-> Short Key.
*   **Algorithm:** **Base62 Encoding** (A-Z, a-z, 0-9). 
*   **ID Generation:** Unique ID generator (Snowflake or Database Sequence/Auto-Increment with offset) to feed into Base62.
*   **Database:** **NoSQL (DynamoDB/Cassandra)** for high read/write scalability. Key-Value store.
*   **Caching:** **Redis** (LRU eviction) for popular links (80/20 rule).
*   **Redirection:** `HTTP 301` (Permanent) or `302` (Temporary - useful for analytics).

### Design Notification System
*   **API:** `POST /send` (User ID, Channel, Content).
*   **Queue:** **Kafka** topics per channel (`email-topic`, `sms-topic`, `push-topic`) to isolate failures.
*   **Workers:** Stateless consumers processing messages and calling 3rd party providers (SendGrid, Twilio, FCM).
*   **DB:** Store notification status (Created, Sent, Failed).
*   **Retry:** Exponential backoff for 3rd party failures. DLQ for permanent failures.
*   **Rate Limiting:** Protect users from spam.

### Design Distributed Caching System
*   **Protocol:** Memcached or Redis.
*   **Sharding:** Consistent Hashing (Ring) to distribute keys across nodes and handle node addition/removal with minimal remapping.
*   **Replication:** Master-Slave replication for high availability.
*   **Invalidation:** TTL (Time-To-Live), LRU (Least Recently Used) eviction, Write-Through/Write-Behind patterns.

---

## 3️⃣ Backend

### Difference between Thread and Process.
*   **Process:** Independent execution unit with its own memory space (Heap, Stack, Code). Heavyweight. Context switching is expensive. OS level.
*   **Thread:** Lightweight unit *within* a process. Shares memory (Heap) but has its own Stack. Context switching is faster.

### How does Spring Boot Auto Configuration work?
*   **@EnableAutoConfiguration:** Triggers the process.
*   **Scanning:** Scans classpath for `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (in newer versions) or `spring.factories`.
*   **Conditions:** Uses `@Conditional` annotations:
    *   `@ConditionalOnClass`: If a class (e.g., `DataSource`) is on the classpath.
    *   `@ConditionalOnMissingBean`: If the user hasn't defined their own bean.
    *   `@ConditionalOnProperty`: If a property is set in `application.properties`.

### Explain Spring Bean Lifecycle.
1.  **Instantiation:** Constructor is called.
2.  **Populate Properties:** Dependency Injection.
3.  **Aware Interfaces:** `BeanNameAware`, `BeanFactoryAware`.
4.  **Pre-Initialization:** `BeanPostProcessor.postProcessBeforeInitialization()`.
5.  **Initialization:** `@PostConstruct`, `InitializingBean.afterPropertiesSet()`, custom `init-method`.
6.  **Post-Initialization:** `BeanPostProcessor.postProcessAfterInitialization()` (AOP proxies created here).
7.  **Destruction:** `@PreDestroy`, `DisposableBean`.

### Difference between @Component, @Service, @Repository.
*   **@Component:** Generic stereotype for any Spring-managed component.
*   **@Service:** Specialization of `@Component`. Indicates business logic layer. No extra behavior by default, but good for AOP (Transactions).
*   **@Repository:** Specialization of `@Component`. Indicates Data Access Layer. **Translates database specific exceptions** (SQLExceptions) into Spring's unchecked `DataAccessException` hierarchy.

---

## 4️⃣ Databases

### How do you optimize slow queries?
1.  **Identify:** Use `Slow Query Log` or APM tools (New Relic).
2.  **Analyze:** Run `EXPLAIN ANALYZE` to check execution plan. Look for **Full Table Scans**.
3.  **Index:** Add indexes on columns in `WHERE`, `JOIN`, and `ORDER BY` clauses. Use Composite Indexes efficiently (leftmost prefix rule).
4.  **Refactor:**
    *   Avoid `SELECT *`.
    *   Pagination (don't fetch 1M rows).
    *   Avoid N+1 query problems (use `JOIN fetch` or `EntityGraph`).
5.  **Caching:** Cache result if data doesn't change often.

### Explain Database Indexing.
*   **Structure:** Typically **B-Tree** (Balanced Tree) or **B+Tree**.
*   **Mechanism:** Sorts data pointers to allow `O(log N)` lookup instead of `O(N)`.
*   **Types:**
    *   **Primary Index:** Clustered index (Leaf nodes contain actual data).
    *   **Secondary Index:** Points to Primary Key.
    *   **Composite Index:** Multi-column index.
*   **Trade-off:** Speeds up READs, slows down WRITEs (INSERT/UPDATE/DELETE) because index must be updated.

---

## 5️⃣ Real Experience (Tailored for Vishal)

### Tell me about a major production incident.
"In my current project, we had a **Cache Stampede** incident.
*   **Scenario:** A critical configuration cache key in Redis expired exactly when traffic spiked.
*   **Impact:** Thousands of concurrent requests missed the cache and hit the Database simultaneously. DB CPU went to 100%, causing a cascading failure.
*   **Fix (Immediate):** Restarted services with staggered startup.
*   **Fix (Permanent):** Implemented **Probabilistic Early Expiration** (or Locking) to ensure only one thread refreshes the cache while others serve stale data or wait."

### Tell me about a technical disagreement with the team.
"We were debating between **Choreography vs Orchestration** for a complex Saga pattern in our Order processing.
*   **My View:** I advocated for **Orchestration** (using a central coordinator) because the workflow was complex and hard to track with pure event choreography.
*   **Team View:** They wanted Choreography to avoid a central bottleneck.
*   **Resolution:** I built a POC comparing observability. We realized that with Choreography, debugging failures was a nightmare. The team agreed to Orchestration for this specific complex flow, while keeping simple flows Choreographed."

### How do you mentor engineers?
"I believe in **'Show, Don't Just Tell'**.
*   **Code Reviews:** I don't just say 'fix this'. I explain *why* (e.g., 'This might cause a memory leak because...').
*   **Pair Programming:** I spend time pair programming on complex tasks.
*   **Design Docs:** I ask them to write mini-design docs (RFCs) before coding to structure their thoughts.
*   **Tech Talks:** I encourage them to present on topics they learned."

### How do you review code?
1.  **Correctness:** Does it meet requirements and handle edge cases?
2.  **Performance:** loop-inside-loop, N+1 queries, unnecessary object creation.
3.  **Security:** SQL injection, XSS, exposed secrets.
4.  **Readability:** Meaningful names, small methods, comments on *why* not *what*.
5.  **Test Coverage:** Are there unit tests covering the new logic?

---

## 6️⃣ System Design Questions Kamlesh May Ask

### Very likely:

1.  **Design a Rate Limiter:** (See Section 2). Focus on distributed environment (Redis).
2.  **Design Log Monitoring System:** (See Section 2). Focus on Kafka buffering and ElasticSearch indexing.
3.  **Design Distributed Cache:** Focus on Consistent Hashing and Cache Coherence protocols.
4.  **Design Notification System:** Focus on reliability (never lose a message) using persistent queues.
5.  **Design API Gateway:**
    *   **Functions:** Routing, Auth, Rate Limiting, SSL Termination, Logging.
    *   **Tech:** Netflix Zuul, Spring Cloud Gateway, Nginx.

---

## 7️⃣ Rapid7 Interview Questions Asked Previously

### 1. Explain Kafka internals.
*   **Topic:** Logical category.
*   **Partition:** Physical split of a topic for scaling. Ordered sequence of messages.
*   **Offset:** Unique ID of a message in a partition.
*   **Broker:** Kafka server.
*   **Zookeeper/KRaft:** Manages cluster metadata (controller election, topic config).
*   **ISR (In-Sync Replicas):** Replicas that are caught up with the leader. Ensures durability.

### 2. Design log ingestion pipeline.
*   **Source:** Beats/Fluentd.
*   **Buffer:** Kafka (to decouple speed of producers vs consumers).
*   **Processor:** Logstash/Spark Streaming (filtering, masking PII data).
*   **Sink:** Elasticsearch / S3.

### 3. Explain Event Driven Architecture.
(See Section 1).

### 4. How would you design a vulnerability scanning system? (Product Specific)
*   **Targets:** IP ranges, URLs.
*   **Scheduler:** Trigger scans (Cron/Quartz).
*   **Scan Engine (Worker):**
    *   Stateless workers pick jobs from a Queue.
    *   Performs checks (Port scanning, Signature matching).
*   **Result Processor:**
    *   Parses raw results.
    *   De-duplicates vulnerabilities.
    *   Stores in DB (Postgres) and Index (Elasticsearch).
*   **Alerting:** Triggers notifications for Critical vulns.

### 5. How do you handle high traffic APIs?
*   **Async Processing:** Offload heavy work to queues.
*   **Caching:** CDN for static, Redis for dynamic content.
*   **Scale Out:** Auto-scaling groups (K8s HPA).
*   **Database:** Read Replicas, Sharding.
*   **Backpressure:** Reject requests if system is overloaded (Load Shedding).

### 6. Explain backpressure in Kafka.
*   **Consumer Side:** If the consumer is slow, it processes messages slower than the producer sends. Kafka handles this naturally because it's **Pull-based**. The consumer pulls only what it can handle.
*   **Monitoring:** Monitor **Consumer Lag** (difference between High watermark and Consumer Offset). If high, scale up consumers (up to # of partitions).

