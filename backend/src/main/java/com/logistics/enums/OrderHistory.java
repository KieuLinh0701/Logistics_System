package com.logistics.enums;

public class OrderHistory {
    public enum OrderHistoryActionType {
        PENDING,
        READY_FOR_PICKUP,
        PICKING_UP,
        PICKED_UP,
        IMPORTED,
        EXPORTED,
        DELIVERING,
        DELIVERED,
        FAILED_DELIVERY,
        RETURNING,
        RETURNED,
        CANCELLED
    }
}
