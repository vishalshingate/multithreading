# Core Java: Memory, Variables & Concurrency

## 1. Static vs Instance vs Local Variables

| Feature | Instance Variable | Static Variable | Local Variable |
| :--- | :--- | :--- | :--- |
| **Scope** | Inside class, outside methods. | Inside class with `static` keyword. | Inside a method or block. |
| **Default Value** | **Yes** (0, false, or null). | **Yes** (0, false, or null). | **No** (Compile error if used). |
| **Memory** | Resides in **Heap**. | Resides in **Metaspace** (Java 8+). | Resides in **Stack**. |
| **Lifecycle** | Created with object, dies with GC. | Created when class loads, dies when class unloads. | Created on method entry, dies on exit. |

---

## 2. JVM Memory: Heap vs RAM vs ROM

### Is it RAM or ROM?
*   JVM Memory resides in **RAM** (Random Access Memory). RAM is volatile and fast.
*   **ROM** (Read-Only Memory) is typically for firmware or boot instructions; Java objects are never stored there.

### Heap vs Stack
*   **Heap:** Stores **Objects** and their instance variables. Shared by all threads.
*   **Stack:** Stores **Local Variables** and method call frames. Private to each thread (Isolation).

### Heap Memory Configuration
JVM gets heap memory from the OS. You can configure it using these flags:
*   `-Xms<size>`: Initial heap size (e.g., `-Xms512m`).
*   `-Xmx<size>`: Maximum heap size (e.g., `-Xmx2g`).
*   **Default:** Usually 1/4th of physical RAM (varies by JVM version and OS).

---

## 3. Concurrency: Multiple People calling Method1

**Scenario:** You have an API or `method1()`. Two users call it simultaneously.

### Does it create different threads?
*   **Web Servers (Tomcat/Spring):** Yes. Each incoming request is handled by a **separate thread** from a Thread Pool.
*   **Thread Execution:** Both Thread-A and Thread-B will execute the *same code* of `method1()`.

### How is Data Isolation handled?
1.  **Stack Isolation:** Every thread has its own **Stack**. Local variables inside `method1()` are created on the thread's private stack. Thread-A cannot see Thread-B's local variables.
2.  **Heap Sharing:** If `method1()` modifies a shared object (member variable), you have a **Race Condition**. This is where `synchronized` or `Locks` are needed.

---

## 4. Java Streams Internals

For a detailed explanation of **Sequential Streams vs Parallel Streams**, including internal working mechanism (Fork/Join, Spliterator) and when to use them, please refer to:
[Java Streams Internals](JAVA_STREAMS_INTERNALS.md)

---

## 5. Object Lock vs Class Lock

### Object Lock (Level: Instance)
Used to synchronize access to non-static code. Only one thread can execute a synchronized instance method on the **same object instance**.
```java
synchronized(this) { ... }
// or
public synchronized void method() { ... }
```

### Class Lock (Level: Class)
Used to synchronize access to static code. Only one thread can execute this code across **all instances** of that class.
```java
synchronized(MyClass.class) { ... }
// or
public static synchronized void method() { ... }
```

---

## 6. Synchronized Options Comparison

| Type | Syntax | What it locks |
| :--- | :--- | :--- |
| **Synchronized Method** | `public synchronized void m()` | `this` (Current object instance) |
| **Block (this)** | `synchronized(this) { ... }` | `this` (Current object instance) |
| **Block (class)** | `synchronized(Base.class) { ... }` | The `Class` object (Static/Global) |
| **Block (Monitor)**| `synchronized(lockObj) { ... }` | A specific private object (Best Practice) |

> **Best Practice:** Prefer `synchronized(lockObject)` over `synchronized(this)` to prevent external callers from locking your object and causing deadlocks.

---

## 7. Real World Interview Question: Why Static Variables? (ThingWorx Project Context)

**Question:** Why are static variables needed and where have you used them in your project (specifically ThingWorx)?

### Theoretical Answer (Why Needed?)
1.  **Memory Efficiency:** Static variables are per-class, not per-object. If you have 1000 objects but one common property (like a configuration constant), `static` saves memory by storing it once in Metaspace.
2.  **Global Access:** They allow access to data or methods without instantiating the class (e.g., Utility classes).
3.  **State Sharing:** They can maintain a common state across all instances (e.g., a counter).

### Project Implementation (ThingWorx / IoT Context)
In our IoT Edge Gateway aimed at ThingWorx integration, we used `static` in three key areas:

1.  **Property Mapping Constants (`public static final`):**
    *   ThingWorx generally requires data to be pushed in a "Property Bag" (DataShape).
    *   We defined the property names as static constants to avoid typos and determining them at runtime which creates unnecessary String objects on the Heap.
    ```java
    public class TWXConstants {
        // Maps to the "RemoteThing" property on the ThingWorx Platform
        public static final String PROP_TEMPERATURE = "Temp_C";
        public static final String PROP_VIBRATION = "Vibration_Hz";
    }
    ```

2.  **The Connected Client Singleton:**
    *   The `ConnectedThingClient` (from ThingWorx Java SDK) is a heavy object that maintains the active WebSocket connection to the ThingWorx server.
    *   We used a `private static` variable to hold this client instance (Singleton pattern) to ensure we only have **one** open connection per application, preventing port exhaustion or licensing limits on the server side.

3.  **Topic/Queue Definitions:**
    *   For the fallback mechanism, we defined the queue names (for ActiveMQ/RabbitMQ) as static variables to be shared between the *Producer Service* (reading sensors) and the *Consumer Service* (sending to ThingWorx).
