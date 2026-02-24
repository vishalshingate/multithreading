# Comprehensive SQL & Database Guide for Senior Software Engineers (5+ Years Exp)

This guide covers advanced database concepts, internals, performance tuning, and critical interview questions tailored for a Senior Backend role.

---

## 1. Database Internals & Architecture

### **ACID Properties (Deep Dive)**
*   **Atomicity**: All or nothing. Implemented using **Write-Ahead Logs (WAL)** or Undo Logs. If a transaction fails, the DB uses logs to rollback key states.
*   **Consistency**: Data must meet validation rules (Constraints, Foreign Keys). The DB moves from one valid state to another.
*   **Isolation**: Transactions occur independently without interference. Controlled by **Isolation Levels**.
*   **Durability**: Committed data is saved permanently even if power fails. Achieved via WAL/Redo Logs flushed to disk.

### **Transaction Isolation Levels**
Understanding these is crucial for solving concurrency bugs.

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Performance | Use Case |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Read Uncommitted** | Yes | Yes | Yes | Highest | Logging, Analytics (rarely used) |
| **Read Committed** (Default in PG/Oracle) | No | Yes | Yes | High | Standard web apps |
| **Repeatable Read** (Default in MySQL) | No | No | Yes | Medium | Financial apps requiring consistent reads |
| **Serializable** | No | No | No | Lowest | Strict consistency (banking) |

*   **Dirty Read**: Reading uncommitted data from another transaction.
*   **Non-Repeatable Read**: Reading the same row twice gets different data (someone updated it).
*   **Phantom Read**: Running the same query twice gets a different set of rows (someone inserted/deleted a row).

---

## 2. Indexing & Performance Tuning

### **Clustered vs. Non-Clustered Indexes**
*   **Clustered Index**:
    *   Determines the physical order of data on disk.
    *   Only **one** per table (usually Primary Key).
    *   Fast retrieval because the leaf node *is* the data page.
    *   **Tip**: Avoid using UUIDs as Clustered Indexes (causes fragmentation); use auto-increment or sequential IDs (TSID/ULID).
*   **Non-Clustered (Secondary) Index**:
    *   Stored separately from data. Contains the indexed value + pointer (or PK) to the actual data.
    *   Requires a "Lookup" (or "Bookmark Lookup") to get the full row.
    *   **Covering Index**: An index that contains all the fields required by the query, eliminating the need for a lookup.

### **Index Scans vs. Seeks**
*   **Index Seek**: Extremely fast. The engine traverses the B-Tree to find specific keys.
*   **Index Scan**: Reads the entire index structure. Slower than seek but faster than Table Scan.
*   **Table Scan**: Reads every row in the table. Worst performance (O(N)).

### **B-Tree vs. Hash Index**
*   **B-Tree**: Default. Good for range queries (`<`, `>`, `BETWEEN`, `ORDER BY`).
*   **Hash Index**: O(1) lookups. Only good for equality (`=`). No range queries.

### **Query Optimization Tips (Interview Gold)**
1.  **Select only what is needed**: Avoid `SELECT *`.
2.  **Avoid Functions on Indexed Columns**: `WHERE YEAR(date_col) = 2023` kills the index (Full Scan). Use `WHERE date_col >= '2023-01-01' AND date_col < '2024-01-01'`.
3.  **Use `EXPLAIN`**: Analyze the execution plan. Look for "Table Scan" or "Filesort".
4.  **Composite Indexes**: Order matters! If index is `(A, B, C)`, a query on `B` alone won't use it. (Leftmost Prefix Rule).
5.  **Avoid `OR`**: Sometimes splitting into `UNION ALL` is faster.
6.  **Wildcards**: `LIKE '%value'` disables index usage. `LIKE 'value%'` uses the index.

---

## 3. Advanced SQL Queries

### **Window Functions**
Essential for analytics and ranking without `GROUP BY` collapsing rows.

**Syntax**: `FUNCTION() OVER (PARTITION BY col1 ORDER BY col2)`

*   `ROW_NUMBER()`: Unique number for each row (1, 2, 3, 4).
*   `RANK()`: Ranking with gaps (1, 1, 3, 4).
*   `DENSE_RANK()`: Ranking without gaps (1, 1, 2, 3).
*   `LEAD()` / `LAG()`: Access next or previous row data (e.g., calculating WoW growth).

**Example: Find top 3 Highest Paid Employees per Department**
```sql
WITH RankedSalaries AS (
    SELECT 
        emp_name, 
        dept_id, 
        salary, 
        DENSE_RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) as rank
    FROM employees
)
SELECT * FROM RankedSalaries WHERE rank <= 3;
```

### **Common Table Expressions (CTEs)**
*   Improves readability over nested subqueries.
*   **Recursive CTE**: Used for hierarchical data (e.g., Organization Chart, Folder Structure).

**Example: Recursive CTE to find Manager Hierarchy**
```sql
WITH RECURSIVE Hierarchy AS (
    SELECT id, name, manager_id FROM employees WHERE id = 1  -- Anchor (CEO)
    UNION ALL
    SELECT e.id, e.name, e.manager_id 
    FROM employees e
    INNER JOIN Hierarchy h ON e.manager_id = h.id            -- Recursive member
)
SELECT * FROM Hierarchy;
```

---

## 4. Database System Design (Scaling)

### **Partitioning vs. Sharding**
*   **Partitioning**: Breaking a single table into smaller chunks within the **same** database instance.
    *   *Vertical*: Splitting columns (e.g., moving BLOBs to a separate table).
    *   *Horizontal*: Splitting rows (e.g., by Year).
*   **Sharding**: Distributing data across **multiple** database servers (nodes).
    *   Logic is complex (requires a Sharding Key).
    *   Cross-shard joins are expensive/impossible.

### **Replication**
*   **Master-Slave**: Writes go to Master, Reads go to Slaves. (Eventual Consistency).
*   **Master-Master**: Writes to any node. Complex conflict resolution.

### **CAP Theorem**
*   **Consistency**: Every read receives the most recent write or an error.
*   **Availability**: Every request receives a (non-error) response, without the guarantee that it contains the most recent write.
*   **Partition Tolerance**: The system continues to operate despite an arbitrary number of messages being dropped (network failure).
*   **Rule**: You can only pick 2 (CP or AP). RDBMS is usually CA (if single node) or CP/AP (if distributed).

---

## 5. Must-Do Interview Questions (5+ Years Experience)

### **Q1: How do you delete duplicate rows from a table without a temporary table?**
**Answer**: Using `CTE` and `ROW_NUMBER()`.
```sql
WITH Duplicates AS (
    SELECT id, 
           ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) as rn
    FROM users
)
DELETE FROM users 
WHERE id IN (SELECT id FROM Duplicates WHERE rn > 1);
```

### **Q2: Explain the "N+1 Problem". How do you fix it?**
**Answer**:
*   *Problem*: You fetch 1 parent object (Query 1), then loop through it to fetch N child objects (N queries). Total N+1.
*   *Fix (SQL)*: Use `JOIN` to fetch everything in 1 query.
*   *Fix (Hibernate)*: Use `JOIN FETCH` or `@EntityGraph`.

### **Q3: Index (a, b, c) exists. Will `WHERE b = ?` use the index?**
**Answer**: No. This violates the **Leftmost Prefix Rule**. B-Trees are traversed from the root; without 'a', the engine cannot traverse the tree efficiently. You need an index starting with `b`.

### **Q4: Difference between TRUNCATE, DROP, and DELETE?**
*   **DELETE**: DML command. Deletes row by row. Can be rolled back. Slow. Triggers fire.
*   **TRUNCATE**: DDL command. Resets table storage. Faster. Cannot be rolled back (in some DBs). No triggers.
*   **DROP**: DDL. Deletes entire table schema and data.

### **Q5: Optimistic vs. Pessimistic Locking? When to use which?**
*   **Pessimistic**: Locks the row immediately (`SELECT ... FOR UPDATE`). No one else can touch it.
    *   *Use case*: High contention, data integrity is critical (Bank Balance). Risk of deadlocks.
*   **Optimistic**: Doesn't lock. Uses a `version` column. Checks version on update. If changed, throws exception (`OptimisticLockException`).
    *   *Use case*: Low contention, web applications (Read-heavy). Better performance.

### **Q6: Find the 3rd highest salary without using `TOP` or `LIMIT`?**
**Answer**:
```sql
SELECT salary 
FROM employees e1 
WHERE 2 = (
    SELECT COUNT(DISTINCT salary) 
    FROM employees e2 
    WHERE e2.salary > e1.salary
);
```

### **Q7: What is a Deadlock? How to simulate and fix it?**
**Answer**:
*   *Definition*: Two transactions wait for each other to release locks. T1 locks A, needs B. T2 locks B, needs A.
*   *Fix*:
    *   Always access resources in the same order (Lock A then B).
    *   Keep transactions short.
    *   Use lower isolation levels if possible.

### **Q8: `UNION` vs `UNION ALL`?**
*   **UNION**: Removes duplicates. Performs a sorting operation (expensive).
*   **UNION ALL**: Keeps duplicates. Just concatenates results (Fast). always prefer `UNION ALL` if you know rows are unique.

### **Q9: How to optimize a slow query? (Step-by-step approach)**
1.  Check `EXPLAIN ANALYZE`. Is it doing a sequential scan?
2.  Are indexes being used?
3.  Are statistics up to date? (`ANALYZE table`).
4.  Is the query returing too much data?
5.  Is there locking contention?
6.  Rewrite: Replace `NOT IN` with `NOT EXISTS` or `LEFT JOIN`, avoid functions on columns, avoid `OR`.

### **Q10: Designing a URL Shortener DB Schema (System Design)**
*   **Columns**: `id` (PK), `long_url`, `short_code`, `created_at`, `expires_at`.
*   **Optimization**: Index on `short_code`.
*   **Collision**: How to generate `short_code`? (Base62 encoding of ID).
*   **Scaling**: If rows > billions, sharding based on `short_code` prefix.

### **Q11: Difference between WHERE and HAVING?**
*   **WHERE**: Filters rows **before** aggregation (`GROUP BY`). Cannot work with aggregate functions (like `SUM`, `COUNT`).
*   **HAVING**: Filters groups **after** aggregation. Works with aggregate functions.
*   **Example**:
    ```sql
    -- Correct
    SELECT dept_id, AVG(salary) 
    FROM employees 
    WHERE status = 'Active'      -- Filter row first
    GROUP BY dept_id 
    HAVING AVG(salary) > 50000;  -- Filter group result
    ```

### **Q12: Find the 2nd, 3rd, and Nth Highest Salary?**
**Method 1: Using `DENSE_RANK()` (Recommended for ties)**
```sql
WITH RankedSalaries AS (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) as rank
    FROM employees
)
SELECT salary FROM RankedSalaries WHERE rank = N; -- Replace N with 2, 3, etc.
```

**Method 2: Using `OFFSET` (MySQL/Postgres)**
```sql
SELECT DISTINCT salary 
FROM employees 
ORDER BY salary DESC 
LIMIT 1 OFFSET (N-1); -- For 2nd highest: LIMIT 1 OFFSET 1
```

**Method 3: Generic SQL (No Window Function)**
```sql
SELECT MAX(salary) 
FROM employees 
WHERE salary < (SELECT MAX(salary) FROM employees); -- 2nd Highest
```

### **Q13: SQL Order of Execution?**
Important for debugging errors like "Unknown column in WHERE clause".
1.  **FROM / JOIN**
2.  **WHERE**
3.  **GROUP BY**
4.  **HAVING**
5.  **SELECT**
6.  **ORDER BY**
7.  **LIMIT / OFFSET**

### **Q14: Write a query to find the Average Salary per Department?**
**Basic Aggregation**:
```sql
SELECT dept_id, AVG(salary) as distinct_avg_salary
FROM employees
GROUP BY dept_id;
```

**Scenario**: Find Departments where the average salary is greater than 10,000.
```sql
SELECT dept_id, AVG(salary) 
FROM employees 
GROUP BY dept_id 
HAVING AVG(salary) > 10000
ORDER BY AVG(salary) DESC;
```

### **Q15: Find employees whose salary is higher than the average salary of their department.**
**Answer**: Uses a **Correlated Subquery** or **Window Function**.

**Option 1: Window Function (Efficient)**
```sql
SELECT id, name, salary, dept_id 
FROM (
    SELECT id, name, salary, dept_id,
           AVG(salary) OVER (PARTITION BY dept_id) as avg_dept_salary
    FROM employees
) t
WHERE salary > avg_dept_salary;
```

**Option 2: Join with Aggregate**
```sql
SELECT e.name, e.salary
FROM employees e
JOIN (
    SELECT dept_id, AVG(salary) as avg_sal
    FROM employees
    GROUP BY dept_id
) avg_table ON e.dept_id = avg_table.dept_id
WHERE e.salary > avg_table.avg_sal;
```
