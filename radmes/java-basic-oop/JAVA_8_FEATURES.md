# Java 8 Features - Comprehensive Guide

Java 8 (released in March 2014) was a major feature release that transformed how Java code is written. It introduced functional programming concepts, the Stream API, and a new Date/Time API.

---

## 1. Lambda Expressions (`->`)

Lambda expressions allow you to write concise code for implementing functional interfaces (interfaces with a single abstract method). They treat functionality as a method argument, or code as data.

**Syntax:** `(parameters) -> expression` or `(parameters) -> { statements; }`

### Before Java 8 (Anonymous Inner Class):
```java
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Running without Lambda");
    }
};
```

### With Java 8 (Lambda):
```java
Runnable r = () -> System.out.println("Running with Lambda");
```

### Key Benefit:
*   Reduces boilerplate code.
*   Enables functional programming.

---

## 2. Functional Interfaces

A **Functional Interface** is an interface that contains exactly one abstract method. They can have any number of default or static methods.

*   **@FunctionalInterface Annotation:** While optional, it's best practice to use it. The compiler will trigger an error if you add a second abstract method.

### Common Functional Interfaces (`java.util.function`):

| Interface | Method Signature | Purpose | Example |
| :--- | :--- | :--- | :--- |
| **Predicate<T>** | `boolean test(T t)` | Takes an argument, returns boolean. | filtering streams (`filter()`) |
| **Consumer<T>** | `void accept(T t)` | Takes an argument, returns nothing. | printing (`forEach()`) |
| **Supplier<T>** | `T get()` | Takes nothing, returns a result. | generating values (`generate()`) |
| **Function<T, R>** | `R apply(T t)` | Takes type T, returns type R. | mapping values (`map()`) |

---

## 3. Stream API (`java.util.stream`)

Streams allow functional-style operations on collections of elements. A Stream does not store data; it operates on the source (Lists, Sets, etc.) and produces a result.

### Key Concepts:
*   **Intermediate Operations:** Return a new Stream, lazy (not executed until a terminal operation is called). Examples: `filter`, `map`, `sorted`.
*   **Terminal Operations:** Produce a result or side-effect, closing the stream. Examples: `collect`, `forEach`, `reduce`, `count`.

### Example:
```java
List<String> names = Arrays.asList("John", "Jane", "Adam", "Tom");
List<String> result = names.stream()
    .filter(name -> name.startsWith("J")) // Intermediate
    .map(String::toUpperCase)             // Intermediate
    .collect(Collectors.toList());        // Terminal
```

### Parallel Streams:
Enables multithreading effortlessly.
```java
names.parallelStream().forEach(System.out::println);
```

---

## 4. Method References (`::`)

Method references provide a way to refer to a method without executing it. It is a shorthand for a lambda expression that only calls an existing method.

### Types of Method References:
1.  **Static Method Reference:** `ClassName::staticMethodName` (e.g., `Math::max`)
2.  **Instance Method of a Particular Object:** `instance::methodName` (e.g., `System.out::println`)
3.  **Instance Method of an Arbitrary Object of a Particular Type:** `ClassName::methodName` (e.g., `String::length`)
4.  **Constructor Reference:** `ClassName::new` (e.g., `ArrayList::new`)

---

## 5. Default and Static Methods in Interfaces

Prior to Java 8, interfaces could only have abstract methods. Java 8 allows concrete methods.

### Default Methods (`default` keyword):
Allows adding new methods to interfaces without breaking implementing classes. This was crucial for adding `stream()` method to the `Collection` interface (backward compatibility).

```java
interface Vehicle {
    default void print() {
        System.out.println("I am a vehicle");
    }
}
```

### Static Methods:
Utility methods can now be defined directly in the interface.

```java
static void properties() {
    System.out.println("Vehicle properties");
}
```

---

## 6. Optional Class (`java.util.Optional`)

`Optional` is a container object used to contain not-null objects. It is used to represent null with absent value. It avoids `NullPointerException` and explicit null checks.

### Creation:
*   `Optional.of(value)` - Throws NPE if value is null.
*   `Optional.ofNullable(value)` - Allows null.
*   `Optional.empty()` - Creates an empty Optional.

### Usage:
```java
Optional<String> name = Optional.ofNullable(null);

// If present, do something
name.ifPresent(System.out::println);

// Return default value if empty
String validName = name.orElse("Default Name");

// Throw exception if empty
String value = name.orElseThrow(() -> new IllegalArgumentException("Name not found"));
```

---

## 7. Date and Time API (`java.time`)

The old `java.util.Date` and `Calendar` were mutable and not thread-safe. The new API (Joda-Time inspired) is **immutable** and **thread-safe**.

### Key Classes:
*   **LocalDate:** Date without time (e.g., `2023-10-05`).
*   **LocalTime:** Time without date (e.g., `12:30:00`).
*   **LocalDateTime:** Date and Time (e.g., `2023-10-05T12:30:00`).
*   **ZonedDateTime:** Date and Time with Timezone (e.g., `2023-10-05T12:30:00+05:30[Asia/Kolkata]`).
*   **Period:** Quantity of time in terms of years, months, days.
*   **Duration:** Quantity of time in terms of seconds and nanoseconds.

```java
LocalDate today = LocalDate.now();
LocalDate tomorrow = today.plusDays(1);
```

---

## 8. Nashorn JavaScript Engine

A new lightweight, high-performance implementations of JavaScript engine was introduced, allowing developer to embed JS code within Java applications.
*(Note: Deprecated in Java 11 and removed in Java 15).*

---

## 9. CompletableFuture

A huge improvement over the older `Future` interface. It provides a non-blocking way to write asynchronous code. It allows chaining multiple asynchronous tasks (callbacks).

```java
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenAccept(System.out::println);
```

---

## 10. Collectors Class

`Collectors` is a final class that provides reduction operations, such as accumulating elements into collections, summarizing elements according to various criteria, etc.

*   `Collectors.toList()`
*   `Collectors.toSet()`
*   `Collectors.joining()`
*   `Collectors.groupingBy()` - Similar to SQL GROUP BY.
*   `Collectors.partitioningBy()` - Partitions based on a Predicate (true/false).

```java
Map<Department, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment));
```

