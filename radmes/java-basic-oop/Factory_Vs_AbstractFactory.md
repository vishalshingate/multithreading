# Factory Method vs Abstract Factory

Both patterns deal with creating objects (Creational Patterns), but they differ in scope and complexity.

## 1. Factory Method Pattern
**"Define an interface for creating an object, but let subclasses decide which class to instantiate."**

*   **Focus:** Creating **one** product.
*   **Mechanism:** Inheritance. Subclasses override the `createMethod()`.
*   **When to use:**
    *   You don't know the exact types of objects your code needs to work with.
    *   You want to provide a library but let users extend its internal components.
    *   Example: `Collections.iterator()` (ArrayList returns Itr, LinkedList returns ListItr).

### Structure
```text
[Logistics (Abstract)]
    |-- createTransport() --> [Transport]
           ^
           | (Inheritance)
    [RoadLogistics]
        |-- createTransport() --> [Truck]
```

---

## 2. Abstract Factory Pattern
**"Provide an interface for creating families of related or dependent objects without specifying their concrete classes."**

*   **Focus:** Creating a **Family** of products (e.g., Button + Checkbox + ScrollBar).
*   **Mechanism:** Object Composition. The factory object is passed to the client.
*   **When to use:**
    *   Your code needs to work with various families of related products (Multi-platform UI: Windows/Mac/Linux).
    *   You want strict consistency: A Client shouldn't accidentally mix a Windows Button with a Mac Checkbox.

### Structure
```text
[GUIFactory (Interface)]
    |-- createButton()
    |-- createCheckbox()
            ^
            | (Impl)
    [WindowsFactory]
        |-- createButton()   --> [WinButton]
        |-- createCheckbox() --> [WinCheckbox]
```

---

## 3. Key Differences Table

| Feature | Factory Method | Abstract Factory |
| :--- | :--- | :--- |
| **Number of Products** | Creates **one** product. | Creates a **family** of products. |
| **Technique** | Uses **Inheritance** (Subclass decides). | Uses **Composition** (Factory object helps). |
| **Complexity** | Simple. Extends the creator class. | Complex. Requires new interfaces for every product type. |
| **Relationship** | Often usually just a *method* inside a larger class. | Is usually a separate *class* entirely. |

### Note
In Spring Framework:
*   `BeanFactory` is a Factory pattern.
*   `ApplicationContext` is an Abstract Factory (creates beans, aspects, messages, etc).

