package com.logistics.utils;

import java.util.Set;
import com.logistics.enums.ShipmentStatus;

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
}