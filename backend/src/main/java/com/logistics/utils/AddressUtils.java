package com.logistics.utils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AddressUtils {
    public static String buildFullAddress(String detail, String wardName, String cityName) {
        return Stream.of(detail, wardName, cityName)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }
}