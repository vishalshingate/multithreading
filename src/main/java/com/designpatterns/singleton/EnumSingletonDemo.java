package com.designpatterns.singleton;

/**
 * The Enum Singleton is the most robust way to implement a Singleton in Java.
 *
 * WHY IS IT SAFE?
 * 1. Thread-Safe: Enum creation is thread-safe by default in Java.
 * 2. No Synchronized: You don't need to write 'synchronized' keywords.
 * 3. Reflection Safe: Java internally prevents Reflection from creating new instances of Enums.
 * 4. Serialization Safe: Enum serialization is handled by JVM, preserving the singleton property.
 */
public enum EnumSingletonDemo {

    INSTANCE; // The single instance

    // Business Logic
    public void performAction() {
        System.out.println("Processing action in Enum Singleton. HashCode: " + this.hashCode());
    }

    // Main method to test
    public static void main(String[] args) {
        System.out.println("--- Enum Singleton Test ---");

        EnumSingletonDemo s1 = EnumSingletonDemo.INSTANCE;
        EnumSingletonDemo s2 = EnumSingletonDemo.INSTANCE;

        s1.performAction();
        s2.performAction();

        System.out.println("Are they the same object? " + (s1 == s2));
    }
}

