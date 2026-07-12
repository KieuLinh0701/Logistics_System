package com.logistics.utils;

import com.logistics.entity.Order;
import com.logistics.enums.OrderPickupType;
import com.logistics.enums.OrderStatus;

public final class OrderStatusUtils {

    private OrderStatusUtils() {}

    public static boolean isLegacyDeliveryReady(Order order) {
        if (order == null) {
            return false;
        }
        return order.getPickupType() == OrderPickupType.AT_OFFICE
                && (order.getStatus() == OrderStatus.READY_FOR_PICKUP
                    || order.getStatus() == OrderStatus.PICKED_UP);
    }

    public static boolean canTransitionToDelivering(Order order) {
        if (order == null || order.getPickupType() != OrderPickupType.AT_OFFICE) {
            return false;
        }
        OrderStatus status = order.getStatus();
        return status == OrderStatus.AT_DEST_OFFICE
                || status == OrderStatus.READY_FOR_PICKUP
                || status == OrderStatus.PICKED_UP;
    }
}