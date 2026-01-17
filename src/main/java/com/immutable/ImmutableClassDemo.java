package com.immutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ImmutableClassDemo {

    public static void main(String[] args) {
        List<String> skills = new ArrayList<>();
        skills.add("Java");
        skills.add("Spring");

        // 1. Creation
        ImmutableEmployee emp = new ImmutableEmployee(101, "Alice", skills);
        System.out.println("Original: " + emp);

        // 2. Attempt direct modification of passed list
        System.out.println("\n--- Attack 1: Modifying the original list passed to constructor ---");
        skills.add("Hacking"); // Modifying correct list
        System.out.println("After modifying original list: " + emp);
        // Should NOT change if constructor made a copy

        // 3. Attempt modification via getter
        System.out.println("\n--- Attack 2: Modifying the list returned by getter ---");
        try {
            emp.getSkills().add("Malware"); // Modifying list from getter
        } catch (UnsupportedOperationException e) {
            System.out.println("Caught Expected Exception: Cannot modify immutable list.");
        }
        System.out.println("After getter attack: " + emp);
    }
}

// ---------------------------------------------------------
// RULES FOR IMMUTABILITY
// ---------------------------------------------------------

// Rule 1: Class must be final (Cannot be extended/overridden)
final class ImmutableEmployee {

    // Rule 2: Fields should be private and final
    private final int id;
    private final String name;

    // Rule 3: Mutable fields need special handling
    private final List<String> skills;

    public ImmutableEmployee(int id, String name, List<String> skills) {
        this.id = id;
        this.name = name;

        // Rule 4: Deep Copy in Constructor
        // Never assign the passed mutable object directly!
        // this.skills = skills; <--- WRONG! Caller still holds reference.
        this.skills = new ArrayList<>(skills);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // Rule 5: No Setters

    // Rule 6: Return Deep Copy or Immutable View in Getters
    public List<String> getSkills() {
        // return this.skills; <--- WRONG! Caller can modify it.
        return Collections.unmodifiableList(this.skills);
    }

    @Override
    public String toString() {
        return "Employee{id=" + id + ", name='" + name + "', skills=" + skills + '}';
    }
}

