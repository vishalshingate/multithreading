package com.strings;

public class StringComparisonDemo {

    public static void main(String[] args) {
        int ITERATIONS = 100_000;

        System.out.println("--- 1. String (Immutable) ---");
        long start = System.currentTimeMillis();
        String str = "";
        for (int i = 0; i < 10_000; i++) { // Using smaller number for String to avoid hanging
            str += "a"; // Creates a NEW String object every time! (O(n^2) behavior)
        }
        long end = System.currentTimeMillis();
        System.out.println("String concatenation (10,000 times): " + (end - start) + "ms");


        System.out.println("\n--- 2. StringBuffer (Mutable + Thread Safe) ---");
        start = System.currentTimeMillis();
        StringBuffer sBuffer = new StringBuffer();
        for (int i = 0; i < ITERATIONS; i++) {
            sBuffer.append("a"); // Modifies the existing object
        }
        end = System.currentTimeMillis();
        System.out.println("StringBuffer append (" + ITERATIONS + " times): " + (end - start) + "ms");


        System.out.println("\n--- 3. StringBuilder (Mutable + Not Thread Safe) ---");
        start = System.currentTimeMillis();
        StringBuilder sBuilder = new StringBuilder();
        for (int i = 0; i < ITERATIONS; i++) {
            sBuilder.append("a"); // Modifies the existing object
        }
        end = System.currentTimeMillis();
        System.out.println("StringBuilder append (" + ITERATIONS + " times): " + (end - start) + "ms");
    }
}

