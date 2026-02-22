# Qualys Interview Guide - Part 7: Resume & Experience

This document helps structure your answers for experience-based questions in the 3rd and Managing Round.

---

## 1. Explain Your System Architecture

**Method:** Start High-Level -> Zoom In -> Explain Data Flow.

1.  **Overview:** "I worked on the [Project Name], a microservices-based platform for handling [Core Business Function]."
2.  **Tech Stack:** "Backend: JAVA Spring Boot, DB: PostgreSQL + Redis, Messaging: Kafka, Deployment: Kubernetes on AWS."
3.  **Core Components:**
    *   **API Gateway:** Handles authentication (JWT) and routing.
    *   **Core Services:** Explain 2-3 main services (e.g., User Service, Order Service).
    *   **Async Processing:** Explain where Kafka is used (e.g., Notification, Report Generation).
    *   **Data Storage:** Explain why SQL/NoSQL was chosen.
4.  **Flow:** "When a user initiates X, the request hits Gateway -> Service A validates -> Publishes Event to Kafka -> Service B consumes and updates DB."

---

## 2. Biggest Scaling Challenge / Performance Optimization

**Situation:** "During a [Specific Event/Load Test], our [API/Database] started timing out."
**Target:** "We needed to support [Increase %] more traffic."
**Action:**
1.  **Identified Bottleneck:** Used APM (New Relic / Datadog) to find slow SQL queries or high CPU usage.
2.  **Database Optimization:** Added composite index on columns used in WHERE clause. Partitioned large tables.
3.  **Caching:** Introduced Redis to cache frequent read operations (e.g., User Configuration), reducing DB load by 40%.
4.  **Async Processing:** Moved heavy synchronous tasks (PDF generation) to a background worker using Kafka.
**Result:** "Latency dropped from 2s to 200ms. Throughput increased 3x."

---

## 3. Production Issue You Handled (Critical)

**Situation:** "On a Friday evening, customers reported that they couldn't [Key Action]."
**Investigation:**
1.  **Logs:** Checked Kibana. Found `OutOfMemoryError` or `ConnectionTimeoutException`.
2.  **Thread Dump:** Analyzed thread dump and found a Deadlock or threads waiting on a database lock.
3.  **Root Cause:** A specific long-running query was locking the table, causing other requests to pile up and exhaust the connection pool.
**Fix:**
1.  **Immediate:** Killed the long-running query session. Restarted the service.
2.  **Permanent:** Rewrote the query to fetch data in batches. Added a timeout to the transaction.
**Learning:** Implemented better monitoring and alerts for long-running queries.

---

## 4. How to Debug Memory Leak in Production

**Process:**
1.  **Monitoring:** Notice heap memory usage constantly increasing without dropping after GC (Sawtooth pattern becomes a rising line).
2.  **Heap Dump:** Trigger a Heap Dump using `jmap` or Actuator Endpoint.
3.  **Analysis:** Open the dump in Eclipse MAT (Memory Analyzer Tool) or VisualVM.
4.  **Identify Leak:** Look for "Dominator Tree". Find objects that are retaining the most memory (e.g., a static `HashMap` storing user sessions that never expire).
5.  **Fix:** Clear the collection when not needed or use `WeakReference` / `SoftReference`.

---

## 5. How to Optimize Slow DB Query

1.  **Analyze Execution Plan:** Use `EXPLAIN ANALYZE` (Postgres) or `EXPLAIN` (MySQL).
    *   Check if it's doing a **Full Table Scan** (Bad) or **Index Scan** (Good).
2.  **Indexing:** Add missing indexes on filter columns.
3.  **Selectivity:** Avoid `SELECT *`. Fetch only needed columns.
4.  **N+1 Problem:** Check if the application is firing 1 query per row. Use `JOIN` or `Batch Fetching`.
5.  **Data Archival:** Move old historical data to a separate archive table to keep the active table small.

