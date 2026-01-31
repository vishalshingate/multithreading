# Database Indexing Guide & Best Practices

This guide covers the fundamentals of database indexing and best practices for adding indexes to columns, with examples relevant to the Library Events application.

## What is an Index?

An index is a data structure (commonly a B-Tree) that improves the speed of data retrieval operations on a database table at the cost of additional writes and storage space. Think of it like the index at the back of a book that helps you locate a specific topic quickly without flipping through every page.

## Why Use Indexes?
- **Faster Lookups:** Drastically reduces query time for `SELECT` operations.
- **Sorting:** Speeds up `ORDER BY` clauses.
- **Uniqueness:** Enforces unique constraints (e.g., typically Primary Keys are automatically indexed).
- **Relationships:** Optimizes `JOIN` performance between tables.

---

## Best Practices for Adding Indexes

### 1. Index Columns Used in `WHERE` Clauses
If you frequently search by a specific column, it should be indexed.
*   **Example:** If you often search for books by author:
    ```sql
    SELECT * FROM Book WHERE bookAuthor = 'Vishal';
    ```
    Creating an index on `bookAuthor` will make this query significantly faster.

### 2. Index Columns Used in `JOIN` Clauses
Foreign keys used to join tables should almost always be indexed.
*   **Example:** The `Book` table joins with `LibraryEvent` using `libraryEventId`.
    ```java
    @OneToOne
    @JoinColumn(name = "libraryEventId")
    private LibraryEvent libraryEvent;
    ```
    Ensure `library_event_id` column in the `Book` table has an index.

### 3. High Cardinality is Key
Index columns with **high cardinality** (many unique values).
*   **Good:** `bookId`, `email`, `socialSecurityNumber`.
*   **Bad:** `gender` (M/F), `status` (Active/Inactive), `Boolean` flags.
    *   *Why?* If an index narrows the result down to 50% of the table, the database might just scan the whole table anyway. Indexes work best when they select a small percentage of rows.

### 4. Use Composite Indexes for Multi-Column Queries
If you query by multiple columns together, a single **Composite Index** is more efficient than two separate indexes.
*   **Example:** Finding a book by name AND author.
    ```sql
    SELECT * FROM Book WHERE bookName = 'Kafka' AND bookAuthor = 'Vishal';
    ```
    *   Create an index on `(bookName, bookAuthor)`.
*   **Important:** Order matters! An index on `(A, B)` helps queries on:
    *   `A` and `B`
    *   `Just A`
    *   *But NOT* just `B` efficiently.

### 5. Avoid Over-Indexing
Indexes come with a cost:
*   **Write Performance:** Every `INSERT`, `UPDATE`, or `DELETE` requires updating the indexes.
*   **Storage:** Indexes take up disk space.
*   **Rule of Thumb:** Only index columns you actually query against frequently.

### 6. Consider "Covering Indexes"
A covering index includes all the fields requested in a query.
*   **Example:** `SELECT bookName FROM Book WHERE bookAuthor = 'Vishal';`
*   If you have an index on `(bookAuthor, bookName)`, the database can get the answer directly from the index without even looking up the main table data (this is extremely fast).

### 7. Sorting (`ORDER BY`)
The database can use an index to satisfied `ORDER BY` clauses if the index order matches the sort order.
*   **Example:** `SELECT * FROM LibraryEvent ORDER BY libraryEventId DESC;` is fast because `libraryEventId` is the Primary Key (clustered index).

### 8. Handling "LIKE" Queries
*   **Prefix match:** `WHERE bookName LIKE 'Kafka%'` -> **Can** use an index.
*   **Infix/Suffix match:** `WHERE bookName LIKE '%Kafka'` -> **Cannot** efficiently use a standard B-Tree index (requires full scan).

## Summary Checklist
| Do | Don't |
| :--- | :--- |
| Index Primary Keys and Foreign Keys | Index every single column |
| Index columns frequently used in `WHERE`, `ORDER BY`, `GROUP BY` | Index low-cardinality columns (e.g., boolean) |
| Monitor query performance (Explain Plan) | Ignore write-heavy impacts on indexed tables |
| Use composite indexes for frequent multi-column filters | Create duplicate indexes |

## Scenario-Based Interview Questions

### Scenario 1: Handling Indexing on a Table with Massive Data
**Question:** "We have a production table with millions (or billions) of rows. We realized we need to add an index to a column to fix a performance issue. How do you approach this?"

**Answer Strategy:**
Adding an index is an expensive operation that can lock the table, blocking incoming writes (and potentially reads), leading to downtime.
*   **MySQL:** Use `ALGORITHM=INPLACE, LOCK=NONE`. This allows the index to be created without locking the table for writes.
*   **PostgreSQL:** Use `CREATE INDEX CONCURRENTLY`. This builds the index without locking writes but takes longer and uses more CPU/IO.
*   **Maintenance Window:** If the database doesn't support online indexing, schedule a maintenance window during low traffic.
*   **Replica Strategy:** If using a Master-Slave architecture:
    1.  Take a slave out of rotation.
    2.  Add the index to the slave.
    3.  Bring it back and repeat for other slaves.
    4.  Finally, switch master to one of the indexed slaves (failover) or apply to master during maintenance.

### Scenario 2: Debugging Sudden API Slowness
**Question:** "An API endpoint that fetches library events was working fine for months. Suddenly, it has started timing out or taking 10+ seconds. How do you debug and fix this?"

**Answer Strategy:**
1.  **Isolate the Component:** Check logs/APM (like Datadog/NewRelic). Is the time spent in the Java application (CPU, GC) or waiting for the Database?
2.  **Analyze the Database Query:**
    *   **Enable Slow Query Logs:** Identify the exact SQL query execution time.
    *   **EXPLAIN PLAN:** Run `EXPLAIN` on the query.
        *   *Did the plan change?* Sometimes DB stats get stale, and the optimizer decides to do a **Full Table Scan** instead of using an existing index.
        *   *Fix:* Run `ANALYZE TABLE` to update statistics.
3.  **Check Data Volume:**
    *   Did the data size grow significantly? A query like `SELECT *` might be fine for 1,000 rows but slow for 1,000,000. Pagination should be used.
4.  **Check Index Usage:**
    *   Verify if the `WHERE` clause is still using the index. Did a developer change the query to use `LIKE '%value'` (leading wildcard) which invalidates the index?
5.  **Resource Contention:**
    *   Is the DB CPU/Memory maxed out by *other* queries?
    *   Are there locks? (e.g., a long-running batch job locking the rows this API tries to read).

### Scenario 3: Database Contention & Deadlocks
**Question:** "The application is throwing deadlock errors (e.g., `Deadlock found when trying to get lock`) or experiencing severe performance degradation due to lock contention. How do you find the root cause and fix it?"

**Answer Strategy:**
1.  **Identify the Contention:**
    *   **Metrics:** Check DB metrics for "Lock Wait Time" or "Active Transactions".
    *   **Logs:** Look for specific error codes (e.g., ORA-00060 in Oracle, 1213 in MySQL).
    *   **Real-time Monitoring:** Run `SHOW PROCESSLIST` (MySQL) or queries on `pg_stat_activity` (PostgreSQL) to see processes stuck in "Locked" state.

2.  **Analyze Deadlocks:**
    *   **MySQL:** Run `SHOW ENGINE INNODB STATUS` immediately after a deadlock. It prints the "LATEST DEADLOCK" section showing exactly which two transactions conflicted and which SQL statements they were running.
    *   **Logs:** Ensure your database is configured to log deadlocks (`innodb_print_all_deadlocks` in MySQL).

3.  **Root Cause & Fixes:**
    *   **Inconsistent Ordering:** Transaction A locks Row 1 then Row 2. Transaction B locks Row 2 then Row 1.
        *   *Fix:* Enforce a strict ordering for updates (e.g., always update `LibraryEvent` before `Book`, or sort IDs before locking multiple rows).
    *   **Long Transactions:** The longer a transaction stays open, the longer it holds locks.
        *   *Fix:* Keep transactions short. Do not perform external API calls or complex processing *inside* a `@Transactional` block.
    *   **Missing Indexes:** Without an index, a `DELETE` or `UPDATE` might lock the *entire table* (or many gaps) instead of just one row.
        *   *Fix:* Add indexes to foreign keys and `WHERE` clause columns.
    *   **Isolation Levels:** High isolation levels (like `Serializable`) increase locking.
        *   *Fix:* Evaluate if `Read Committed` is sufficient.

### Common Types of Deadlocks
While "Cyclic" is the most common, it helps to know the specific variations during an interview:

1.  **Cyclic Deadlock (The Classic):**
    *   **Definition:** Circular dependency where Process A waits for Process B, and Process B waits for Process A.
    *   **Example:**
        *   T1 updates `Table_A` (locks it).
        *   T2 updates `Table_B` (locks it).
        *   T1 tries to update `Table_B` -> Waits.
        *   T2 tries to update `Table_A` -> **Deadlock**.

2.  **Conversion Deadlock:**
    *   **Definition:** Two transactions hold a **Shared (Read) Lock** on the same resource and both try to upgrade to an **Exclusive (Write) Lock**.
    *   **Example:**
        *   T1 reads `Row_X` (Shared Lock).
        *   T2 reads `Row_X` (Shared Lock).
        *   T1 tries to update `Row_X` -> Waits for T2 to release its Read lock.
        *   T2 tries to update `Row_X` -> Waits for T1 to release its Read lock -> **Deadlock**.

3.  **Key-Ordering Deadlock:**
    *   **Definition:** Occurs when multiple rows are locked in a different order.
    *   **Example:** Batch job updating users. T1 updates IDs `[1, 5, 10]`. T2 updates IDs `[10, 5, 1]`. They will eventually clash and deadlock.
    *   **Fix:** Always sort resources by ID before processing (e.g., `Collections.sort(ids)`).

4.  **Gap Lock Deadlock (Common in MySQL/InnoDB):**
    *   **Definition:** Locks applied to the "gap" between index records to prevent other transactions from inserting into that gap (phantom reads).
    *   **Example:** T1 and T2 both try to delete a row that *doesn't exist*. Both get a "Gap Lock". Both then try to insert that same row. Deadlock occurs because they are waiting for each other's gap lock to release.
