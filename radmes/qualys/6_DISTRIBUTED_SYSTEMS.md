# Qualys Interview Guide - Part 6: Distributed Systems

This document covers high-level system architecture concepts often asked for Senior Roles at Qualys.

---

## 1. CAP Theorem (Brewer's Theorem)

In a distributed data store, you can only provide **two** of the following three guarantees simultaneously:

1.  **Consistency (C):** Every read receives the most recent write or an error. (All nodes see the same data at the same time).
2.  **Availability (A):** Every request receives a (non-error) response, without the guarantee that it contains the most recent write.
3.  **Partition Tolerance (P):** The system continues to operate despite an arbitrary number of messages being dropped or delayed by the network between nodes.

**Choices:**
*   **CP (Consistency + Partition Tolerance):** MongoDB, HBase, Redis (Wait for sync). Good for Banking. Risk: Not available during network partition.
*   **AP (Availability + Partition Tolerance):** Cassandra, DynamoDB, DNS. Good for Social Media feeds (Eventual Consistency). Risk: Can return stale data.
*   **CA (Consistency + Availability):** RDBMS (MySQL). Only possible in non-distributed systems (No network partitions).

---

## 2. Consistency vs Availability

### Eventual Consistency (BASE)
*   **Basically Available:** System guarantees availability.
*   **Soft state:** State of system may change over time, even without input.
*   **Eventual consistency:** System will become consistent over time (Asynchronous replication).
*   **Example:** Facebook Likes.

### Strong Consistency (ACID)
*   **Atomicity:** All or nothing.
*   **Consistency:** Data validity maintained.
*   **Isolation:** Transactions occur independently.
*   **Durability:** Data persists.
*   **Example:** Bank transaction (Transfer money).

---

## 3. Kafka Architecture

Apache Kafka is a distributed event streaming platform.

### Core Components:
1.  **Topic:** Logical name for a stream of records (e.g., `user-signup-events`).
2.  **Partition:** Topics are split into partitions for parallelism. Ordered, immutable sequence of records.
3.  **Broker:** A Kafka server that stores data.
4.  **Producer:** Publishes messages to topics.
5.  **Consumer:** Subscribes to topics and processes messages.
6.  **Consumer Group:** Set of consumers cooperating to consume data from a topic.
    *   **Rule:** Each partition is consumed by only ONE consumer within a group. This guarantees ordering per partition.
7.  **Zookeeper:** Manages cluster metadata (Controller election, Topic config). Used less in newer versions (KRaft).

### Key Features:
*   **Durability:** Writes to disk (Sequential I/O is fast).
*   **Scalability:** Add more brokers/partitions.
*   **Replication:** Copies data (Leader/Follower) for fault tolerance.

---

## 4. Event-Driven Architecture (EDA)

Decouples services using events. A service publishes an event when something notable happens (State Change), and other services react to it.

**Core Principles:**
1.  **Asynchronous Communication:** Producers don't wait for Consumers.
2.  **Loose Coupling:** Producer doesn't know who is listening.
3.  **Scalability:** Services scale independently.

**Patterns:**
*   **Event Notification:** Simple fire-and-forget (e.g., "OrderCreated").
*   **Event Carried State Transfer:** Event contains data users might need (e.g., "UserUpdated: {id, name, email}"). Avoids calling back to source service.
*   **Event Sourcing:** Store state as a sequence of events, not just current state (e.g., Bank Ledger).

---

## 5. Microservices Communication

### Synchronous (REST / gRPC)
*   **Pros:** Simple, real-time response.
*   **Cons:** Tight coupling, cascading failures (Service A calls B calls C).
*   **Example:** Frontend calling User Service.

### Asynchronous (Message Queue / Pub-Sub)
*   **Pros:** Decoupling, handling spikes (buffering), reliability (retry later).
*   **Cons:** Complexity, eventual consistency debugging.
*   **Example:** Sending emails, generating reports.

### Service Discovery
*   **Client-Side:** Client queries Service Registry (Eureka/Consul) for instance IP.
*   **Server-Side:** Load Balancer (AWS ALB, K8s Service) handles discovery.

### Circuit Breaker Pattern (Resilience)
*   Prevents cascading failures. If a service fails repeatedly, the circuit opens and fails fast instead of waiting for timeout.
*   **Example:** Resilience4j / Netflix Hystrix.

