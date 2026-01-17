# How to Create an Immutable Class in Java

An immutable class is a class whose instances can **never be modified** after they are initialized.

Standard Examples: `String`, `Integer`, `BigDecimal`.

## Checklist / Rules

1.  **Strictly Final Class:** Declare the class as `final` so it cannot be subclassed. (Subclasses could override methods and leak state).
2.  **Private Final Fields:** Make all fields `private` and `final`.
    *   `private`: Ensure direct access is blocked.
    *   `final`: Ensure they are assigned only once (in constructor).
3.  **No Setters:** Do not provide any setter methods.
4.  **Deep Copy in Constructor:**
    *   If a field is a **Mutable Object** (like `List`, `Date`, `Map`), do **NOT** assign the constructor argument directly to the field.
    *   Create a **new instance** (copy) of the object instead.
5.  **Deep Copy in Getters:**
    *   Do **NOT** return the direct reference of a mutable field in the getter.
    *   Return a copy or an implementation like `Collections.unmodifiableList()`.

---

## Example Scenario: The "Date" Trap

`java.util.Date` is mutable. `java.lang.String` is immutable.

### Bad Implementation (Vulnerable)
```java
public final class Period {
    private final Date start; // Date is mutable!

    public Period(Date start) {
        this.start = start; // DANGEROUS! Reference shared.
    }

    public Date getStart() {
        return start; // DANGEROUS! Caller gets reference.
    }
}
```

**Attack:**
```java
Date d = new Date();
Period p = new Period(d);
d.setYear(1990); // Modified 'p' internal state externally! Immutability broken.
```

### Good Implementation (Safe)
```java
public final class Period {
    private final Date start;

    public Period(Date start) {
        // Defensive Copy
        this.start = new Date(start.getTime());
    }

    public Date getStart() {
        // Defensive Copy on return
        return new Date(start.getTime());
    }
}
```

## Why use Immutable Classes?
1.  **Thread Safe:** Automatically thread-safe. No synchronization needed because state cannot change.
2.  **HashMap Keys:** Safe to use as keys in a Map. (If a key's hashcode changes after insertion, the map gets corrupted).
3.  ** caching:** Since they don't change, they can be cached safely.

