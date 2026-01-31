# Kafka Consumer Scenarios & Interview Questions

## 1. The "Manual Acknowledgement & Duplicates" Paradox

**User Question:**  
*"If I am using At-Least-Once delivery and Manual Acknowledgement, how is there any chance I still get duplicate records?"*

### The Short Answer
**Manual Acknowledgement protects against Data Loss, not Duplicates.**  
Acknowledgment (`ack.acknowledge()`) is the step where you tell Kafka, "I'm done with this message, move the offset forward." If you crash *after* doing the work but *before* sending that signal, Kafka thinks the work wasn't done and sends the message again.

### Detailed Scenario (Step-by-Step)
Imagine a bank transaction system.

1.  **Poll**: Consumer requests messages. Kafka sends **Message A** (Offset 50).
    *   *Current Commited Offset: 49*
2.  **Process**: 
    *   Your code calculates interest.
    *   **Side Effect**: Update Database: `UPDATE ACCOUNT SET BALANCE = BALANCE + 10 WHERE ID = 1`.
    *   *Database Transaction Commits successfully.*
3.  **The Crash**: 
    *   Right before your code executes `acknowledgment.acknowledge()`, the server loses power, or the Kubernetes pod gets killed (OOMKilled).
4.  **Restart**: 
    *   The Consumer restarts and asks Kafka, "Where should I start?"
    *   Kafka checks the last committed offset. It is still **49** (because step 3 never finished).
5.  **Redelivery**: 
    *   Kafka sends **Message A** (Offset 50) again.
6.  **Duplicate Processing**: 
    *   Your code runs again.
    *   **Side Effect**: Update Database: `UPDATE ACCOUNT SET BALANCE = BALANCE + 10 WHERE ID = 1`.
    *   *Now the user has been credited $20 instead of $10.*

**Conclusion**: This is why **Idempotency** (handling duplicates in business logic) is mandatory, regardless of your AckMode.

---

## 2. Batch Listener vs. Record Listener: Failure Impact

**User Question:**  
*"How do batch and record listeners differ? If a batch listener fails, does the whole batch fail? What happens?"*

### Record Listener (`ConsumerRecord<K,V>`)
*   **How it works**: Framework calls your method once per record.
*   **Failure**: 
    *   If Record #5 fails, the `DefaultErrorHandler` catches the exception.
    *   It can retry just that record. 
    *   If retries are exhausted, it sends just Record #5 to the DLQ.
    *   It then proceeds to Record #6.
*   **Impact**: Failures are isolated. One bad apple doesn't spoil the bunch.

### Batch Listener (`List<ConsumerRecord<K,V>>`)
*   **How it works**: Framework calls your method with a list (e.g., 50 records).
*   **The Problem**:
    *   You loop through the list: `for (record : records) { ... }`.
    *   Records 1-20 process successfully.
    *   **Record 21 throws an Exception.**
    *   The loop breaks, and the exception bubbles up out of the `onMessage` method.

*   **What happens to the Batch?**
    *   **Default Behavior (Without specialized handling)**: 
        *   The framework marks the *entire batch* as failed.
        *   Since the listener "failed", the offsets for the successful records (1-20) are NOT committed.
        *   The container backs off and then redelivers the **entire batch** (Records 1-50).
    *   **The Consequence**: 
        *   Records 1-20 are processed a **second time**. (High duplicate risk!)
        *   Record 21 fails again.
        *   Infinite loop of processing 1-20 and failing at 21.

*   **How to fix Batch Failures?**
    *   You must use a `BatchErrorHandler` (e.g., `RecoveringBatchErrorHandler` or `CommonErrorHandler` properly configured).
    *   It helps the framework determine *which* index in the list failed.
    *   It can then:
        1.  Commit offsets for 1-20 (saving the work done).
        2.  Send Record 21 to a Dead Letter Topic.
        3.  Resume processing from Record 22.

---

## 3. Scenario-Based Interview Questions

### Scenario A: The "Poison Pill" in a Batch
**Interviewer**: *"You have a batch listener receiving 100 records per poll. Record #99 is malformed JSON. Your deserializer works, but your business logic throws a NullPointerException. Currently, the system enters a loop processing the batch over and over. How do you fix this without switching to Record Listener?"*

**Answer**: 
"I would configure a `CommonErrorHandler` with a `DeadLetterPublishingRecoverer`. Crucially, for a batch listener, the error handler needs to know *which* record failed. 
If iteration happens inside the `@KafkaListener` method, the exception usually doesn't carry the index of the failed record. 
To fix this, I would:
1.  Ensure the listener throws a `BatchListenerFailedException` containing the index of the failed record/list position.
2.  Or, switch the error handler to retry the batch, but logically split it to isolate the bad record."

### Scenario B: Order Processing & Inventory
**Interviewer**: *"We are processing 'Order Created' events. We reserve inventory in a SQL database. We use At-Least-Once delivery. Occasionally, we see inventory reserved twice for the same order ID. Why?"*

**Answer**:
"This is a classic duplicate delivery scenario. If the consumer crashes after the SQL `INSERT/UPDATE` but before the Kafka commit, the message is redelivered.
**Fix**: Make the operation idempotent.
1.  **Primary Key Constraint**: Use `order_id` as a Primary Key in an `inventory_reservations` table. The second attempt will fail with `DuplicateKeyException`, which we can catch and ignore (or just ack).
2.  **Upsert**: Use `INSERT ... ON CONFLICT DO NOTHING` or `MERGE` statements."

### Scenario C: High Lag during Rebalance
**Interviewer**: *"Every time we deploy a new instance of our consumer, we see a massive spike in lag and the group enters a 'Stop-the-World' rebalance state for 2 minutes. Why?"*

**Answer**:
"This is likely due to 'Eager Rebalancing'. When a new consumer joins, all partitions are revoked from all consumers, the group pauses, assigns partitions, and then resumes.
**Fixes**:
1.  **Cooperative Sticky Assignor**: Switch `partition.assignment.strategy` to `CooperativeStickyAssignor`. This allows consumers to hold onto their partitions while only moving the specific partitions that need to be transferred to the new node, eliminating the global downtime.
2.  **Long Processing**: Check if `max.poll.interval.ms` is too close to the actual processing time of a batch."

### Scenario D: The "Lost Data" on New Deployment
**Interviewer**: *"We deployed a new consumer group `analytics-group` to an existing topic with 1 million historical messages. We expected it to process all that history, but it is only processing new messages arriving after the deployment. Why?"*

**Answer**: 
"This is an issue with the `auto.offset.reset` configuration.
The default value is often `latest` (or `count` depending on the client, but usually `latest` in Spring defaults if not specified).
*   **Behavior**: When a consumer group connects and **has no existing committed offset** (like a brand new group), `latest` puts the offset at the end of the log. It skips all history.
*   **Fix**: Set `spring.kafka.consumer.auto-offset-reset=earliest`. This ensures valid offsets are used if found, but if no offset exists, it starts from the beginning (offset 0)."

### Scenario E: Scaling Limits & Idle Consumers
**Interviewer**: *"We have a topic with 4 partitions. We are experiencing consumer lag, so we scaled our consumer application from 4 instances to 10 instances in Kubernetes. Surprisingly, the throughput remained exactly the same, and 6 instances seem to be doing nothing. Why?"*

**Answer**:
"Kafka Consumer scaling is strictly limited by the number of partitions.
*   **The Rule**: Within a single Consumer Group, a partition can be assigned to **only one** consumer instance (thread) at a time to guarantee ordering.
*   **The Limit**: If you have 4 partitions, you can have at most 4 active consumers. The other 6 are 'idle standbys'.
*   **Fix**: To scale further, you must increase the number of partitions in the topic (e.g., to 10 or 20). Note that this re-partitions data and might affect ordering guarantees if you rely on Key-based ordering."

### Scenario F: Transactional "Dirty Reads"
**Interviewer**: *"Our producer sends messages using Kafka Transactions (`producer.sendOffsetsToTransaction...`) to ensure data is only written if a database update commits. However, our consumer is seeing messages that were supposed to be rolled back/aborted. Why?"*

**Answer**:
"The consumer is effectively doing a 'dirty read'.
*   **Root Cause**: The default `isolation.level` for a Kafka consumer is `read_uncommitted`. This means it reads all messages written to the log, including those from aborted transactions.
*   **Fix**: Set `spring.kafka.consumer.isolation-level=read_committed`. The consumer will then only read messages that have been effectively committed by the producer transaction, filtering out the aborted ones."

### Scenario G: Different Group IDs (Broadcasting vs Scaling)
**Interviewer**: *"If we accidentally (or intentionally) set different `group.id`s for two instances of the SAME application, what happens?"*

**Answer**:
"You switch from **Scaling** to **Broadcasting**.
*   **Same Group ID**: Kafka treats them as one logical application. It splits the partitions among them (Load Balancing). Message A goes to Instance 1 OR Instance 2.
*   **Different Group IDs**: Kafka treats them as two completely independent applications.
    *   Instance 1 (Group A) gets a copy of Message A.
    *   Instance 2 (Group B) *also* gets a copy of Message A.
*   **Result**: You will process every single message twice (once per instance). This is usually a bug if it's the same application logic, but a feature if they are different microservices (e.g., Inventory Service vs. Email Service)."

### Scenario H: Performance Tuning (Maxed Out Consumers)
**Interviewer**: *"Let's suppose we have a topic with 6 partitions and we have 6 consumer instances running (1 per partition). We are still seeing lag. We cannot add more partitions to the topic. How do we improve performance without increasing partitions?"*

**Answer**:
"Since we have reached the limit of Kafka Consumer parallelism (1 consumer per partition), we cannot scale horizontally by adding more instances. We must improve throughput within the existing consumers:
1.  **Optimize Processing Logic**: Review the code for bottlenecks. Can we batch DB updates? Can we use caching to reduce lookups? Reducing the time to process one message is the most effective way to clear lag.
2.  **Multi-threaded Processing**: 
    *   The `KafkaListener` runs on a single thread. It reads a record, processes it, and then reads the next. 
    *   We can decouple consumption from processing. The listener reads the record and submits it to an internal `ExecutorService` (Thread Pool) for async processing. 
    *   **Caution**: This makes offset management tricky. You must ensure you don't commit an offset until the async thread completes the work (Manual Ack with waiting, or using a specialized async handling library).
3.  **Tune Batch Size**: If using batch listeners, increasing `max.poll.records` might improve throughput by reducing network overhead and commit frequency.
    *   **Hardware Scaling**: Vertically scale the 6 consumer nodes (more CPU/RAM) if the bottleneck is computational."

### Scenario I: Downstream Service Down (Handling Resilience)
**Interviewer**: *"Your consumer receives a message and needs to call an external REST API (Downstream Service) to complete the transaction. That Downstream Service is currently down (returning 503 or Connection Timeout). How do you handle this? Do you keep retrying?"*

**Answer**:
"This depends on whether the outage is short (glitch) or long (outage).
1.  **Initial Strategy: Blocking Retry (Backoff)**
    *   For transient network errors, we use the `DefaultErrorHandler` with a `FixedBackOff` (e.g., 3 retries, 1s apart). If the service recovers quickly, processing continues.
    *   **risk**: If the service is down for hours, retrying every record 3 times effectively halts the consumer. Lag builds up.

2.  **Circuit Breaker Pattern (Resilience4j) - The Better Way**:
    *   We wrap the REST call in a Circuit Breaker.
    *   If the failure rate exceeds a threshold (e.g., 50%), the circuit **Opens**.
    *   **Open State**: Calls fail immediately without waiting for timeouts.
    *   **Integration with Kafka**: When the circuit is open, we can:
        *   **Throw an exception immediately**: This triggers the Kafka Error Handler.
        *   **Pause the Listener**: We can programmatically pause the `MessageListenerContainer` so we stop fetching records entirely until the service is healthy (detected via a health check or half-open state).
    
3.  **Dead Letter Queue (DLQ) vs. Retry Topic**:
    *   If the service is down for a long time, we shouldn't send everything to DLQ (because the data isn't 'poison', the system is just broken).
    *   **Better**: Valid messages should be stored in a **Parking Topic** or **Retry Topic** with a long delay (e.g., 10 minutes), or we stop consumption to prevent flooding the DLQ.

4.  **Summary**: I would implement a Circuit Breaker. If open, I would either pause the consumer or route messages to a 'Parking Lot' topic to be replayed manually when the system recovers."

### Scenario J: Downstream Service Slowness (Latency)
**Interviewer**: *"The downstream service isn't down, but it's very slow (taking 3 seconds per request instead of 100ms). Your consumer lag is increasing rapidly. What are your options?"*

**Answer**:
"Slowness creates a throughput bottleneck.
1.  **Increase Concurrency (Partition Splitting)**:
    *   The most robust Kafka solution is to increase the number of partitions (e.g., from 6 to 30) and increase consumer instances to match.
    *   This provides true parallel processing.
2.  **Async Processing (Decoupling)**:
    *   Instead of waiting for the REST call on the main listener thread, submit the task to a local `ThreadPoolTaskExecutor`.
    *   **Risk**: You must manage offset commits manually. You cannot commit until the async task succeeds. If the pod crashes, you might reprocess messages.
3.  **Strict Timeouts**:
    *   Configure aggressive `ReadTimeouts` (e.g., 2s). Fail fast and retry (potentially to a DLQ/Retry topic) rather than holding the connection open for 60s.
4.  **Bulkhead Pattern**:
    *   Use Resilience4j Bulkhead to limit the number of concurrent calls to the slow service. This prevents the slow service from consuming ALL threads in your application, allowing other efficient listeners to keep working."

### Scenario K: The Slow Consumer & "Stealing" (Rebalance)
**Interviewer**: *"If one consumer instance is extremely slow to process a message, will Kafka automatically give that message to another idle consumer in the same group?"*

**Answer**:
"**No, not automatically.** Kafka assigns partitions exclusively. If Consumer A has Partition 0, Consumer B cannot touch it, even if Consumer A is slow.
**BUT**, there is a catch: `max.poll.interval.ms`.
*   **The Trap**: If Consumer A takes longer than `max.poll.interval.ms` (default 5 mins) to process **one batch**, the broker assumes Consumer A is acting dead.
*   **The Consequence**:
    1.  The broker kicks Consumer A out of the group.
    2.  A **Rebalance** triggers.
    3.  Partition 0 is reassigned to Consumer B.
    4.  Consumer B reads the message and starts processing.
    5.  **Meanwhile**, Consumer A is *still running* the logic for that message (it wasn't dead, just slow).
*   **Result**: You now have **two consumers processing the same message simultaneously**. This is a dangerous 'Zombie Consumer' scenario.

### **How to Prevent Zombie Consumers? (Detailed Fixes)**
1.  **Reduce `max.poll.records`**:
    *   **Logic**: If processing 500 records takes 10 minutes, reducing it to 50 records might take 1 minute.
    *   **Action**: Set `spring.kafka.consumer.max-poll-records=50` (or lower). This forces the consumer to poll more frequently, sending heartbeats to the broker.

2.  **Increase `max.poll.interval.ms`**:
    *   **Logic**: Give the consumer more time to finish the batch before kicking it out.
    *   **Action**: Increase from default 5 minutes (300000) to 10 or 15 minutes if you have very heavy tasks.

3.  **Use Async Processing (External Thread Pool)**:
    *   **Logic**: Stop the main Kafka listener thread from blocking during heavy computation/IO.
    *   **Mechanism**: The Listener reads the record and instantly submits it to a `ExecutorService`. The listener function returns immediately, so it can poll again.
    *   **CRITICAL WARNING**: If you return immediately, Kafka thinks the message is "Done" and might commit the offset. You **MUST** switch to `AckMode.MANUAL` and only acknowledge the offset when the async thread finishes.

4.  **Isolate Heavy Processing**:
    *   If a task takes minutes, it might not belong in a real-time Kafka consumer. Consider offloading the ID to a database and having a separate "Job Worker" (e.g., Spring Batch) process it, rather than holding the Kafka connection open."

### Scenario L: Autoscaling Strategies (KEDA vs. CPU)
**Interviewer**: *"We deployed our consumers on Kubernetes. We set up Horizontal Pod Autoscaling (HPA) based on CPU usage > 70%. However, we realized that sometimes lag builds up massively (millions of records), but the pods don't scale up because CPU usage remains low (e.g., 30%). How do we fix this?"*

**Answer**:
"Standard CPU-based autoscaling is often ill-suited for Kafka Consumers.
*   **The Problem**: A consumer might be I/O bound (waiting for DB or Network), so CPU is low, but it is moving too slowly to keep up with the producer. Lag increases, but HPA sees '30% CPU' and does nothing.
*   **The Solution**: **Lag-Based Autoscaling**.
*   **KEDA (Kubernetes Event-driven Autoscaling)**: This is the industry standard for this problem.
    1.  KEDA connects to the Kafka Broker as an external metrics source.
    2.  It monitors the **Consumer Lag** for your specific consumer group.
    3.  **Rule**: 'If Lag > 5000, add a pod'.
    4.  It scales the deployment *proactively* based on the pending work backlog.
*   **The Hard Limit**: Remember, KEDA can only scale you up to the number of partitions. If you have 6 partitions, KEDA effectively stops at 6 pods. Scaling to 7 pods adds an idle consumer."

---

## 4. Core Concepts & Frequent Questions

### Q1: How does Kafka ensure message ordering?
**Answer:**
Kafka guarantees message ordering **only within a partition**, not across the entire topic.
*   **Partition Level**: Messages sent to a specific partition are appended in the order they arrive. A consumer reading from that partition will read them in that exact order (Offset 0, 1, 2...).
*   **Global Level**: There is no global ordering across partitions.
*   **How to achieve it**: To ensure strict ordering for specific data (e.g., all updates for `User_100`), you must ensure all messages for `User_100` go to the same partition. You do this by producing messages with a **Key** (e.g., key=`User_100`). Kafka hash-partitions the key to consistently map it to the same partition.

### Q2: Difference between At-Least-Once, At-Most-Once, and Exactly-Once delivery?
**Answer:**
These refer to the guarantee of message delivery semantics:
*   **At-Most-Once**: 
    *   **Behavior**: Message might be lost, but never redelivered.
    *   **How**: The consumer commits the offset *before* processing the message. If processing fails, the message is lost.
*   **At-Least-Once (Default & Most Common)**: 
    *   **Behavior**: Message is never lost, but might be redelivered (duplicates).
    *   **How**: The consumer commits the offset *after* successfully processing the message. If the consumer crashes before committing, the message is read again. Requires idempotent consumers.
*   **Exactly-Once**: 
    *   **Behavior**: Even if a producer retries sending or a consumer retries processing, the effect is reflected only once.
    *   **How**: Requires **Transactional API** on the Producer/Consumer side (`isolation.level=read_committed`) and Idempotent Producers (`enable.idempotence=true`).

### Q3: How does a Consumer Group work?
**Answer:**
A Consumer Group is a logical grouping of consumers working together to consume a topic.
*   **Load Balancing**: Kafka automatically divides the partitions of a topic among the consumers in the group. If a topic has 10 partitions and the group has 5 consumers, each consumer gets 2 partitions.
*   **Exclusivity**: A partition can be consumed by *only one* consumer within a group at any given time.
*   **Fault Tolerance**: If a consumer instance fails, the group performs a **Rebalance**. The failed consumer's partitions are reassigned to the remaining active consumers.
*   **Offset Tracking**: The group maintains its own offsets (`__consumer_offsets` topic), allowing it to resume from where it left off even after a restart.

### Q4: What is a Partition and how do you choose a Partition Key?
**Answer:**
*   **Partition**: A topic is split into multiple logs called partitions, allowing data to be stored and processed in parallel across multiple brokers. It is the unit of parallelism.
*   **Partition Key**: A value (like a User ID or Order ID) used to determine which partition a message goes to.
*   **Choosing a Key**:
    1.  **Cardinality**: Choose a key with high cardinality (many unique values) to ensure data is evenly distributed across partitions (avoiding "Hot Partitions").
    2.  **Ordering**: If you need strict ordering for an entity (e.g., updates for a specific `OrderId`), use that entity ID as the key.
    3.  **Business Logic**: If your consumer logic relies on grouping data (e.g., aggregation by Region), you might key by Region, though be careful of uneven distribution.

### Q5: How do you handle message retries and DLQ?
**Answer:**
In Spring Boot / Spring Kafka:
1.  **Local Retries (Blocking)**: 
    *   Use a `DefaultErrorHandler` with a `BackOff` policy (Fixed or Exponential).
    *   The consumer pauses, waits, and re-executes the listener for the same record. This blocks the partition but preserves strict order.
2.  **Dead Letter Queue (DLQ)**:
    *   After retries are exhausted (e.g., 3 failed attempts), the `DeadLetterPublishingRecoverer` is triggered.
    *   It publishes the failed message to a separate topic (usually `topic-name.DLQ`).
    *   The original offset is committed so the main consumer can move on.
3.  **Non-Blocking Retries (Advanced)**:
    *   Instead of blocking the main thread, publish the failed message to a "Retry Topic" with a timestamp. A separate consumer reads from that retry topic later.

### Q6: How do you ensure Idempotent Consumers?
**Answer:**
Since Kafka (At-Least-Once) guarantees duplicates can happen, consumers must be idempotent (safe to re-run).
1.  **Database Constraints**: 
    *   Rely on Primary Key constraints. If inserting a record with ID 100 fails because it exists, catch the exception and treat it as a success.
2.  **Upsert Logic**: 
    *   Use "Insert or Update" logic. "If ID 100 exists, update status; otherwise insert."
3.  **Tracking Table**: 
    *   Maintain a separate table of `processed_message_ids`.
    *   Start Transaction -> Check if Message ID exists -> Process Logic -> Insert Message ID -> Commit Transaction.

### Q7: What happens if I use different Consumer Group IDs?
**Answer:**
*   **Broadcast/Pub-Sub Model**: Each consumer group receives a **full copy** of all the messages in the topic.
*   **Independent Offsets**: Group A can be at Offset 100 while Group B is at Offset 5. They do not affect each other.
*   **Use Case**: This is how you allow multiple downstream services (e.g., Analytics, Audit, Notifications) to react to the same event independently.

### Q8: Why shouldn't I use unique `group.id`s for every instance of the SAME application?
**Answer:**
While you *can* technically do it, it is usually a mistake because:
1.  **Duplicate Processing (The biggest issue)**: If you have 3 instances of your `OrderService` with different Group IDs, **all 3** will process Order #123. The customer will get 3 confirmation emails, or your database will be hit 3 times.
2.  **No Load Balancing**: The goal of running multiple instances is usually to split the work (Instance A takes Orders 1-100, Instance B takes 101-200). Different Group IDs force every instance to do 100% of the work.
3.  **Data Corruption**: If both instances try to different updates to the same database row based on the same message simultaneously, you will have race conditions.

**When IS it okay?**
*   **Local Caching**: If every instance needs to update its own internal in-memory cache (e.g., specific configuration updates), then Broadcasting (unique Group IDs) is the correct pattern.
