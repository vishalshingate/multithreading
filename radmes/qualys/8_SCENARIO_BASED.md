# Qualys Interview Guide - Part 8: Scenario-Based Questions

This document covers typical behavioral and situational technical questions asked in Qualys interviews.

---

## 1. How to Debug Memory Leak in Production

**Scenario:** "Your service is crashing with `java.lang.OutOfMemoryError: Java heap space` every few days. How do you find the root cause?"

**Step-by-Step Approach:**
1.  **Analyze Metrics:** Check JVM Heap Usage graph in Grafana/New Relic. Does it look like a "sawtooth" pattern (healthy GC) or a steadily rising staircase (leak)?
2.  **Enable Heap Dump on OOM:** Configure JVM flag `-XX:+HeapDumpOnOutOfMemoryError` to automatically capture a dump when it crashes.
3.  **Analyze Heap Dump:**
    *   Load the `.hprof` file into **Eclipse MAT (Memory Analyzer Tool)**.
    *   Run the **"Leak Suspects Report"**.
    *   Look at the **Dominator Tree** to see which objects are retaining the most memory.
4.  **Common Culprits:**
    *   **Static Collections:** A `static Map` that keeps growing and never gets cleared.
    *   **Unclosed Resources:** Database connections or Streams not closed in `finally` block.
    *   **Thread Locals:** Data stored in ThreadLocal variables not cleaned up when thread returns to pool.
    *   **Listeners/Callbacks:** Registering a listener but never unregistering it.

---

## 2. How to Design Secure API

**Scenario:** "You are designing an API that exposes sensitive vulnerability data. How do you secure it?"

**Security Layers:**
1.  **Transport Layer (HTTPS):** Enforce TLS 1.2+ to encrypt data in transit. Use HSTS header.
2.  **Authentication (Who are you?):**
    *   **OAuth 2.0 / OIDC:** Standard protocol. Client exchanges credentials for an Access Token.
    *   **JWT (JSON Web Token):** Stateless token containing user identity. Verify signature on every request.
    *   **mTLS (Mutual TLS):** For service-to-service communication, both client and server present certificates.
3.  **Authorization (What can you do?):**
    *   **RBAC (Role-Based Access Control):** Check if user has `ROLE_ADMIN` or `ROLE_VIEWER`.
    *   **Scope Checks:** Verify strict permissions (e.g., `scope: vulnerability.read`).
4.  **Input Validation:**
    *   **Sanitize Inputs:** Prevent SQL Injection and XSS (Cross-Site Scripting). Validate types/formats strictly.
5.  **Rate Limiting:** Protect against DDoS and Brute Force attacks (Token Bucket algorithm).
6.  **Audit Logging:** Log *who* accessed *what* and *when* (crucial for security compliance).

---

## 3. How to Handle Large Scale Logs

**Scenario:** "Your application generates 1TB of logs per day. How do you manage, store, and search them effectively?"

1.  **Asynchronous Logging:** Application code should just push logs to a buffer (e.g., Kafka) and return immediately. Don't write directly to file/disk in the request path.
2.  **Log Aggregation Pipeline (ELK Stack):**
    *   **Filebeat:** Lightweight shipper on each server.
    *   **Kafka:** Buffer to handle burst traffic.
    *   **Logstash:** Filter, parse, and transform logs (e.g., mask credit card numbers).
    *   **Elasticsearch:** Index and store logs for fast search.
3.  **Storage Costs:**
    *   **Retention Policy:** Keep hot logs (last 7 days) in SSD-backed indices for fast search.
    *   **Rollover:** Move older logs (7–30 days) to HDD-backed "Warm" nodes.
    *   **Archive:** Move very old logs (>30 days) to "Cold" storage or S3 (Glacier) for compliance. Delete from index.
4.  **Sampling:** If volume is too high (e.g., debug logs), implement probabilistic sampling (keep 10% of success logs, 100% of error logs).

---

## 4. How to Optimize Slow DB Queries

**Scenario:** "A critical API endpoint is taking 5 seconds to respond. You suspect a slow database query. What do you do?"

1.  **Identify the Query:**
    *   Check APM tools (New Relic) or DB Slow Query Log (`long_query_time = 1s`).
2.  **Analyze Execution Plan:** Run `EXPLAIN ANALYZE` on the query.
    *   **Full Table Scan?** The DB is reading every row. **Bad.**
    *   **Index Scan?** The DB uses an index. **Good.**
    *   **Filesort?** Sorting is happening on disk. **Slow.**
3.  **Fixes:**
    *   **Add Index:** Create an index on columns used in `WHERE`, `JOIN`, and `ORDER BY`.
    *   **Composite Index:** If filtering by multiple columns (`WHERE status='ACTIVE' AND type='USER'`), a composite index `(status, type)` is faster than two separate indexes.
    *   **Covering Index:** Include selected columns in the index itself so the DB doesn't need to look up the table heap.
    *   **Rewrite Query:**
        *   Avoid `SELECT *`. Select specific columns.
        *   Avoid `OR` on non-indexed columns.
        *   Use `LIMIT` if you don't need all rows.
    *   **Partitioning:** If the table is huge (billions of rows), partition it by Date or Region.

