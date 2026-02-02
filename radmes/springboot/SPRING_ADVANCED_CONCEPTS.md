# Spring Boot & Java Advanced Concepts

## 🏗 The Builder Design Pattern

### Why do we need the Builder Pattern?
The Builder pattern is a creational design pattern used to construct complex objects step-by-step. It is primarily used to solve two problems:

1.  **The Telescoping Constructor Problem**: When a class has many optional parameters, you end up with multiple constructors (e.g., `User(name)`, `User(name, age)`, `User(name, age, email)`, etc.). This is hard to maintain and read.
2.  **The Java Beans Pattern (Setters) Drawbacks**: While setters solve the telescoping constructor issue, they introduce new problems.

---

### Why can't we just use Setters?
If we provide a no-arg constructor and then multiple setters, we face several issues:

1.  **Inconsistent State**: An object can be in an "incomplete" or "invalid" state during its construction. For example, if a `User` MUST have an `email`, but the setter for `email` hasn't been called yet, the object is technically broken but still usable in code.
2.  **Mutability**: Setters make an object **mutable**. In multi-threaded environments or complex business logic, you often want **Immutable Objects** to prevent accidental side effects. With setters, anyone can change the object's value at any time.
3.  **Missing "Essential" check**: A Builder can have a `.build()` method that performs validation before returning the final object, ensuring the object is 100% valid upon creation.

---

### How it looks in Java (The Builder Flow)

```java
public class User {
    private final String name;  // Immutable
    private final String email; // Immutable
    private final int age;      // Immutable

    private User(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.age = builder.age;
    }

    // Builder Static Inner Class
    public static class Builder {
        private String name;
        private String email;
        private int age;

        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder age(int age) { this.age = age; return this; }

        public User build() {
            // Validation Logic
            if (email == null) throw new IllegalStateException("Email is required");
            return new User(this);
        }
    }
}

// Usage
User user = new User.Builder()
                .name("Alice")
                .email("alice@example.com")
                .age(25)
                .build();
```

---

### Key Benefits
*   **Immutability**: The final object is immutable (no setters).
*   **Safety**: Validation happens at the very end (`build()`).
*   **Readability**: The method chaining (`.name().email()`) is very clear compared to a constructor with 10 parameters where you might mix up the order of two strings.
*   **Spring Boot Context**: We see this everywhere in Spring (e.g., `ResponseEntity.ok().header(...).body(...)` or custom configurations).
