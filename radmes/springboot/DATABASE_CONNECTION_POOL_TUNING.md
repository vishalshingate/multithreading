# Database Connection Pool Tuning in Spring Boot (HikariCP)

Spring Boot uses **HikariCP** as the default connection pool because it is extremely fast and lightweight. Tuning it correctly is critical for production stability and performance.

## 1. The Core Philosophy
**More connections != More performance.**  
In fact, having *too many* connections often hurts performance due to context switching and disk I/O contention on the database side.

**The Golden Rule:**  
> A connection pool should be sized to maximize the resources of the **Database**, not the Client Application.

---

## 2. Key Configuration Properties
Configure these in `application.yml` or `application.properties`.

### 2.1 `maximum-pool-size` (The most critical setting)
*   **Definition:** The maximum number of connections that the pool will keep, including both idle and in-use connections.
*   **Default:** 10
*   **How to Tune:** 
    *   Do **NOT** set this to 100 or 200 just "to be safe".
    *   **PostgreSQL Formula:** `connections = ((core_count * 2) + effective_spindle_count)`
    *   *Example:* If your DB server has 4 CPUs and uses an SSD (no seeking), sizing should be `(4 * 2) + 1 = 9`. 
    *   Surprisingly, a pool size of **10-20** is often enough to handle thousands of concurrent transactions if queries are fast.
*   **YML:**
    ```yaml
    spring:
      datasource:
        hikari:
          maximum-pool-size: 20
    ```

### 2.2 `minimum-idle`
*   **Definition:** The minimum number of idle connections Hikari tries to maintain in the pool.
*   **Recommendation:** **Set this same as `maximum-pool-size`**.
*   **Why?** Fixed size pools perform better. It prevents the penalty of creating new connections during traffic spikes. If `min=5` and `max=20`, and traffic spikes, the app pauses to create 15 TCP connections.

### 2.3 `connection-timeout`
*   **Definition:** How long the client waits for a connection from the pool before throwing an exception.
*   **Default:** 30000ms (30 seconds).
*   **Recommendation:** Lower it to **2000ms - 5000ms** (2-5 seconds).
*   **Why?** If the pool is exhausted, waiting 30 seconds usually just piles up more requests and crashes the web server (Thread Starvation). It's better to fail fast.

### 2.4 `max-lifetime`
*   **Definition:** The maximum time a connection typically lives in the pool.
*   **Default:** 30 minutes.
*   **Recommendation:** Set this **shorter than your database's connection time limit** (e.g., MySQL `wait_timeout`).
*   **Rule of Thumb:** Use 1800000ms (30 mins) or whatever the infrastructure (Load Balancers/Firewalls) dictates.
*   **Note:** Hikari respects this to avoid using "stale" connections that the network has quietly dropped.

---

## 3. Real World Calculation Example

**Scenario:**
- **App:** 50 Pods of Microservices.
- **DB:** 8 Core CPU, SSD Storage.
- **Traffic:** High concurrency.

**Step 1: Calculate DB Capacity**
Using the formula `(Core * 2) + 1`:  
`Total Max Connections DB can handle efficiently = (8 * 2) + 1 = 17 connections.`  
*Wait, only 17?* Yes, for *active running queries*. Ideally, the DB wants to execute 8-16 queries in parallel effectively.

**Step 2: Allocate to Pods**
We have 50 pods.
If we give `maximum-pool-size: 10` per pod => `50 * 10 = 500` connections.
This is **way** above the optimal 17.
However, usually, not all 500 connections are *active* at the exact same millisecond.

**Compromise Strategy:**
1.  **Reduce Pod Pool Size:** Set `maximum-pool-size: 5`. (Total possible: 250).
2.  **Use Proxy (PgBouncer):** If you absolutely need hundreds of client connections (idle mostly), put **PgBouncer** in front of the DB. The pods talk to PgBouncer (1000 connections), and PgBouncer funnels them into the 17 real connections on Postgres.

---

## 4. Monitoring & Troubleshooting

### How to detect Pool Exhaustion?
Enable Actuator Metrics.
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "prometheus,metrics,health"
```

Query Prometheus/Grafana used by Hikari:
1.  `hikaricp_connections_active`: How many are currently doing work.
2.  `hikaricp_connections_pending`: **The Danger Metric**. If this is > 0, threads are blocked waiting for connections.

### Common Exceptions
1.  **`SQLTransientConnectionException: Connection is not available, request timed out after 30000ms`**
    *   **Cause:** Pool is full. Every connection is busy running a slow query.
    *   **Fix:** NOT increasing pool size immediately. **Fix the slow query first.** If queries are fast (2ms), a pool of 10 can handle 5000 req/sec. If queries take 5 seconds, a pool of 10 handles 2 req/sec.

2.  **`CommunicationsLinkFailure` (The Morning Info)**
    *   **Cause:** Connection sat idle all night, firewall killed it.
    *   **Fix:** Check `max-lifetime` < Firewall Timeout.

---

## 5. Summary Config Snippet

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20             # Max connections
      minimum-idle: 20                  # Fixed pool size preferred
      connection-timeout: 3000          # Fail fast (3 sec) if pool empty
      idle-timeout: 600000             # 10 mins (only relevant if min < max)
      max-lifetime: 1800000            # 30 mins (refresh connection to prevent stale)
      pool-name: MyHikariCP            # Good for logging/metrics
```
