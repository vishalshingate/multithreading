package com.treemap;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class TreeMapDemo {

    public static void main(String[] args) {

        // 1. Natural Ordering (Comparable)
        // Integers implement Comparable, so they are sorted low-to-high by default.
        System.out.println("--- 1. Natural Ordering (Integer Keys) ---");
        TreeMap<Integer, String> scores = new TreeMap<>();
        scores.put(90, "Alice");
        scores.put(10, "Bob");
        scores.put(50, "Charlie");
        scores.put(20, "Dave");

        // Will print in sorted order: {10=Bob, 20=Dave, 50=Charlie, 90=Alice}
        System.out.println("Sorted Map: " + scores);


        // 2. Custom Comparator (Reverse Order)
        System.out.println("\n--- 2. Custom Comparator (Students by Name length) ---");
        TreeMap<String, Integer> studentAges = new TreeMap<>((s1, s2) -> {
            int lenCompare = Integer.compare(s1.length(), s2.length());
            // If length is same, fallback to alphabetical to avoid overwriting keys
            return lenCompare != 0 ? lenCompare : s1.compareTo(s2);
        });

        studentAges.put("Christopher", 25);
        studentAges.put("Ann", 20);
        studentAges.put("Bob", 22);

        // Sorted by name length: {Ann=20, Bob=22, Christopher=25}
        System.out.println("Sorted by Name Length: " + studentAges);


        // 3. Navigation Methods (Unique to NavigableMap interface)
        System.out.println("\n--- 3. Navigation Features ---");
        // Re-using the scores map: {10=Bob, 20=Dave, 50=Charlie, 90=Alice}

        System.out.println("First Key (Lowest): " + scores.firstKey()); // 10
        System.out.println("Last Key (Highest): " + scores.lastKey());  // 90

        // ceilingKey(45): Least key greater than or equal to 45 -> 50
        System.out.println("Ceiling Key of 45: " + scores.ceilingKey(45));

        // floorKey(45): Greatest key less than or equal to 45 -> 20
        System.out.println("Floor Key of 45:   " + scores.floorKey(45));

        // subMap(from, to): Range view (from inclusive, to exclusive)
        System.out.println("SubMap (20 to 90): " + scores.subMap(20, 90)); // {20=..., 50=...}
    }
}

