# JDBC Interview Questions and Answers

## 1. What is JDBC?
**JDBC (Java Database Connectivity)** is a Java API that manages connecting to a database, issuing queries and commands, and handling result sets obtained from the database. It is part of Java SE (Standard Edition).

## 2. What are the key components of the JDBC API?
*   **DriverManager:** Manages a list of database drivers. Matches connection requests with the appropriate driver.
*   **Driver:** The interface that handles the communications with the database server.
*   **Connection:** Represents a session/connection with a specific database.
*   **Statement:** Used to execute static SQL statements.
*   **PreparedStatement:** Used to execute pre-compiled SQL queries (faster and secure).
*   **CallableStatement:** Used to execute Stored Procedures.
*   **ResultSet:** Represents the result set of a database query.

## 3. What is the difference between `Statement` and `PreparedStatement`?
| Feature | Statement | PreparedStatement |
| :--- | :--- | :--- |
| **Compilation** | SQL is compiled on the database server every time it is run. | SQL is compiled once and cached. Faster for repeated execution. |
| **Parameters** | Cannot pass parameters (`?`). Must concatenate strings. | Can act as a template with placeholders (`?`). |
| **Security** | Vulnerable to **SQL Injection**. | Secure against SQL Injection (escapes input). |
| **Usage** | DDL (Create/Drop) or one-time queries. | DML (Insert/Update/Select) with user input. |

## 4. What is `Class.forName("driver")` used for?
It dynamically loads the driver class into memory.
*   In older JDBC versions, this was required to register the driver with the `DriverManager`.
*   In JDBC 4.0+, this is often optional because `DriverManager` can automatically discover drivers found in the classpath via SPI (Service Provider Interface). However, explicit loading is still safer in some environments.

## 5. What are the different types of `ResultSet`?
*   **TYPE_FORWARD_ONLY (Default):** The cursor can only move forward.
*   **TYPE_SCROLL_INSENSITIVE:** The cursor can move forward and backward. The result set is *not sensitive* to changes made by others to the database while it is open.
*   **TYPE_SCROLL_SENSITIVE:** The cursor can move forward and backward. The result set *is sensitive* to changes made by others.

## 6. What is Connection Pooling?
Connection Pooling is a technique where a **cache of database connections** is maintained so that connections can be reused when future requests to the database are required.
*   **Problem:** Creating a new physical connection (`DriverManager.getConnection`) is expensive (time-consuming).
*   **Solution:** A pool creates N connections at start-up. Threads borrow a connection, use it, and return it to the pool instead of closing it.
*   **Libraries:** HikariCP, Apache DBCP, C3P0.

## 7. How do you handle Transactions in JDBC?
By default, JDBC is in **Auto-Commit** mode (every SQL statement is treated as a transaction and permanently written to the database immediately).

To manage transactions manually:
1.  **Disable Auto-Commit:** `connection.setAutoCommit(false);`
2.  **Perform Operations:** Run multiple SQL updates.
3.  **Commit:** `connection.commit();` (If all succeed).
4.  **Rollback:** `connection.rollback();` (If an exception occurs).

## 8. What is the difference between `execute()`, `executeQuery()`, and `executeUpdate()`?
*   **`executeQuery(sql)`:** Used for **SELECT** statements. Returns a `ResultSet`.
*   **`executeUpdate(sql)`:** Used for **INSERT, UPDATE, DELETE** or DDL statements. Returns an `int` (number of rows affected).
*   **`execute(sql)`:** Generic method used when the SQL type is unknown. Returns `true` if the first result is a `ResultSet`, `false` if it is an update count.

## 9. How to prevent SQL Injection in JDBC?
Use `PreparedStatement`. It sends the query structure and the data separately to the database driver. The driver treats the data strictly as literal values, not as executable SQL code.

**Bad (Vulnerable):**
```java
String query = "SELECT * FROM users WHERE name = '" + userName + "'";
```

**Good (Secure):**
```java
String query = "SELECT * FROM users WHERE name = ?";
PreparedStatement pstmt = conn.prepareStatement(query);
pstmt.setString(1, userName);
```

## 10. What is a "Dirty Read"?
A dirty read occurs when a transaction reads data that has not yet been committed. If the other transaction rolls back, the first transaction has read invalid data. This happens in the `READ_UNCOMMITTED` isolation level. JDBC allows setting isolation levels via `connection.setTransactionIsolation(level)`.

