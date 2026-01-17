# Abstract Class vs Interface

This is one of the most common Java interview questions.

## 1. Quick Comparison Table

| Feature | Abstract Class | Interface |
| :--- | :--- | :--- |
| **Relationship** | **"Is-A"** (Identity). Inherits identity. | **"Can-Do"** (Capability). Defines a contract. |
| **Multiple Inheritance** | **No.** Can extend only one class. | **Yes.** Can implement multiple interfaces. |
| **State (Variables)** | **Yes.** Can have instance variables (`int age`). | **No.** Only `public static final` constants. |
| **Constructors** | **Yes.** Used to initialize state. | **No.** Cannot be instantiated. |
| **Access Modifiers** | Public, Protected, Private, Package-private. | Methods are implicitly `public`. (Java 9+ allows `private` helper methods). |
| **Performance** | Slightly faster (direct invocation). | Slightly slower (interface lookup), though negligible in modern JVMs. |
| **Adding Methods** | Adding a generic method can break subclasses (if not implemented). | Adding a `default` method (Java 8+) preserves backward compatibility. |

---

## 2. When to use which?

### Use an **Abstract Class** when:
1.  **You want to share code** among several closely related classes. (e.g., `Animal` -> `Dog`, `Cat`).
2.  **You need state.** The classes share a common state (fields) that requires initialization (e.g., `String name`, `dbResult`).
3.  **You need strict control** over access modifiers (`protected`, `private`).
4.  **Template Pattern:** You want to define a skeleton algorithm where some steps are abstract.

### Use an **Interface** when:
1.  **You expect unrelated classes** to implement your interface. (e.g., `Comparable`, `Serializable`).
    *   `House implements Comparable` and `Student implements Comparable`. Use Interface!
2.  **You want to specify the behavior** of a particular data type, but not concerned about who implements its behavior.
3.  **Multiple Inheritance:** You need a type to be part of multiple hierarchies (`class FlyingCar implements Car, Flyable`).

---

## 3. Modern Java Evolution (The Line is Blurring)

Previously (Java 7), Interfaces could ONLY have abstract methods.
*   **Java 8:** Added `default` and `static` methods to Interfaces.
    *   *Why?* To maintain backward compatibility (e.g., adding `stream()` to `List` interface without breaking all custom List implementations).
*   **Java 9:** Added `private` methods to Interfaces (for code reuse inside default methods).

**However, the core difference remains:**
> Interfaces cannot hold **State** (Instance Variables). Abstract Classes can.

---

## 4. Real World Analogy

*   **Abstract Class (Animal):**
    *   Defines what something **IS**.
    *   "A Dog IS-A Animal".
    *   It has failing concrete traits: It breathes, it has DNA code. 

*   **Interface (Flyable, Swimmable):**
    *   Defines what something **CAN DO**.
    *   "A Plane implements Flyable". "A Bird implements Flyable".
    *   They are functionally different things, but they share a capability.

