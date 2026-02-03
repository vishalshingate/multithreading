# Advanced Java: Collections, Design Patterns & Principles

This guide covers deep-dives into Java core concepts, design patterns, and OOP principles.

---

## 1. Abstract Class vs. Interface

| Feature | Interface (Java 8+) | Abstract Class |
| :--- | :--- | :--- |
| **Inheritance** | A class can implement multiple interfaces. | A class can extend only one abstract class. |
| **State** | Cannot have instance variables (only `static final`). | Can have instance variables (fields). |
| **Methods** | Can have `default` and `static` methods. | Can have all types of methods (abstract, concrete, final, static). |
| **Constructor** | Cannot have constructors. | Can have constructors (called via `super()`). |
| **Access Modifiers** | All methods are `public` by default. | Can have `private`, `protected`, etc. |

**Rule of Thumb:**
- Use **Interface** for "Can-Do" relationships (capabilities like `Serializable`, `Runnable`).
- Use **Abstract Class** for "Is-A" relationships (base identity like `Animal` -> `Dog`).

---

## 2. Composition vs. Inheritance
"Favour Composition over Inheritance."

- **Inheritance**: Tight coupling. Changes in parent break children. "Is-A".
- **Composition**: Loose coupling. One class "Has-A" reference to another. Allows changing behavior at runtime (Dependency Injection).

**Real-world Example:**
Instead of `PrintingService extends Logger`, use `PrintingService` which has a `private Logger logger` field.

---

## 3. Design Patterns Deep Dive

### Factory vs. Abstract Factory
- **Factory**: Creates an object of a specific type (e.g., `ShapeFactory` returns a `Circle` or `Square`).
- **Abstract Factory**: A "Factory of Factories". It creates a family of related objects (e.g., `UIFactory` returns a `WindowsButton` and `WindowsCheckbox` or `MacButton` and `MacCheckbox`).

### Decorator Pattern
- **Purpose**: Dynamically add responsibilities to an object without subclassing.
- **Example in Java**: `InputStream` -> `BufferedInputStream` -> `GzipInputStream`. Each "decorates" the previous stream with new functionality.

### Observer Pattern
- **Purpose**: Define a one-to-many dependency so that when one object changes state, all its dependents are notified.
- **Usage in Spring**: `ApplicationEvent` and `@EventListener`.

---

## 4. TreeMap: Under the Hood
- **Data Structure**: **Red-Black Tree** (Self-balancing Binary Search Tree).
- **Ordering**: Sorted according to the natural ordering of its keys, or by a `Comparator`.
- **Complexity**: $O(\log n)$ for `get`, `put`, and `remove`.
- **Null Keys**: Does NOT allow `null` keys (will throw `NullPointerException`) because it needs to call `compareTo()` or `compare()`.

---

## 5. How to make a class Immutable
1. Declare the class as `final` (cannot be extended).
2. Make all fields `private` and `final`.
3. Do not provide **setters**.
4. **Deep Copy** in Constructor: If fields are mutable objects (like a `List` or `Date`), create a copy when storing them.
5. **Deep Copy** in Getters: Return a copy of mutable objects instead of the original reference.

---

## 6. WeakReferences
- **What**: Data that can be Garbage Collected even if the reference is still held.
- **Use Case**: Caches where you want to prevent Memory Leaks. If the JVM needs memory, it will clear WeakReferences.
- **Example**:
```java
WeakReference<User> weakUser = new WeakReference<>(new User("123"));
User user = weakUser.get(); // Might return null if GC'd
```
- **WeakHashMap**: A Map where entries are automatically removed when the key is no longer in ordinary use.

---

## 7. Memory & Variables: FAQ

### JVM Heap Memory: RAM or ROM?
- **Heap Memory** is definitely in **RAM**. ROM (Read-Only Memory) is for firmware and permanent storage.
- **How much heap by default?**: Usually 1/4 of total physical RAM (up to a limit).
- **How to configure?**: Use JVM flags:
    - `-Xms`: Initial heap size.
    - `-Xmx`: Maximum heap size.
    - `java -Xms512m -Xmx2g -jar app.jar`

### Default Values
| Variable Type | Default Value |
| :--- | :--- |
| **Static / Instance Fields** | Yes (e.g., `int=0`, `boolean=false`, `Object=null`). |
| **Local Variables** | **No**. Must be initialized before use or the code won't compile. |

---

## 8. String Performance: Builder vs. Buffer
- Both use the **Builder Design Pattern**.
- **StringBuilder**: Fast. Not thread-safe. Use it within methods (local scope).
- **StringBuffer**: Slower. Thread-safe (using `synchronized`). Use it for shared strings in multi-threaded contexts (rarely needed).

---

## 9. Spring Annotations & Design Patterns

### @Qualifier vs @Primary
- **Problem**: You have 1 Interface and 2 implementing classes. Spring doesn't know which one to inject.
- **@Primary**: Sets the "default" implementation.
- **@Qualifier**: Explicitly names the bean to inject.
- **Design Pattern**: This is a form of the **Strategy Pattern**. You choose the strategy (implementation) at runtime or configuration time.

### What pattern do DTOs solve?
- **Pattern**: **Data Transfer Object**.
- **Problem**: 
    1. **Over-fetching**: Sending 50 fields from a DB entity to the UI when only 2 are needed.
    2. **Coupling**: The DB schema should not dictate the API schema.
    3. **Performance**: Multiple remote calls can be merged into one DTO.
- **Mapping**: Often uses **Assembler Pattern** (via MapStruct or ModelMapper).

### Annotation-Pattern Mapping
| Annotation | Pattern Used |
| :--- | :--- |
| `@Transactional` | **Proxy Pattern** |
| `@PostConstruct` | **Template Method** (callback) |
| `@Autowired` | **Dependency Injection / Strategy** |
| `@Bean` | **Factory Method** |
| `@EventListener` | **Observer Pattern** |
| `@ConfigurationProperties` | **Decorator / Adapter** |
