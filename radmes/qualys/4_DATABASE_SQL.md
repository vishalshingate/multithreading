# Qualys Interview Guide - Part 4: Database & SQL

This document covers detailed answers for **SQL Queries** and **Database Design** questions frequently asked in Qualys interviews.

---

## 1. SQL Queries

### A. Find Max / 2nd Highest Salary
**Use Cases:** Very common in interviews.

**Query 1: Max Salary**
```sql
SELECT MAX(salary) FROM Employee;
```

**Query 2: 2nd Highest Salary (Subquery Method)**
```sql
SELECT MAX(salary) FROM Employee 
WHERE salary < (SELECT MAX(salary) FROM Employee);
```

**Query 3: N-th Highest Salary (using LIMIT)**
```sql
SELECT salary FROM Employee 
ORDER BY salary DESC 
LIMIT 1 OFFSET 1; -- For 2nd highest (N-1)
```

**Query 4: Using Dense Rank (Window Function - Best Practice)**
```sql
SELECT * FROM (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) as rank 
    FROM Employee
) as ranked_table
WHERE rank = 2;
```

### B. Joins vs Subqueries
| Feature | JOIN | Subquery |
| :--- | :--- | :--- |
| **Logic** | Combines rows from two or more tables based on a related column. | A query nested inside another query (SELECT, INSERT, UPDATE, DELETE). |
| **Performance** | Generally faster (optimized by DB engine). | Often slower (especially correlated subqueries executed per row). |
| **Use Case** | Retrieving data from multiple tables. | Filtering results based on another query's result. |
| **Example** | `SELECT e.name, d.name FROM Employee e JOIN Department d ON e.dept_id = d.id;` | `SELECT name FROM Employee WHERE dept_id IN (SELECT id FROM Department WHERE location = 'USA');` |

### C. Indexing Strategies
**Definition:** Data structure (B-Tree) that improves speed of data retrieval operations.
**Types:**
1.  **Primary Index:** Automatically created on Primary Key (Unique + Not Null).
2.  **Unique Index:** Ensures no duplicate values.
3.  **Composite Index:** Index on multiple columns (Order matters: Leftmost prefix rule).
4.  **Clustered Index:** Defines physical order of data (Only 1 per table).
5.  **Non-Clustered Index:** Logical order. Points to data rows.

**Best Practices:**
*   Index columns used frequently in `WHERE`, `JOIN`, `ORDER BY`.
*   Avoid over-indexing (slows down `INSERT`/`UPDATE` operations).
*   Use `EXPLAIN` or `ANALYZE` to check query execution plan.

---

## 2. Database Design Concepts

### A. SQL vs NoSQL (When to Choose?)
| Criterion | SQL (RDBMS) | NoSQL (Distributed) |
| :--- | :--- | :--- |
| **Structure** | Structured (pre-defined schema). | Unstructured / Flexible schema. |
| **Scaling** | Vertical (Scale Up: bigger server). | Horizontal (Scale Out: more servers). |
| **Consistency** | Strong Consistency (ACID). | Eventual Consistency (BASE). |
| **Examples** | MySQL, PostgreSQL, Oracle. | MongoDB (Document), Cassandra (Column), Redis (Key-Value). |
| **Use Case** | Financial systems, Complex relationships. | Big Data, Real-time analytics, Content Management. |

### B. Partitioning vs Sharding
| Feature | Partitioning | Sharding |
| :--- | :--- | :--- |
| **Logic** | Splitting a large table into smaller parts within the **same** database instance. | Distributing data across **multiple** database instances (physical servers). |
| **Types** | Range (Date), List (Region), Hash. | Horizontal (Row-based), Vertical (Column-based). |
| **Complexity** | Managed by DB engine (e.g., PostgreSQL Partitions). | Application logic handles routing (Complex). |
| **Goal** | Improve manageability and query performance. | Unlimited horizontal scalability. |

### C. ACID Properties
Ensures reliable processing of database transactions.
1.  **Atomicity:** All or nothing. If one part fails, entire transaction rolls back.
2.  **Consistency:** DB goes from one valid state to another (constraints/rules maintained).
3.  **Isolation:** Concurrent transactions don't interfere with each other. (Levels: Read Uncommitted -> Serializable).
4.  **Durability:** Once committed, data is permanent even if system crashes (Write-Ahead Logging).

