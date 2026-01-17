# Decorator Design Pattern

The **Decorator Pattern** allows you to dynamically add behavior or responsibilities to an individual object without affecting the behavior of other objects from the same class.

It is a **Structural Design Pattern**.

Think of it like **Matryoshka Dolls (Russian Nesting Dolls)** or **Getting Dressed**:
*   You (Component) put on a Shirt (Decorator).
*   Then you put on a Jacket (Decorator).
*   You are still "You", but now you are warmer and look different.

---

## 1. Visual Structure

```text
       [Interface: Coffee]
              ^
              |
      +-------+-------+
      |               |
[SimpleCoffee]   [Decorator] <---- Has a reference to Coffee
                      ^
                      |
             +--------+---------+
             |                  |
      [MilkDecorator]    [SugarDecorator]
```

*   **Component Interface:** Defines the common methods (`getCost`).
*   **Concrete Component:** The base object (`SimpleCoffee`).
*   **Decorator:** Wraps a component and implements the same interface. It delegates calls to the wrapped component, possibly adding logic before or after.

---

## 2. Why use Decorator? (Problem vs Solution)

### The Problem: Inheritance Explosion
Imagine a Coffee Shop. You have:
*   `Espresso`
*   `Decaf`

Now you want to add condiments:
*   `EspressoWithMilk`
*   `EspressoWithSugar`
*   `EspressoWithMilkAndSugar`
*   `DecafWithMilk`...

If you use inheritance, you will end up with dozens of classes for every combination. This is the **Class Explosion** problem.

### The Solution: Decorator (Composition)
Instead of inheriting, we **wrap**.
*   We create a `Milk` wrapper.
*   We create a `Sugar` wrapper.
*   We can stack them infinitely at runtime: `new Sugar(new Milk(new Espresso()))`.

---

## 3. Real World Example: Java I/O

The entire `java.io` package is built on the Decorator Pattern!

```java
// 1. Core Component (Stream from file)
InputStream fileStream = new FileInputStream("data.txt");

// 2. Decorator (Adds Buffering capability)
InputStream bufferedStream = new BufferedInputStream(fileStream);

// 3. Decorator (Adds method to read compressed ZIP data)
InputStream zipStream = new ZipInputStream(bufferedStream);

// 4. Decorator (Adds capability to read Objects)
ObjectInputStream objectStream = new ObjectInputStream(zipStream);
```

You are mixing and matching capabilities (Buffering, Unzipping, Object Deserialization) by wrapping streams inside streams.

---

## 4. Advantages vs Disadvantages

### Advantages
1.  **Flexibility:** Add or remove responsibilities at runtime (unlike Inheritance which is static).
2.  **Single Responsibility Principle:** You can break a monolithic class that does everything (logging, encryption, compression) into small decorators.
3.  **Recursion:** You can wrap an object multiple times (e.g., Double Milk).

### Disadvantages
1.  **Lots of Small Objects:** The code can become hard to debug because there are so many layers of wrappers (`Wrapper` inside `Wrapper` inside `Wrapper`).
2.  **Order Matters:** Sometimes the order in which you wrap decorators changes the result (e.g., Applying a Discount Decorator before or after a Tax Decorator).
3.  **Initial Setup Code:** Creating the object looks ugly: `new A(new B(new C(new D())))`. (The **Builder Pattern** is often used to fix this).

---

## 5. Decorator vs Inheritance

| Feature | Inheritance | Decorator |
| :--- | :--- | :--- |
| **Binding** | Static (Compile-time) | Dynamic (Run-time) |
| **Extension** | Extends the class logic for ALL instances. | Extends the logic for ONE specific instance. |
| **Combinations** | Hard. Requires new classes for every combo. | Easy. Mix and match wrappers. |


