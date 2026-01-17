package com.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class IteratorDemo {

    public static void main(String[] args) {
        testFailFast();
        testFailSafe();
    }

    // FAIL-FAST: Throws ConcurrentModificationException
    // Used by: ArrayList, HashMap, HashSet, etc.
    private static void testFailFast() {
        System.out.println("--- Testing Fail-Fast Iterator (ArrayList) ---");
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        list.add("D");

        try {
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                String item = it.next();
                System.out.println("Reading: " + item);

                if ("B".equals(item)) {
                    // DIRECT MODIFICATION
                    System.out.println("Attempting to remove B via List reference...");
                    list.remove("B");
                }
            }
        } catch (java.util.ConcurrentModificationException e) {
            System.err.println("EXCEPTION CAUGHT: ConcurrentModificationException (As expected for Fail-Fast)");
        }
    }

    // FAIL-SAFE (Weakly Consistent): Does NOT throw Exception
    // Used by: CopyOnWriteArrayList, ConcurrentHashMap
    private static void testFailSafe() {
        System.out.println("\n--- Testing Fail-Safe Iterator (CopyOnWriteArrayList) ---");
        List<String> list = new CopyOnWriteArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String item = it.next();
            System.out.println("Reading: " + item);

            if ("B".equals(item)) {
                // Modification is allowed.
                // CopyOnWriteArrayList creates a CLONE of the array for mutation.
                // The Iterator is still traversing the OLD array (Snapshot).
                System.out.println("Removing B via List reference...");
                list.remove("B");

                // Add something new too
                list.add("D");
            }
        }

        System.out.println("Final List: " + list);
        System.out.println("(Notice 'D' was added, but Iterator didn't see it because it iterated over a snapshot)");
    }
}

