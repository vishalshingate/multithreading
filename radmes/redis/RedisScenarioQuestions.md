# Redis Cache Scenario-Based Interview Questions (Spring Boot, 5+ Years Experience)

## 1. Cache Consistency Strategies
**Scenario:** You have a Spring Boot application using the "Cache-Aside" pattern with Redis. You need to update a frequently accessed entity (e.g., `ProductPrice`).
*   **Question:** How do you ensure data consistency between the database and Redis during updates?
*   **Discussion Points:**
    *   Should you update the cache or delete the cache? (Delete is usually preferred to avoid race conditions).
    *   What happens if the DB update succeeds but the cache deletion fails? (Dual-write problem).
    *   Solutions: Retry mechanisms, Transactional outbox pattern, Eventual consistency using message queues (Kafka/RabbitMQ) to invalidate cache.
    *   Use of Spring's `@CacheEvict` vs. manual `RedisTemplate` operations.

## 2. The "Thundering Herd" Problem (Cache Stampede)
**Scenario:** A very popular cache key expires (e.g., "DailyDeals"), and thousands of concurrent requests hit the application simultaneously.
*   **Question:** How do you prevent all these requests from hitting the database at the same time?
*   **Discussion Points:**
    *   **Mutex Locking:** One thread gets a lock to rebuild the cache; others wait or return stale data.
    *   **Logical Expiry:** Set a physical TTL longer than the logical TTL. When logical TTL is passed, return stale data while asynchronously refreshing the cache.
    *   **Spring Cache Sync:** Using `@Cacheable(sync=true)` in Spring Boot (uses local synchronization, usually sufficient for single instance, but for distributed systems, need distributed locks).

## 3. Cache Penetration
**Scenario:** An attacker is sending thousands of requests for non-existent IDs (e.g., `userID=-1` or random UUIDs). These keys are never in Redis, so every request hits the database.
*   **Question:** How do you protect the database?
*   **Discussion Points:**
    *   **Cache Null Values:** Store the "null" result in Redis with a short TTL (`spring.cache.redis.cache-null-values=true`).
    *   **Bloom Filters:** Implement a Bloom Filter in front of Redis to quickly check if an ID *might* exist before even checking the cache or DB.

## 4. Distributed Locking
**Scenario:** You have a scheduled job or a critical section of code (e.g., generating a daily report or inventory reservation) running in a microservices environment with multiple instances of the service. Only one instance should execute this task.
*   **Question:** How do you implement a distributed lock using Redis?
*   **Discussion Points:**
    *   Using `SET resource_name my_random_value NX PX 30000` (Atomic set if not exists with expiry).
    *   Why simpler implementations fail (e.g., process crashes before releasing lock, TTL expires before process finishes).
    *   **Redlock Algorithm:** How it works for high availability.
    *   **Libraries:** Using **Redisson** in Spring Boot instead of implementing `SETNX` manually.

## 5. Redis Eviction & Memory Management
**Scenario:** Your Redis instance has reached its max memory limit.
*   **Question:** How does Redis behave? What eviction policy would you choose for a caching layer vs. a session store?
*   **Discussion Points:**
    *   `noeviction`: Returns errors on writes (bad for cache).
    *   `allkeys-lru` vs `volatile-lru`: Evict any key vs. only keys with an expiry set.
    *   `allkeys-lfu`: Least Frequently Used (better for identifying hot keys).
    *   Scenario: If using Redis for Spring Session, you don't want to evict session data just to make room for cached HTML fragments. Separate Redis instances or careful policy selection.

## 6. Serializers and Performance
**Scenario:** You notice that the memory usage of your Redis cluster is higher than expected, and CPU usage is high during serialization/deserialization. You are currently using the default Spring Boot `JdkSerializationRedisSerializer`.
*   **Question:** How do you optimize this?
*   **Discussion Points:**
    *   **Drawbacks of Java Serialization:** Verbose, slow, security vulnerabilities.
    *   **Alternatives:** `GenericJackson2JsonRedisSerializer` or `StringRedisSerializer`.

## 7. Redis vs. Spring Cache Abstraction
**Question:** What is the difference between Redis and other caching mechanisms provided by Spring?

**Answer:**
*   **Abstraction vs. Store:** Spring Cache is an **abstraction layer** (an interface) that allows you to swap caching providers (ConcurrentHashMap, EhCache, Redis, Caffeine) without changing code. Redis is a specific **distributed in-memory data store**.
*   **Local vs. Distributed:** Spring's default `SimpleCacheManager` (using Maps) and providers like Caffeine are **local** to the JVM. If you have 3 pods, you have 3 separate caches. Redis is **external and distributed**, meaning all 3 pods share the same data (Single Source of Truth).
*   **Features:** Redis supports advanced features that local Spring caches do not, such as:
    *   **Persistence:** Saving data to disk (RDB/AOF).
    *   **Eviction Policies:** Global policies (LRU/LFU) across the entire cluster.
    *   **Data Structures:** Lists, Sets, Sorted Sets, Pub/Sub (Spring Cache only uses Key-Value).
    *   **Scalability:** Clustering and Sentinel for high availability.
*   **Production Use:** In production systems, Redis is preferred for **consistency** (users don't see stale data when hitting different pods) and **reliability**.

## 8. Handling Redis Downtime
**Scenario:** The Redis cluster acts up or goes down completely.
*   **Question:** How do you ensure your Spring Boot application doesn't crash effectively becoming unavailable?
*   **Discussion Points:**
    *   **Timeouts:** Aggressive connection and read timeouts.
    *   **Circuit Breaker:** Use Resilience4j to wrap cache calls. If Redis fails, fall back to DB directly or return default/empty responses (Degraded mode).
    *   `CachingConfigurerSupport` and `ErrorHandler`: Custom error handling in Spring Cache to log errors instead of throwing exceptions to the caller.

## 9. Hot Key Issue
**Scenario:** One specific key (e.g., a viral tweet or a flash sale item) is being accessed so frequently that a single Redis shard becomes the bottleneck (CPU 100%).
*   **Question:** How do you mitigate the "Hot Key" problem?
*   **Discussion Points:**
    *   **Local Caching (Multi-level Cache):** Use Caffeine/Guava in-memory cache on the application JVM for these specific hot keys (L1 Cache) backed by Redis (L2 Cache).
    *   **Key Replication:** Create `key_1`, `key_2`, `key_N` and distribute reads across them (though writes become harder).

## 10. Transactional Support
**Scenario:** You need to perform multiple Redis operations atomically (e.g., decrement stock and add item to user cart).
*   **Question:** Does Spring's `@Transactional` work with Redis?
*   **Discussion Points:**
    *   Redis logic is not rollback-able in the traditional relational DB sense.
    *   `MULTI` / `EXEC` blocks in Redis.
    *   Lua Scripts: The preferred way to ensure atomicity for complex operations in Redis (using `RedisTemplate.execute(script, ...)`).

## 11. Spring Boot Customization
**Scenario:** You want to cache data based on multiple parameters, but exclude specific fields from the key generation (e.g. `userId` and `date` are keys, but `requestId` is just for tracing).
*   **Question:** How do you implement this in Spring Boot?
*   **Discussion Points:**
    *   SPEL (Spring Expression Language) in `@Cacheable(key = "#userId + '_' + #date")`.
    *   Custom `KeyGenerator` bean implementation.
