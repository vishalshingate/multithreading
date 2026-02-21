package com.codility;

import java.util.*;
import java.util.stream.*;

public class VisitCounter {

    public static class UserStats {
        Optional<Long> visitCount;

        public Optional<Long> getVisitCount() {
            return visitCount;
        }
    }

    public Map<Long, Long> count(Map<String, UserStats>... visits) {
        if (visits == null || visits.length == 0) {
            return Collections.emptyMap();
        }

        return Arrays.stream(visits)
                .filter(Objects::nonNull) // Skip null maps
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> entry.getValue() != null) // Skip null UserStats
                .filter(entry -> entry.getValue().getVisitCount().isPresent()) // Skip if Optional is empty
                .filter(entry -> isValidLong(entry.getKey())) // Skip if key is not a valid Long
                .collect(Collectors.groupingBy(
                        entry -> Long.parseLong(entry.getKey()),
                        Collectors.summingLong(entry -> entry.getValue().getVisitCount().get())
                ));
    }

    private boolean isValidLong(String s) {
        if (s == null) return false;
        try {
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}



/*


  if (visits == null || visits.length == 0) {
            return Collections.emptyMap();
        }

        return Arrays.stream(visits)
                .filter(Objects::nonNull)
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().getVisitCount() != null && entry.getValue().getVisitCount().isPresent())
                .filter(entry -> isValidLong(entry.getKey()))
                .collect(Collectors.groupingBy(
                        entry -> Long.parseLong(entry.getKey()), // Helper ensures validity
                        Collectors.summingLong(entry -> entry.getValue().getVisitCount().orElse(0L))
                ));




 */

