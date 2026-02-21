# Pillars of Object-Oriented Programming (OOP)

This document explains the four fundamental pillars of Object-Oriented Programming (OOP) concepts: **Abstraction**, **Encapsulation**, **Inheritance**, and **Polymorphism**.

---

## 1. Abstraction

**Definition:** Abstraction is the process of hiding the internal implementation details and showing only the necessary features of an object. It focuses on **what** an object does rather than **how** it does it.

### Key Characteristics:
*   **Focus:** "What" an object does.
*   **Goal:** To reduce complexity by hiding unnecessary details.
*   **Implementation:** In Java, abstraction is achieved using:
    *   **Abstract Classes:** Can have both abstract (without body) and concrete methods.
    *   **Interfaces:** Define a contract (pure abstraction before Java 8; Java 8+ allows default/static methods).

### Real-world Example:
When you use a **Car**, you only need to know how to use the steering wheel, brake, and accelerator. You don't need to know how the engine combustion works or how the gears change internally. The dashboard provides an "abstract" interface to the complex machinery.

---

## 2. Encapsulation

**Definition:** Encapsulation is the process of wrapping data (variables) and code (methods) together as a single unit. It involves hiding the internal state of an object and restricting direct access to it.

### Key Characteristics:
*   **Focus:** "How" to hide the data and protect it.
*   **Goal:** To ensure data integrity and security by preventing unauthorized access/modification.
*   **Implementation:** In Java, encapsulation is achieved by:
    *   Declaring variables as `private`.
    *   Providing `public` getter and setter methods to access and update the values.

### Real-world Example:
A **Medical Capsule** contains the medicine inside it. The capsule (the wrapper) protects the contents from the external environment. You can't touch the medicine directly without going through the capsule.

---

## 3. Key Differences

| Feature | Abstraction | Encapsulation |
| :--- | :--- | :--- |
| **Definition** | Hiding complexity by showing only essential features. | Wrapping data and methods into a single unit and hiding data. |
| **Focus** | Focuses on the external behavior (What it does). | Focuses on the internal implementation (How it hides data). |
| **Implementation** | Achieved using `abstract` classes and `interfaces`. | Achieved using `private` variables and `public` getters/setters. |
| **Problem Solved** | Solves design-level problems. | Solves implementation-level problems. |
| **Relationship** | It is the process of gaining information. | It is the process of containing information. |

---

## 4. How they work together

Abstraction and Encapsulation are often used together.
*   **Abstraction** says: "I need a way to send a message."
*   **Encapsulation** says: "I will store the message in a private field and ensure it's not empty before sending."

### Java Example:

```java
// Abstraction: Interface defines "What" can be done
interface BankAccount {
    void deposit(double amount);
    void withdraw(double amount);
}

// Encapsulation: Class hides "How" data is stored and managed
class SavingsAccount implements BankAccount {
    private double balance; // Data Hiding (Encapsulation)

    @Override
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount; // Internal logic protected
        }
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= balance) {
            balance -= amount;
        }
    }

    // Encapsulation: Getter for read-only access
    public double getBalance() {
        return balance;
    }
}
```

## 5. Benefits

| Abstraction Benefits | Encapsulation Benefits |
| :--- | :--- |
| Reduces complexity for the user. | Data hiding provides security. |
| Enhances maintainability (change implementation without affecting users). | Makes the code more flexible and easy to change (Internal changes don't break external code). |
| Helps in focusing on high-level design. | Allows validation when setting values (e.g., preventing negative balance). |

---

## 6. Inheritance

**Definition:** Inheritance is a mechanism where a new class (Subclass/Child) acquires the properties and behaviors of an existing class (Superclass/Parent). It represents an **IS-A** relationship.

### Key Characteristics:
*   **Reusability:** Code written in the parent class can be used by the child class.
*   **Extensibility:** Child classes can add their own specific behavior.
*   **Method Overriding:** Child classes can provide a specific implementation of a method already defined in its parent class.

### Real-world Example:
**Vehicle** is a parent class. **Car** and **Bike** are child classes.
*   `Car` **IS-A** `Vehicle`.
*   `Car` inherits common properties like `speed`, `color` from `Vehicle` but has its own specific properties like `numberOfDoors`.

### Java Example:
```java
class Vehicle {
    protected String brand = "Ford";
    public void honk() {
        System.out.println("Tuut, tuut!");
    }
}

class Car extends Vehicle {
    private String modelName = "Mustang";
    public static void main(String[] args) {
        Car myCar = new Car();
        myCar.honk(); // Inherited method
        System.out.println(myCar.brand + " " + myCar.modelName);
    }
}
```

---

## 7. Polymorphism

**Definition:** Polymorphism means "many forms". It allows an object to be treated as an instance of its parent class rather than its actual class. It enables a single interface to represent different underlying forms (data types).

### Types of Polymorphism:

#### A. Compile-time Polymorphism (Static Binding)
*   **Achieved via:** Method Overloading.
*   **Definition:** Multiple methods with the same name but different parameters (count, type, or order) in the same class.

```java
class Calculator {
    int add(int a, int b) { return a + b; }
    double add(double a, double b) { return a + b; }
}
```

#### B. Runtime Polymorphism (Dynamic Binding)
*   **Achieved via:** Method Overriding.
*   **Definition:** A subclass provides a specific implementation of a method that is already defined in its superclass. The method to be called is determined at runtime based on the object instance.

### Real-world Example:
A **Person** behaves differently in different situations:
*   At home -> Father.
*   At office -> Employee.
*   At market -> Customer.
One person, many forms.

### Java Example:
```java
class Animal {
    public void animalSound() {
        System.out.println("The animal makes a sound");
    }
}

class Pig extends Animal {
    public void animalSound() {
        System.out.println("The pig says: wee wee");
    }
}

class Dog extends Animal {
    public void animalSound() {
        System.out.println("The dog says: bow wow");
    }
}

public class Main {
    public static void main(String[] args) {
        Animal myAnimal = new Animal();
        Animal myPig = new Pig();  // Upcasting
        Animal myDog = new Dog();  // Upcasting
        
        myAnimal.animalSound();
        myPig.animalSound(); // Runs Pig's version
        myDog.animalSound(); // Runs Dog's version
    }
}
```

---

## Summary of 4 Pillars

| Pillar | Concept | Key Benefit |
| :--- | :--- | :--- |
| **Abstraction** | Hiding implementation details. | Reduces complexity. |
| **Encapsulation** | Hiding data (state). | Security & Integrity. |
| **Inheritance** | Acquiring properties of another class. | Reusability. |
| **Polymorphism** | One interface, many implementations. | Flexibility. |
