# Miscellaneous: LLD, Database, & DevOps

This document covers essential Low-Level Design (LLD) concepts, Database fundamentals, and DevOps/Containerization basics essential for senior developer interviews.

---

## 1. Low-Level Design (LLD)

### Associations
Relationships between objects define how they interact.
-   **Association**: A general relationship where all objects have their own lifecycle and there is no owner (e.g., A Teacher communicates with a Student).
-   **Aggregation ("Has-A")**: A specialized form of association with a one-way relationship. Both entries can survive individually (e.g., A Department and a Teacher). If the Department is destroyed, the Teacher still exists.
-   **Composition ("Part-Of")**: A restricted form of Aggregation. The child cannot exist without the parent. (e.g., A House and a Room). If the House is destroyed, the Room is destroyed too.

### Favor Composition over Inheritance
-   **Inheritance ("Is-A")**: Creates a tight coupling between parent and child. Changes in the parent class affect all subclasses. It creates a rigid hierarchy.
-   **Composition ("Has-A")**: Classes contain instances of other classes to implement functionality.
    -   **Why Favor Composition?**
        -   **Flexibility**: Behaviors can be swapped at runtime (Strategy Pattern).
        -   **Testability**: Easier to mock components.
        -   **Encapsulation**: Does not expose internal implementation details of the parent.
        -   **Avoids Class Explosion**: Prevents deep inheritance trees.

### Design Principles
-   **SOLID**:
    -   **S - Single Responsibility Principle (SRP)**: A class should have only one reason to change.
    -   **O - Open/Closed Principle (OCP)**: Open for extension, but closed for modification.
    -   **L - Liskov Substitution Principle (LSP)**: Objects of a superclass should be replaceable with objects of its subclasses without breaking the application.
    -   **I - Interface Segregation Principle (ISP)**: Clients should not be forced to depend on interfaces they do not use. (Split large interfaces).
    -   **D - Dependency Inversion Principle (DIP)**: High-level modules should not depend on low-level modules. Both should depend on abstractions.
-   **DRY (Don't Repeat Yourself)**: Avoid code duplication. Extract common logic into methods/classes.
-   **YAGNI (You Ain't Gonna Need It)**: Do not implement features until they are actually needed. Avoid over-engineering.
-   **KISS (Keep It Simple, Stupid)**: Simplicity should be a key goal in design.
-   **TDD (Test-Driven Development)**: Write tests *before* writing the implementation code (Red -> Green -> Refactor).

### Low Coupling and High Cohesion
-   **Coupling**: The degree of dependency between two modules.
    -   **Goal**: **Low Coupling**. Changes in one module should not impact others. Achieved via Interfaces/DI.
-   **Cohesion**: How strongly related and focused the responsibilities of a single module/class are.
    -   **Goal**: **High Cohesion**. A class should do one thing and do it well (SRP).

### Design Patterns
Often categorized into three types:
1.  **Creational**: Singleton, Factory, Abstract Factory, Builder, Prototype.
2.  **Structural**: Adapter, Composite, Proxy, Facade, Bridge, Decorator.
3.  **Behavioral**: Strategy, Observer, Command, Template Method, Iterator, State.

---

## 2. Database & SQL

### DELETE vs TRUNCATE vs DROP
-   **DELETE**: DML command. Removes specific rows based on a `WHERE` clause. Can be rolled back. Slower (logs each row deletion).
-   **TRUNCATE**: DDL command. Removes **all** rows from a table. Resets identity seeds. Cannot be rolled back (in some DBs). Faster (minimal logging).
-   **DROP**: DDL command. Removes the **entire table structure** and data. Cannot be rolled back.

### Keys
-   **Primary Key (PK)**: Unique identifier for a record. Cannot be NULL.
-   **Foreign Key (FK)**: A field that links to the Primary Key of another table. Enforces referential integrity.
-   **Composite Key**: A combination of two or more columns to form a Primary Key.

### Joins
-   **INNER JOIN**: Returns records that have matching values in both tables.
-   **LEFT (OUTER) JOIN**: Returns all records from the left table, and the matched records from the right table (NULL if no match).
-   **RIGHT (OUTER) JOIN**: Returns all records from the right table, and the matched records from the left table.
-   **FULL (OUTER) JOIN**: Returns all records when there is a match in either left or right table.
-   **CROSS JOIN**: Cartesian product (every row of A paired with every row of B).

### Indexes
Data structures (B-Tree, Hash) that improve the speed of data retrieval operations.
-   **Clustered Index**: Defines the physical order of data (e.g., Primary Key). Only one per table.
-   **Non-Clustered Index**: Separate structure pointing to data rows. Multiple allowed.
-   **Note**: Indexes speed up `SELECT` but slow down `INSERT`/`UPDATE`/`DELETE`.

### Normalization
Process of organizing data to reduce redundancy and improve integrity.
-   **1NF (First Normal Form)**: Atomic values (no repeating groups or lists in a column).
-   **2NF**: 1NF + No partial dependency (all non-key attributes depend on the *entire* primary key).
-   **3NF**: 2NF + No transitive dependency (non-key attributes depend only on the primary key, not other non-key attributes).

### ACID, CAP, BASE
-   **ACID** (RDBMS):
    -   **Atomicity**: All or nothing.
    -   **Consistency**: DB remains in a valid state before and after transaction.
    -   **Isolation**: Concurrent transactions do not interfere.
    -   **Durability**: Committed data is saved permanently.
-   **CAP Theorem** (Distributed Systems - NoSQL): You can only have 2 of 3:
    -   **Consistency**: All nodes see the same data at the same time.
    -   **Availability**: Every request gets a response (success/failure).
    -   **Partition Tolerance**: System continues to operate despite network failures between nodes.
-   **BASE** (NoSQL model, favors availability):
    -   **B**asically **A**vailable.
    -   **S**oft state (state may change over time).
    -   **E**ventual consistency.

### N+1 Problem
Occurs when fetching a parent entity and then lazily loading children in a loop.
-   **Query 1**: Fetch N Parents.
-   **Query N**: Fetch children for each of the N parents.
-   **Total**: 1 + N queries.
-   **Fix**: Use `JOIN FETCH` (SQL JOIN) to retrieve everything in 1 query.

---

## 3. DevOps Concepts

### Containerization (Docker)
A lightweight alternative to Virtual Machines. Encapsulates the application and its environment (dependencies, OS config) into a single "Image".
-   **Docker Image**: Read-only template (Blueprint).
-   **Docker Container**: Runnable instance of an image.
-   **Dockerfile**: Script containing commands to assemble an image.

### How to Containerize a Spring Boot Application

1.  **Build the JAR**: `mvn clean package` -> Generates `app.jar` in `target/`.
2.  **Create a `Dockerfile`**:
    ```dockerfile
    # Start with a base image containing Java runtime
    FROM openjdk:17-jdk-alpine

    # Set working directory
    WORKDIR /app

    # Copy the JAR file to the container
    COPY target/my-app-1.0.jar app.jar

    # Expose the port the app runs on
    EXPOSE 8080

    # Command to run the application
    ENTRYPOINT ["java", "-jar", "app.jar"]
    ```
3.  **Build the Image**:
    ```bash
    docker build -t my-spring-app .
    ```
4.  **Run the Container**:
    ```bash
    docker run -p 8080:8080 my-spring-app
    ```

