package com.logistics.utils;

import com.logistics.enums.EntityType;

public class EntityTypeUtils {

    public static String translateEntityType(EntityType value) {
        if (value == null) return "";
        return switch (value) {
            default -> value.name();
        };
    }
}