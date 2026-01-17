# Singleton Design Pattern

The **Singleton Pattern** ensures that a class has only **one instance** and provides a global point of access to it.

It is a **Creational Design Pattern**.

## 1. Implementation Approaches

### A. Eager Initialization
Create the instance when the class is loaded.
```java
private static final Singleton INSTANCE = new Singleton();
```
*   **Pros:** Simple, Thread-safe.
*   **Cons:** Instance created even if you never use it (wasted memory).

### B. Lazy Initialization (Non-synchronized)
Create instance only when needed.
```java
if (instance == null) instance = new Singleton();
```
*   **Pros:** Saves memory.
*   **Cons:** **NOT Thread-safe.** Two threads can enter the `if` block simultaneously and create two instances.

### C. Synchronized Method
```java
public static synchronized Singleton getInstance() { ... }
```
*   **Pros:** Thread-safe.
*   **Cons:** **Slow**. Locking the entire method every time creates a bottleneck, even after the instance is already created.

### D. Double-Checked Locking (Optimized)
Check `null` twice. Once without lock, once with lock.
```java
if (instance == null) {
    synchronized (Singleton.class) {
        if (instance == null) instance = new Singleton();
    }
}
```
*   **Pros:** Thread-safe and Performant.
*   **Cons:** Complex code. Requires `volatile` keyword to prevent Instruction Reordering.

### E. Bill Pugh Singleton (Static Inner Class) - Recommended
Uses the ClassLoader mechanism to ensure thread safety.
```java
private static class Helper {
    private static final Singleton INSTANCE = new Singleton();
}
```
*   **Pros:** Lazy loading + Thread-safe without explicit synchronization code.

### F. Enum Singleton - Best for simple cases
```java
enum Singleton { INSTANCE; }
```
*   **Pros:** Handles Serialization, Reflection attacks automatically.
*   **Cons:** Not flexible (cannot inherit from other classes).

---

## 3. FAQ: Singleton "Without Synchronization"

If an interviewer asks: **"Write a Thread-Safe Singleton without using the `synchronized` keyword?"**, you have two main answers:

### Answer 1: Bill Pugh Singleton (Lazy)
Best if you need to load resources lazily.
*   **How:** Uses a static inner class.
*   **Safety:** The JVM guarantees that a class is loaded by a single thread. The inner class isn't loaded until it's referenced.
*   **Code:**
    ```java
    public class Singleton {
        private Singleton() {}
        
        private static class Holder {
            private static final Singleton INSTANCE = new Singleton();
        }

        public static Singleton getInstance() {
            return Holder.INSTANCE;
        }
    }
    ```

### Answer 2: Enum Singleton (Eager/Robust)
Best overall because it is **impossible** to break via Reflection or Serialization.
*   **How:** `public enum Singleton { INSTANCE; }`
*   **Safety:** Handled internally by Java.

### Answer 3: Eager Initialization
*   **How:** `private static Singleton instance = new Singleton();`
*   **Safety:** Static initializers are run at class load time, which is thread-safe.

---

## 4. Ways to Break a Singleton (And how to fix them)

### A. Reflection Attack
You can use `setAccessible(true)` on the private constructor to create a new instance.
*   **Fix:** Throw an exception in the constructor if `INSTANCE` is not null.
    ```java
    private Singleton() {
        if (INSTANCE != null) throw new RuntimeException("Use getInstance()!");
    }
    ```
*   **Fix:** Use `Enum`. Java prevents reflection on Enum constructors.

### B. Serialization Attack
If you deserialize a Singleton (read from file), Java creates a **new object**.
*   **Fix:** Implement `readResolve()` method.
    ```java
    protected Object readResolve() {
        return getInstance();
    }
    ```

### C. Cloning Attack
If the class implements `Cloneable`, client can call `.clone()`.
*   **Fix:** Override `clone()` and throw exception.
    ```java
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    ```

### D. Multiple ClassLoaders
If two different ClassLoaders load the same class, they will each create their own static instance.
*   **Fix:** Ensure the Singleton is loaded by the parent ClassLoader.
