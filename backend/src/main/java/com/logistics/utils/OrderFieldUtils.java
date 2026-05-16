package com.logistics.utils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OrderFieldUtils {

    private OrderFieldUtils() {}

    public static <T> boolean isChanged(T oldValue, T newValue) {
        if (oldValue instanceof BigDecimal oldBd && newValue instanceof BigDecimal newBd) {
            return oldBd.compareTo(newBd) != 0;
        } else if (oldValue instanceof List<?> oldList && newValue instanceof List<?> newList) {
            return !listsEqual(oldList, newList);
        } else if (oldValue instanceof Set<?> oldSet && newValue instanceof Set<?> newSet) {
            return !setsEqual(oldSet, newSet);
        } else if (oldValue instanceof Map<?, ?> oldMap && newValue instanceof Map<?, ?> newMap) {
            return !mapsEqual(oldMap, newMap);
        }
        return !Objects.equals(oldValue, newValue);
    }

    private static boolean listsEqual(List<?> a, List<?> b) {
        if (a == null) a = List.of();
        if (b == null) b = List.of();
        return a.equals(b);
    }

    private static boolean setsEqual(Set<?> a, Set<?> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static boolean mapsEqual(Map<?, ?> a, Map<?, ?> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}