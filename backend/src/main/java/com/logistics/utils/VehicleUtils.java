package com.logistics.utils;

import com.logistics.enums.VehicleStatus;
import com.logistics.enums.VehicleType;

public class VehicleUtils {

    public static String translateVehicleType(VehicleType value) {
        if (value == null) return "";

        return switch (value) {
            case TRUCK -> "Xe tải";
            case VAN -> "Xe van";
            case CONTAINER -> "Xe container";
            default -> value.name();
        };
    }

    public static String translateVehicleStatus(VehicleStatus value) {
        if (value == null) return "";

        return switch (value) {
            case AVAILABLE -> "Sẵn sàng";
            case IN_USE -> "Đang sử dụng";
            case MAINTENANCE -> "Đang bảo trì";
            case ARCHIVED -> "Ngừng hoạt động";
            default -> value.name();
        };
    }
}