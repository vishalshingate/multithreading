# Composition vs Inheritance

In Object-Oriented Programming, there are two main ways to reuse code: **Inheritance** and **Composition**.

While Inheritance is taught first in school, **Composition** is generally preferred in real-world software architecture.

---

## 1. The Core Difference

| Feature | Inheritance | Composition |
| :--- | :--- | :--- |
| **Relationship** | **"Is-A"** Relationship. | **"Has-A"** Relationship. |
| **Coupling** | **Tight Coupling.** Child is dependent on Parent implementation. | **Loose Coupling.** Class depends on an Interface. |
| **Flexibility** | **Static.** Defined at compile time. | **Dynamic.** Can change behavior at runtime. |
| **Code Reuse** | White-Box reuse (internals visible to subclass). | Black-Box reuse (internals hidden). |
| **Java Keyword** | `extends` | `private MyInterface field;` |

---

## 2. Inheritance ("Is-A")

Inheritance is when a class derives from another class.
*   **Example:** `Dog extends Animal`. A Dog IS AN Animal.
*   **Problem (Fragile Base Class):** If you change the `Animal` class, you might break the code in `Dog`, `Cat`, and `Lion` unexpectedly.

### Why it can be bad:
1.  **Tight Coupling:** The subclass relies heavily on the parent. You cannot change the parent without affecting all children.
2.  **Explosion of Classes:** If you have `Car`, and want `ElectricCar`, `PetrolCar`, `RacingElectricCar`, `RacingPetrolCar`... the hierarchy gets messy.
3.  **No Multiple Inheritance:** In Java, you can only extend **one** class. If you extend `Engine`, you cannot extend `Vehicle`.

---

## 3. Composition ("Has-A")

Composition is when a class holds a reference to another class/interface as a field.
*   **Example:** `Car has-an Engine`.
*   **Benefit:** You can plug in a `PetrolEngine` or an `ElectricEngine` without changing the `Car` class.

### Why it is better (Composition over Inheritance):
1.  **Flexibility:** You can change the "part" (Engine) at runtime (Setter Injection).
2.  **Testability:** You can easily Mock the component (Interface) during unit testing.
3.  **Encapsulation:** The internal details of the component are hidden. The Car only calls `engine.start()`. It doesn't care how it starts.
4.  **Avoids "God Objects":** Prevents creating massive parent classes that try to do everything.

---

## 4. When to use what?

| Use Inheritance if... | Use Composition if... |
| :--- | :--- |
| The relationship is strictly "Is-A". (e.g., `Manager` is an `Employee`). | The relationship is "Has-A" or "Uses-A". (e.g., `Employee` uses a `Laptop`). |
| You want to expose the *entire* API of the parent to the user of the child. | You want to expose only selected functionality (by delegating). |
| There is a clear hierarchy that won't change much. | You need to mix and match behaviors (e.g., Strategy Pattern). |

### The "Liskov Substitution Principle" Test
If `Dog` inherits from `Animal`, you should be able to use `Dog` everywhere you use `Animal`. If your subclass throws "NotSupportedException" for a parent method (e.g., `Ostrich extends Bird`, but Ostrich cannot `fly()`), then you have used Inheritance wrongly. Use Composition instead (`Bird has-a FlightBehavior`).

