package com.abstractvsinterface;

public class AbstractVsInterfaceDemo {
    public static void main(String[] args) {
        System.out.println("--- Abstract Class vs Interface Demo ---");

        Dog dog = new Dog("Buddy");

        // Abstract Class methods
        dog.sleep(); // Inherited concrete method
        dog.makeSound(); // Implemented abstract method

        // Interface methods
        dog.play(); // Implemented method
        Pet.validate(); // Static method in Interface (Java 8+)

        // State
        System.out.println("State from Abstract Class: " + dog.name);
        System.out.println("Constant from Interface: " + Pet.CATEGORY);
    }
}

// ==========================================
// 1. ABSTRACT CLASS
// ==========================================
// - Can have state (instance variables)
// - Can have constructors
// - "Is-A" relationship
abstract class Animal {
    // State (Mutable)
    protected String name;

    // Constructor
    public Animal(String name) {
        this.name = name;
        System.out.println("Animal Constructor called");
    }

    // Abstract Method (No body)
    abstract void makeSound();

    // Concrete Method (Has body)
    public void sleep() {
        System.out.println(name + " is sleeping (Abstract Class method)");
    }
}

// ==========================================
// 2. INTERFACE
// ==========================================
// - "Can-Do" relationship (Capabilities)
// - No state (Only public static final constants)
// - Multiple implementation allowed
interface Pet {
    // Constant (automatically public static final)
    String CATEGORY = "Domestic";

    // Abstract Method (automatically public abstract)
    void play();

    // Default Method (Java 8+) - Allows backward compatibility
    default void groom() {
        System.out.println("Pet is being groomed (Default Interface method)");
    }

    // Static Method (Java 8+)
    static void validate() {
        System.out.println("Validating Pet (Static Interface method)");
    }
}

// ==========================================
// 3. CONCRETE CLASS
// ==========================================
// Extends ONLY ONE Abstract Class
// Implements MULTIPLE Interfaces
class Dog extends Animal implements Pet {

    public Dog(String name) {
        super(name); // Must call parent constructor
    }

    // Implementing Abstract Class method
    @Override
    void makeSound() {
        System.out.println("Woof! Woof!");
    }

    // Implementing Interface method
    @Override
    public void play() {
        System.out.println(name + " is fetching the ball.");
    }
}

