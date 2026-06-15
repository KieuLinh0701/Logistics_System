package com.logistics.enums;

public enum OrderHistoryActionType {
    PENDING,
    READY_FOR_PICKUP,
    PICKING_UP,
    PICKED_UP,
    IMPORTED,
    EXPORTED,
    DELIVERING,
    DELIVERED,
    PARTIAL_DELIVERY,
    PARTIAL_RETURN,
    FAILED_DELIVERY,
    RETURNING,
    RETURNED,
    CANCELLED
}