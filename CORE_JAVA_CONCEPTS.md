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

## 4. Object Lock vs Class Lock

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

## 5. Synchronized Options Comparison

| Type | Syntax | What it locks |
| :--- | :--- | :--- |
| **Synchronized Method** | `public synchronized void m()` | `this` (Current object instance) |
| **Block (this)** | `synchronized(this) { ... }` | `this` (Current object instance) |
| **Block (class)** | `synchronized(Base.class) { ... }` | The `Class` object (Static/Global) |
| **Block (Monitor)**| `synchronized(lockObj) { ... }` | A specific private object (Best Practice) |

> **Best Practice:** Prefer `synchronized(lockObject)` over `synchronized(this)` to prevent external callers from locking your object and causing deadlocks.

