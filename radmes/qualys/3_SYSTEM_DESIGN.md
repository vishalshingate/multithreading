# Qualys Interview Guide - Part 3: Backend & System Design

This document focuses on the Low-Level Design (LLD) and High-Level Design (HLD) concepts heavily asked in Qualys interviews.

---

## 1. LLD Questions (Very Common)

### A. Design Coffee Vending Machine
**Problem:** Model a vending machine.
**Key Components:**
*   **Enum:** `CoffeeType` (Espresso, Latte, Cappuccino). `PaymentType` (Card, Cash).
*   **State Pattern:** `VendingMachineState` (Idle, Selecting, Dispensing, Refunding).
*   **Interfaces:** `VendingMachine` (selectItem(), insertMoney(), dispense()).
*   **Logic:**
    1.  User Selects Item -> State changes to 'Selecting'.
    2.  User Inserts Coin -> Check sufficient Amount.
    3.  If Sufficient -> Dispense -> Update Inventory -> Return Change -> State to 'Idle'.
    4.  If Insufficient -> Refund -> State to 'Idle'.

### B. Design Rate Limiter
**Problem:** Prevent abuse of API by limiting requests (e.g., 100 req/min per user).
**Algorithms:**
1.  **Token Bucket:** Bucket holds N tokens. Request takes 1. Add tokens at fixed rate.
2.  **Leaky Bucket:** Queue with constant outflow rate. Input rate varies. Drop if queue full.
3.  **Fixed Window Counter:** Count requests in `[12:00, 12:01]`. Reset at `12:01`. Burst issues at boundary.
4.  **Sliding Window Log:** Store timestamps and count valid ones. High memory.
**Implementation:** Use Redis (INCR + EXPIRE).

**Spring Cloud Gateway Internal Implementation:**
*   Uses **RedisRateLimiter** with the **Token Bucket Algorithm**.
*   **Lua Script:** Executes logic atomically inside Redis to avoid race conditions.
*   **Keys Used:**
    *   `request_rate_limiter.{id}.tokens`: Number of tokens remaining.
    *   `request_rate_limiter.{id}.timestamp`: Last time the bucket was refilled.
*   **Logic:**
    1.  Calculates elapsed time since last request.
    2.  Refills tokens based on `replenishRate` * elapsed time.
    3.  Caps tokens at `burstCapacity`.
    4.  If tokens >= 1, decrement and allow request. Else, reject (HTTP 429).

### C. Design Logging System / Distributed Log Collector
**Problem:** Collect logs from multiple services effectively.
**Architecture:**
*   **Producers:** Applications write loops to files/console.
*   **Agent (Log Shipper):** Filebeat / Fluentd running on each host reads logs and sends to buffer.
*   **Buffer:** Kafka (Message Queue) to decouple heavy write load.
*   **Indexer:** Logstash consumes from Kafka, parses logs, and indexes into Elasticsearch.
*   **Storage/Search:** Elasticsearch (Search Engine).
*   **Visualization:** Kibana.
**Key Need:** Async logging (don't block main thread). Use Correlation ID for tracing.

### D. Design URL Shortener
**Problem:** `tinyurl.com/xyz` -> `google.com/long/path`.
**Core:** Unique ID Generation.
*   **Base62 Encoding:** Convert Database primary key (ID) to Base62 (a-z, A-Z, 0-9).
    *   ID `100` -> `bM`.
*   **Collision:** Database ensures uniqueness.
*   **Cleanup:** Delete expired URLs via background job.

---

## 2. HLD Questions (Security & Scale Focus for Qualys)

### A. Design Scalable Vulnerability Scanning System
**Requirement:** Scan millions of IPs for vulnerabilities.
**Architecture:**
1.  **Job Scheduler (Manager):** Receives scan request (IP range). Breaks it into smaller chunks (Jobs).
2.  **Message Queue (Kafka/RabbitMQ):** Pushes job chunks. Decouples producer/consumer.
3.  **worker Nodes (Scanners):** Scale horizontally. Pick jobs from MQ. Run scan logic (port scanning, signature matching).
4.  **Result Aggregator:** Workers push results to a database (Cassandra/S3).
5.  **Status Check:** Redis stores `JobID -> Status (Pending, Running, Completed)`.

**Challenges:**
*   **Network Limits:** Don't DDoS the target. Rate limit the scanner.
*   **Failures:** If worker dies, message returns to MQ (Ack mechanism).
*   **Data Volume:** Results are huge. Store raw XML/JSON in S3, metadata in DB.

### B. How to Design High-Throughput API System
**Techniques:**
1.  **Load Balancing:** Nginx/HAProxy to distribute traffic. Round Robin / Least Connections.
2.  **Caching:** Redis for frequently accessed data. CDN for static assets.
3.  **Asynchronous Processing:** Don't process everything synchronously. Move heavy tasks to queues.
4.  **Database Optimization:** Read Replicas, Sharding, Indexing.
5.  **Compression:** Gzip response.
6.  **Connection Pooling:** Reuse DB connections.

### C. How to Handle Millions of Scan Results
**Storage Strategy:**
*   **Relational DB (PostgreSQL):** Good for metadata (Job ID, User, Time). Not good for massive blob storage.
*   **NoSQL (Cassandra/DynamoDB):** Good for high write throughput. Storing findings per IP.
*   **Object Storage (S3):** Best for storing the raw comprehensive report (PDF/XML). Lower cost.
**Processing:** Use Stream Processing (Kafka Stream / Flink) to aggregate results in real-time.

### D. Monolith to Microservices Migration Strategy
**Pattern:** **Strangler Fig Pattern**.
*   **Concept:** Like a vine strangling a tree, you gradually replace specific pieces of functionality with new microservices until the old monolith is gone.
*   **Steps:**
    1.  **Identify Edges:** Find a non-critical module (e.g., Notification) to extract first.
    2.  **Facade/Proxy:** Place an API Gateway in front of the Monolith.
    3.  **Route Traffic:** Direct traffic for the new module to the new Microservice. Direct everything else to the legacy Monolith.
    4.  **Repeat:** Continue extracting modules until the Monolith is empty.
