# Kafka Partitions & Message Ordering

## 1. The Core Concept: Ordering in Kafka
A fundamental rule of Kafka is: **Ordering is guaranteed ONLY within a single partition.**

*   **Within Partition 0:** If you send Message A then Message B, the consumer will strictly read A then B.
*   **Across Partitions:** If Message A is in Partition 0 and Message B is in Partition 1, there is **no guarantee** which one the consumer will read first.

## 2. Scenario: Strictly Ordered Messages
**Business Requirement:** You are processing bank transactions.
1.  `AccountCreated` (ID: 101)
2.  `MoneyDeposited` (ID: 101)
3.  `MoneyWithdrawn` (ID: 101)

If these messages are processed out of order (e.g., *Withdraw* before *Deposit*), the transaction might fail due to "Insufficient Funds".

### How to handle this?
You must ensure all messages related to Account `101` go to the **same partition**.

### The Solution: Partition Keys
When a producer sends a message, it can optionally specify a **Key**.
```java
// Producer Record: (Topic, Key, Value)
producer.send(new ProducerRecord<>("bank-transactions", "Account-101", transactionData));
```

*   **Default Partitioner Logic:** `Hash(Key) % NumberOfPartitions`
*   Kafka takes the bytes of the Key, hashes them, and uses modulo arithmetic to assign a partition.
*   **Result:** The key `"Account-101"` will ALWAYS hash to the same partition (e.g., Partition 2), as long as the number of partitions doesn't change.

## 3. How to Choose a Partition Key?

Choosing the right key is critical for system performance and correctness.

### Strategy A: High Cardinality (Best Practice)
Choose a key that has many unique values.
*   **Examples:** `UUID`, `CustomerID`, `DeviceID`, `OrderID`.
*   **Why?** It ensures data is evenly distributed across all partitions.
*   **Ordering:** You get ordering for that specific customer/device.

### Strategy B: strictly Business Grouping (Proceed with Caution)
Choosing a key based on a broad category.
*   **Examples:** `DepartmentID` (HR, IT, Sales), `Region` (North, South).
*   **Risk:** Data Skew (Hot Partitions).
    *   If "Sales" generates 90% of the messages, the partition handling "Sales" will be overloaded, while other partitions sit idle. Consumers will lag on the "Sales" partition.

### Strategy C: No Key (Null Key)
*   **Behavior:** Round-Robin (or Sticky Partitioning in newer versions). Kafka distributes messages evenly across all partitions.
*   **Pros:** Perfect load balancing.
*   **Cons:** **NO Ordering Guarantees.** Related messages will be scattered across different partitions.

## 4. Best Practices Checklist

| Requirement | Strategy | Key Selection |
| :--- | :--- | :--- |
| **Strict Ordering** (e.g., User actions) | Key-based Partitioning | Use `UserId` or `AggregateId` as Key. |
| **High Throughput / No Ordering** (e.g., Logs) | Round-Robin (No Key) | Send `null` as Key. |
| **Data Locality** (Group by Region) | Key-based | Use `RegionId` (Watch out for skew!). |

## 5. What if I change the number of partitions?
**Warning**: If you resize a topic (add partitions) while using Key-based partitioning:
1.  `Hash(Key) % 5` is NOT the same as `Hash(Key) % 6`.
2.  Existing keys will map to different partitions.
3.  **Ordering breaks** for existing data vs new data.
4.  **Fix**: When resizing, you typically need to create a new topic and migrate data, or accept a temporary loss of ordering locality.

## 6. Summary for Interviews
> "To handle ordering scenarios, I use a specific business identifier (like OrderID) as the **Partition Key**. This ensures all events for that ID land in the same partition. Since Kafka guarantees FIFO order within a partition, my consumer will process the lifecycle events (Created -> Updated -> Closed) in the exact order they occurred."
