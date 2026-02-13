# 🏦 UBS Interview Preparation Guide: Java & System Design

This comprehensive guide contains expert-level answers to key interview topics including **Java 8+**, **Spring Boot**, **Microservices Architecture**, and **High-Scale System Design**.

---

## 📑 Table of Contents
1. [🛡️ Logging Sensitive Information](#1-logging-sensitive-information-best-practices)
2. [⚡ Java 8 Stream API: Parallel Processing](#2-java-8-stream-api-parallel-processing)
3. [🧬 Exception Handling in Inheritance](#3-exception-handling-in-inheritance)
4. [🛠️ Custom Checked Exceptions in Spring Boot](#4-custom-checked-exception-in-spring-boot)
5. [🔢 Algorithm: Find Missing Number](#6-find-missing-number-in-array)
6. [🚀 Migrating Large Tables with Minimal Downtime](#7-migrating-large-table-millions-of-records-with-minimal-downtime)
7. [📊 Stream API: Multi-Level Sorting](#8-stream-api-sorting-by-transaction-then-type)
8. [🧩 Algorithm: Longest Consecutive Sequence](#9-longest-consecutive-sequence)
9. [🛡️ API Abuse & Mitigation Strategies](#10-api-abuse--mitigation)
10. [🔑 JWT Flow & Security Structure](#11-jwt-flow--structure)
11. [🧠 Complex Scenario: Filters, Regions, and Exceptions](#12-complex-scenario-filters-regions-and-exceptions)

---

## 🛡️ 1. Logging Sensitive Information: Best Practices
**Scenario:** You have client-identifying data (PII). You need to troubleshoot issues without exposing sensitive data in plain-text logs.

### Best Practices Checklist:
*   ✅ **Masking/Obfuscation**: Log partial versions (e.g., `4532XXXXXXXX1234`).
*   ✅ **Pseudonymization (Salting/Hashing)**: Hash client IDs with a secret salt. Cross-reference hashes in a secure DB for internal lookup.
*   ✅ **Correlation & Trace IDs**: Use UUIDs for transactions. Re-map Trace IDs to actual clients in a secure audit system only when necessary.
*   ✅ **Auto-Redaction**: Configure **Logback** or **Log4j2** filters to detect and redact patterns (Regex-based).
*   ✅ **Level Management**: Strictly control logging levels. Ensure `DEBUG` logs containing sensitive payloads never reach production.
*   ✅ **Vaulted Logs**: Store audit-trail logs in encrypted, RBAC-protected systems.

---

## ⚡ 2. Java 8 Stream API: Parallel Processing
How the JVM handles parallel data processing under the hood:

| Component | Responsibility |
| :--- | :--- |
| **ForkJoinPool** | Uses `commonPool()` to manage background threads. |
| **Spliterator** | Partitions the source (e.g., List) into manageable chunks. |
| **Recursive Decomposition** | Divides tasks using "Divide and Conquer" until chunks are small enough. |
| **Work-Stealing** | Idle threads steal tasks from busy ones to maximize CPU usage. |

> [!WARNING]
> **Parallel streams are NOT a silver bullet.** They add overhead for splitting/merging. Use them only for **CPU-intensive** tasks with large datasets where element order is irrelevant.

---

## 🧬 3. Exception Handling in Inheritance

**Rule:** If a superclass method declares an exception, the subclass overriding method can declare:
1.  **The same exception**.
2.  **A subclass of that exception**.
3.  **No exception at all** (safest).

**Restriction:** The subclass **CANNOT** declare a broad/new checked exception that is not in the parent's signature. (Liskov Substitution Principle).

```java
class Parent {
    void process() throws IOException {} // Checked
}

class Child extends Parent {
    // START VALID
    @Override void process() throws IOException {}
    @Override void process() throws FileNotFoundException {} // Subclass of IOException
    @Override void process() {} // No exception
    @Override void process() throws RuntimeException {} // Unchecked is always allowed
    // END VALID

    // INVALID: Exception 'SQLException' is not compatible with throws clause in Parent.process()
    // @Override void process() throws SQLException {} 
}
```

---

## 🛠️ 4. Custom Checked Exception in Spring Boot

Generally, Spring Boot (and clean code practices) prefers **Unchecked Implementation (RuntimeException)** because they don't force caller handling and work better with Transaction rollbacks (by default, transactions only rollback on RuntimeExceptions).

However, if you *must* use Checked Exceptions:

1.  **Create the Exception**:
    ```java
    public class BusinessRuleViolationException extends Exception {
        public BusinessRuleViolationException(String message) {
            super(message);
        }
    }
    ```
2.  **Handle in @ControllerAdvice**:
    Since it's checked, your Controller methods must declare `throws BusinessRuleViolationException`.
    ```java
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<String> handlechecked(BusinessRuleViolationException ex) {
        return ResponseEntity.status(400).body(ex.getMessage());
    }
    ```

---

## 🔢 6. Find Missing Number in Array
**Problem:** Given an array containing n distinct numbers taken from 0, 1, 2, ..., n, find the one that is missing.

**Optimized Approach (Sum Formula):**
1.  Calculate expected sum of `0` to `n`: `n * (n + 1) / 2`.
2.  Calculate actual sum of elements in array.
3.  Subtract actual from expected.

```java
public int missingNumber(int[] nums) {
    int n = nums.length;
    int expectedSum = n * (n + 1) / 2;
    int actualSum = 0;
    for (int x : nums) actualSum += x;
    return expectedSum - actualSum;
}
// Time: O(n), Space: O(1)
```

---

## 🚀 7. Migrating Large Table (Millions of Records) with Minimal Downtime
**Strategy: Dual-Write + Background Migration**

1.  **Add Column/Table**: Create the new table (or column) in the DB.
2.  **Dual Write (Application Layer)**: Modify the code to write to **BOTH** the old and new locations (New data is now consistent). 
    *   Reads still come from the Old table.
3.  **Backfill (Batch Job)**: Run a background script (Spring Batch / SQL procedure) to copy *historical* data from Old to New.
    *   *Challenge*: Updated records? Use a "LastModified" timestamp to only copy older records, or rely on the Dual Write having the latest version.
4.  **Verification**: Write a script to compare counts and random samples.
5.  **Flip the Read Switch**: Deploy code to read from the New table.
6.  **Stop Writing to Old**: Deploy code to stop the Dual Write.
7.  **Cleanup**: Drop the old table/column.

---

## 📊 8. Stream API: Sorting by Transaction then Type
**Scenario:** List of `Transaction { double value; String type; }`.
**Task:** Sort by Value (desc), then by Type (asc).

```java
List<Transaction> result = transactions.stream()
    .sorted(Comparator.comparingDouble(Transaction::getValue).reversed() // Primary
        .thenComparing(Transaction::getType))           // Secondary
    .collect(Collectors.toList());
```

---

## 🧩 9. Longest Consecutive Sequence
**Problem:** `[120, 204, 132]`. (Example seems malformed, typically input is `[100, 4, 200, 1, 3, 2]` -> Result `4` because `[1, 2, 3, 4]`).
**Logic:** Use a `HashSet` for O(1) lookups.
1.  Add all numbers to Set.
2.  Iterate array.
3.  For each `num`, check if `num - 1` exists.
    *   If **No**: This is the start of a sequence. Count upwards (`num + 1`, `num + 2`...) as long as they exist in Set.
    *   If **Yes**: Skip it (it's part of a sequence started by someone else).

```java
public int longestConsecutive(int[] nums) {
    Set<Integer> set = new HashSet<>();
    for (int n : nums) set.add(n);
    
    int max = 0;
    for (int n : nums) {
        if (!set.contains(n - 1)) { // Only start counting from the beginning of a sequence
            int currentNum = n;
            int count = 1;
            while (set.contains(currentNum + 1)) {
                currentNum++;
                count++;
            }
            max = Math.max(max, count);
        }
    }
    return max;
}
```

---

## 🛡️ 10. API Abuse & Mitigation
**Scenario:** A client is sending billions of requests (DDoS or crawling).
**Azure/Cloud Solution:**
1.  **Azure Front Door / WAF**: Block malicious IPs, Geoblocking (if attack is from non-business regions).
2.  **API Management (APIM)**:
    *   **Rate Limiting**: Limit calls to 100 per minute per Key/IP.
    *   **Quota**: 10,000 calls per day.
3.  **Pattern**: **Throttling**. Return `429 Too Many Requests` with a `Retry-After` header.

---

## 🔑 11. JWT Flow & Structure
**JWT (JSON Web Token)** is not "parent/child" but a carrier of **Claims**.

**Usage Flow:**
1.  **Client** POSTs credentials (user/pass) to `/auth/login`.
2.  **Server** validates DB, generates a JWT signed with a **Private Secret**.
    *   Payload: `{ "sub": "user123", "role": "admin", "exp": 1234567890 }`
3.  **Server** returns JWT.
4.  **Client** stores it (LocalStorage/Cookie) and sends it in Header: `Authorization: Bearer <token>` for all future requests.
5.  **Server (Gateway/Filter)**:
    *   Intercepts request.
    *   Validates Signature (using the same Secret).
    *   Checks Expiry.
    *   Extracts User/Roles and sets `SecurityContext`.

**Can JWT go "out"?** Yes, it is **stateless**. If stolen, it can be used until it expires.
*   **Mitigation**: Short Expiration (5-15 mins) + Refresh Tokens (stored in HTTPOnly Cookie).

---

## 🧠 12. Complex Scenario: Filters, Regions, and Exceptions
**Scenario**: 
- Employee Response has `isAttack` (or Region).
- If `Region == "Attack"`, Allow manipulation.
- If `Region != "Attack"`, Throw Checked Exception in a **Filter**.

### Handling Checked Exception in Filter
Java Filters methods (`doFilter`) do not declare custom checked exceptions. You cannot throw a checked exception directly out of a Filter unless you wrap it in `ServletException` or `RuntimeException`.

**Correct Approach (Terminate in Filter):**
Don't "throw" up. Handle it right there and send a response.

```java
@Override
public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
  throws IOException, ServletException {
      
    try {
        // Logic checking region...
        if (isNotAttackRegion) {
             throw new MyCustomCheckedException("Operation Not Allowed");
        }
        chain.doFilter(req, res); // Continue
        
    } catch (MyCustomCheckedException ex) {
        // 1. Log it
        // 2. Return Error JSON directly to client
        HttpServletResponse response = (HttpServletResponse) res;
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{ \"error\": \"Region not supported\" }");
        // Do NOT call chain.doFilter() -> Request stops here.
    }
}
```

### Unchecked Exception in Stream API (Retries)?
Streams terminate on **any** exception.
*   **To Handle**: Wrap the logic inside `.map` or `.forEach` in a try-catch block.
*   **To Retry**: Native Streams don't support retry. You must wrap the function call in a helper that has a Retry loop (e.g., executing the logic 3 times before re-throwing).
