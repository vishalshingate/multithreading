# String vs StringBuilder vs StringBuffer

Comparison of the three ways to handle text in Java.

## 1. Quick Comparison Table

| Feature | String | StringBuffer | StringBuilder |
| :--- | :--- | :--- | :--- |
| **Immutability** | **Immutable**. Once created, cannot be changed. | **Mutable**. Can modify characters in place. | **Mutable**. Character sequence can be changed. |
| **Storage** | String Pool (Heap). | Heap. | Heap. |
| **Thread Safety** | **Yes** (Inherently, because it's immutable). | **Yes** (Synchronized methods). | **No** (Not Synchronized). |
| **Performance** | **Slow** for concatenation (Create new object every time). | **Slower than Builder** (Due to synchronization overhead). | **Fastest**. |
| **Introduced In** | Java 1.0 | Java 1.0 | Java 1.5 |

---

## 2. Detailed Breakdown

### A. String (Immutable)
*   **Behavior:** Every time you modify a string (`str + "a"`), Java creates a **new String object** in memory and updates the reference. The old object waits for Garbage Collection.
*   **Why?** Security, Caching (HashCodes), and String Pool efficiency.
*   **Use Case:** Constants, Parameters, small text manipulations.

```java
String s = "Hello";
s.concat(" World"); // 's' is STILL "Hello". The result is lost unless assigned.
s = s.concat(" World"); // Now 's' points to a new object "Hello World".
```

### B. StringBuffer (Mutable + Thread Safe)
*   **Behavior:** Modifies the existing character array. It methods are `synchronized`.
*   **Thread Safety:** Multiple threads can safely append to the same `StringBuffer` object.
*   **Performance:** Check lock overhead makes it slower than StringBuilder.
*   **Use Case:** Legacy code or when multiple threads must modify the **same** string buffer (Rare).

```java
StringBuffer sb = new StringBuffer("Hello");
sb.append(" World"); // 'sb' is now "Hello World" (Same Object)
```

### C. StringBuilder (Mutable + Not Thread Safe)
*   **Behavior:** Same as StringBuffer, but **without synchronization**.
*   **Performance:** Fastest.
*   **Use Case:** Most string manipulation scenarios (JSON building, SQL query building, Loops).

---

## 3. When to use what?

1.  **If string content will not change:** Use `String`.
2.  **If you alter the string in a loop:** Use `StringBuilder`.
3.  **If you alter the string in a loop being accessed by multiple threads:** Use `StringBuffer` (Though usually, `ThreadLocal` + `StringBuilder` is preferred).

---

## 4. String Internal Change (Java 9+)
*   **Before Java 9:** Stored as `char[]` (UTF-16, 2 bytes per char).
*   **Java 9+ (Compact Strings):** Stored as `byte[]`.
    *   If text is Latin-1 (mostly English), uses 1 byte per char.
    *   If text needs UTF-16, uses 2 bytes per char (with an encoding flag).
    *   This saves roughly 50% memory for English text strings.

