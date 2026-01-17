# Builder Design Pattern

The **Builder Pattern** separates the construction of a complex object from its representation, allowing the same construction process to create different representations. 

It is a **Creational Design Pattern**.

### Core Problem: The Constructor Nightmare (Telescoping Constructor)

Imagine you have a class `User` with 2 required fields (`firstName`, `lastName`) and 5 optional fields (`age`, `phone`, `address`, `salary`, `nationality`).

**Attempt 1: Telescoping Constructors**
```java
public User(String first, String last) { ... }
public User(String first, String last, int age) { ... }
public User(String first, String last, int age, String phone) { ... }
public User(String first, String last, int age, String phone, String address) { ... }
// ... and so on ...
```
*   **Issue:** Hard to write, harder to read. "What does that 4th parameter do again?"

**Attempt 2: Setters (JavaBeans Pattern)**
```java
User u = new User();
u.setFirst("A");
u.setLast("B");
u.setAge(10);
```
*   **Issue:** The object is in an **inconsistent state** halfway through creation (e.g., created but Age is missing). Also, makes **Immutability** impossible (since setters must exist).

### The Solution: The Builder
```java
User user = new User.UserBuilder("A", "B") // Required
    .age(10)      // Optional
    .address("NY") // Optional
    .build();     // Create Final Immutable Object
```

---

## 1. Structure

1.  **Product:** The complex object being built (e.g., `Computer`). Its constructor is usually private.
2.  **Builder:** Inner static class.
    *   Duplicates the fields of the Product.
    *   Has setters that return `this` (Fluent Interface).
    *   Has a `build()` method that calls the private constructor of the Product.

---

## 2. Advantages

1.  **Fluent Interface:** Code reads like a sentence.
    *   `.setGraphicsCard().setRAM()`
2.  **Immutability:** You can make the Product class immutable (no setters), setting everything once in the Builder and then creating the object.
3.  **Readability:** No more passing `null` for optional parameters.
    *   **Bad:** `new Computer("i5", "8GB", null, null, true, false)`
    *   **Good:** `new ComputerBuilder("i5", "8GB").setBluetooth(true).build()`

---

## 3. Real World Use Cases

### A. Java `StringBuilder`
The most common example. String is immutable, so we use a builder to construct it.
```java
StringBuilder sb = new StringBuilder();
sb.append("Hello").append(" ").append("World"); // Method Chaining
String result = sb.toString(); // .build() equivalent
```

### B. Java Stream API
Builds a processing pipeline.
```java
Stream.of("a", "b", "c")
      .map(String::toUpperCase)
      .collect(Collectors.toList());
```

### C. Lombok `@Builder`
In modern Java/Spring Boot, we rarely write Builders manually. We use the Lombok library annotation.

```java
@Builder
@Getter
public class User {
    private String name;
    private int age;
}

// Usage
User u = User.builder().name("Alice").age(25).build();
```

