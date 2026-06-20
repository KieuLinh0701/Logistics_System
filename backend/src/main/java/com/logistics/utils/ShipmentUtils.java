package com.logistics.utils;

import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.ShipmentType;

import java.util.Set;

public class ShipmentUtils {

    // Những Status mà Manager được phép hủy
    private static final Set<ShipmentStatus> CANCELLABLE_STATUSES_BY_MANAGER = Set.of(
            ShipmentStatus.PENDING);

    public static boolean canManagerCancelShipment(ShipmentStatus status) {
        return CANCELLABLE_STATUSES_BY_MANAGER.contains(status);
    }

    // Những Status mà Manager được phép chỉnh sửa
    private static final Set<ShipmentStatus> EDITABLE_STATUSES_BY_MANAGER = Set.of(
            ShipmentStatus.PENDING);

    public static boolean canManagerEditShipment(ShipmentStatus status) {
        return EDITABLE_STATUSES_BY_MANAGER.contains(status);
    }

    // Những Status mà Manager được phép thêm đơn hàng vào chuyến
    private static final Set<ShipmentStatus> ADDED_ORDER_STATUSES_BY_MANAGER = Set.of(
            ShipmentStatus.PENDING);

    public static boolean canManagerAddOrderForShipment(ShipmentStatus status) {
        return ADDED_ORDER_STATUSES_BY_MANAGER.contains(status);
    }

    public static String translateShipmentStatus(ShipmentStatus value) {
        if (value == null) return "";

        return switch (value) {
            case PENDING -> "Chờ khởi hành";
            case IN_TRANSIT -> "Đang vận chuyển";
            case COMPLETED -> "Đã hoàn thành";
            case CANCELLED -> "Đã hủy";
            default -> value.name();
        };
    }

    public static String translateShipmentType(ShipmentType value) {
        if (value == null) return "";

        return switch (value) {
            case DELIVERY -> "Giao hàng";
            case TRANSFER -> "Trung chuyển";
            default -> value.name();
        };
    }
}