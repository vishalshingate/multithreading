# Qualys Techno Manager Round Preparation Guide

This guide is tailored for the **Senior Software Engineer** role at Qualys, focusing on high-level architecture discussions, deep technical questions, and behavioral/managerial scenarios.

---

## 1️⃣ Project Deep-Dive (CRITICAL)

The manager will start here. You must explain your current project clearly and confidently.

### A. Architecture Explanation (Template)
**"I am currently working on [Project Name], which is a Microservices-based platform designed to handle [Core Business Function, e.g., High-volume payment processing or Real-time data analytics]."**

**High-Level Components:**
1.  **Entry Point:** "We use an **API Gateway (Zuul/Spring Cloud Gateway)** for routing, authentication (OAUTH2/JWT), and rate limiting."
2.  **Core Services:** "The backend consists of **Spring Boot Microservices** communicating via **REST** (synchronous) and **Kafka** (asynchronous for decoupling)."
3.  **Data Layer:** "We use **PostgreSQL** for transactional data and **Redis** for caching frequent read operations to reduce DB load."
4.  **Security:** "We implemented **Spring Security with OAuth2**."
5.  **Deployment:** "Services are containerized using **Docker** and orchestrated via **Kubernetes (EKS/AKS)** with a CI/CD pipeline (Jenkins/GitLab)."

### B. Your Responsibility
*   "I owned the **[Module Name, e.g., Notification Service]** end-to-end, from design to deployment."
*   "I was responsible for optimizing API response times, reducing latency from **500ms to 200ms** by implementing caching."
*   "I mentored junior developers and conducted code reviews to ensure code quality and adherence to **SOLID principles**."

### C. Challenges & Solutions (STAR Method)
**Challenge:** "Handling sudden traffic spikes during [Event] caused our database CPU to hit 100%."
**Solution:** "I introduced **Redis Caching** for read-heavy endpoints and implemented **Database Read Replicas** to distribute the load."
**Result:** "The system remained stable with **99.9% uptime** during peak load."

---

## 2️⃣ System Design Scenarios

### A. Design a Rate Limiter API
**Goal:** Prevent abuse.
*   **Algorithm:** Token Bucket or Leaky Bucket.
*   **Storage:** Redis (Atomic counters with expiration).
*   **Filter:** Implement as a filter in API Gateway.
*   **Headers:** Return `X-RateLimit-Remaining` and `429 Too Many Requests`.

### B. Design a Notification Service
**Goal:** Send Emails/SMS asynchronously.
*   **API:** `POST /send-email` (Accepts request, validates, pushes to Queue).
*   **Queue:** **Kafka/RabbitMQ** (Ensures no message is lost if email provider is down).
*   **Consumer:** Worker service reads from Queue -> Calls 3rd Party API (SendGrid/Twilio).
*   **Retry:** If failure, push to **DLQ (Dead Letter Queue)** for manual retry/alerting.

### C. Design URL Shortener
**Goal:** Shorten long URLs.
*   **Algorithm:** Base62 Encoding (Maps Database ID -> Short String).
*   **DB:** NoSQL (Cassandra/DynamoDB) for high write throughput, or RDBMS with sharding.
*   **Collision:** Pre-generate keys (KGS - Key Generation Service) to avoid runtime collisions.

---

## 3️⃣ Java & Spring Boot Deep Dive

### Java Internals
1.  **HashMap vs ConcurrentHashMap:**
    *   **HashMap:** Not thread-safe. Uses array of buckets (Node<K,V>). In Java 8+, converts LinkedList to Red-Black Tree if bucket size > 8.
    *   **ConcurrentHashMap:** Thread-safe. Uses **CAS (Compare-And-Swap)** and **synchronized** blocks on *specific buckets* (Segment locking in older definitions, now bucket-level locking). Does *not* lock the whole map. Reads are wait-free.
2.  **String intern():** Moves the String object to the **String Constant Pool** (Heap area) to save memory.
3.  **Garbage Collection:**
    *   **G1GC:** Default in modern Java. Splits heap into regions. Cleanups primarily in Eden space (Minor GC).
    *   **Stop-the-world:** Pause application threads to mark/sweep live objects.

### Spring Boot Internals
1.  **Auto Configuration:**
    *   Uses `@EnableAutoConfiguration`.
    *   Scans `META-INF/spring.factories` (or generic `imports` file in newer versions).
    *   Uses `@ConditionalOnClass`, `@ConditionalOnMissingBean` to load beans *only if* libraries are present and user hasn't defined their own.
2.  **Bean Lifecycle:**
    *   Instantiate -> Populate Properties -> `BeanNameAware` -> `BeanFactoryAware` -> `PostProcessBeforeInit` -> `@PostConstruct` -> `PostProcessAfterInit` -> Ready -> `@PreDestroy`.
3.  **Cyclic Dependency:** If A needs B and B needs A.
    *   **Fix:** Use `@Lazy` injection or redesign (setter injection).

---

## 4️⃣ Concurrency (Senior Level)

1.  **synchronized vs ReentrantLock:**
    *   **synchronized:** Implicit, easy to use, blocks indefinitely.
    *   **ReentrantLock:** Explicit, allows `tryLock()` (timeout), fair locking, multiple conditions.
2.  **ThreadLocal:**
    *   Variables local to a thread. Each thread has its own copy.
    *   **Usage:** Holding UserContext/TransactionID per request.
    *   **Leak:** Must call `remove()` in a `finally` block (especially in thread pools) to prevent memory leaks (Tomcat threads are reused!).
3.  **ExecutorService:**
    *   Decouples task submission from execution.
    *   **FixedThreadPool:** Stable for predictable load.
    *   **CachedThreadPool:** For bursty, short tasks.

---

## 5️⃣ Database & Performance

1.  **Indexing:** B-Tree structure. Speeds up `SELECT`, slows down `INSERT/UPDATE`.
    *   **Composite Index:** Order matters (`WHERE a=1 AND b=2`).
2.  **Memory Leak Debugging:**
    *   **Symptoms:** Frequent Full GC, `OutOfMemoryError`.
    *   **Tools:** **VisualVM**, **Eclipse MAT**, **JProfiler**.
    *   **Process:** Take Heap Dump -> Analyze Dominator Tree -> Find large objects (e.g., static Maps, unclosed connections).
3.  **Reduce DB Load:**
    *   **Caching (Redis)**.
    *   **Read Replicas** (Segregate Read/Write traffic).
    *   **Batch Processing** (Hibernate Batch Insert).
    *   **Database Indexing**.

---

## 6️⃣ Microservices & Distributed Systems

1.  **Communication:**
    *   **Sync:** REST/GraphQL (Simple, tight coupling).
    *   **Async:** Kafka/RabbitMQ (Decoupled, eventual consistency).
2.  **Service Discovery:**
    *   **Eureka/Consul/K8s DNS:** Service registers itself. Client queries registry to get IP:Port.
3.  **Circuit Breaker (Resilience4j):**
    *   Prevents cascading failures.
    *   **States:** Closed (Normal) -> Open (Fails fast immediately) -> Half-Open (Test if service is back).

---

## 7️⃣ Managerial & Ownership Questions

1.  **Code Review:**
    *   "I look for **Readability, Maintainability, Security checks, and Test Coverage**."
    *   "I ensure no credentials are hardcoded and variable names are meaningful."
2.  **Production Incident:**
    *   **Step 1:** Mitigation (Rollback or Restart) to restore service.
    *   **Step 2:** RCA (Root Cause Analysis). Check logs, metrics.
    *   **Step 3:** Fix & Prevention (Add unit tests, add alerts).
3.  **Difficult Bug:**
    *   *Example:* "A `NullPointerException` that only happened in Prod under load. Turned out to be a race condition in a non-thread-safe SimpleDateFormat. Fixed by using `ThreadLocal` or switching to `java.time`."

---

## 8️⃣ Why Qualys?

1.  **Product Focus:** "Qualys is a leader in **Cloud Security and Vulnerability Management**. I want to work on critical, high-scale security products."
2.  **Growth:** "I admire Qualys' engineering culture and the challenges of processing petabytes of security data."
3.  **30-60-90 Day Plan:**
    *   **30 Days:** Understand architecture, code base, and deployment pipeline. Fix minor bugs.
    *   **60 Days:** Take ownership of a module/feature. Collaborate with other teams.
    *   **90 Days:** Propose optimizations, contribute to system design, and mentor juniors.

