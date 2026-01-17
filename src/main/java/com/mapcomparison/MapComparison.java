package com.mapcomparison;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MapComparison {

    public static void main(String[] args) {
        // 1. HashMap: Not Thread Safe, Allows Nulls
        System.out.println("--- 1. HashMap ---");
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put(null, "Null Key Allowed");
        hashMap.put("Key", null); // Null Value Allowed
        System.out.println("HashMap success: " + hashMap);


        // 2. Hashtable: Thread Safe (Legacy), NO Nulls
        System.out.println("\n--- 2. Hashtable (Legacy) ---");
        Hashtable<String, String> hashtable = new Hashtable<>();
        try {
            hashtable.put(null, "Value");
        } catch (NullPointerException e) {
            System.out.println("Hashtable threw NPE for key=null");
        }
        try {
            hashtable.put("Key", null);
        } catch (NullPointerException e) {
            System.out.println("Hashtable threw NPE for value=null");
        }


        // 3. SynchronizedMap: Thread Safe (Coarse lock), Allows Nulls
        System.out.println("\n--- 3. Collections.synchronizedMap() ---");
        Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
        syncMap.put(null, "Null Key Allowed"); // Works because the backing map is HashMap
        System.out.println("SynchronizedMap success: " + syncMap);


        // 4. ConcurrentHashMap: Thread Safe (Fine-grained lock), NO Nulls
        System.out.println("\n--- 4. ConcurrentHashMap ---");
        Map<String, String> concurrentMap = new ConcurrentHashMap<>();
        try {
            concurrentMap.put(null, "Value");
        } catch (NullPointerException e) {
            System.out.println("ConcurrentHashMap threw NPE for key=null");
        }
        try {
            concurrentMap.put("Key", null);
        } catch (NullPointerException e) {
            System.out.println("ConcurrentHashMap threw NPE for value=null");
        }
    }
}

