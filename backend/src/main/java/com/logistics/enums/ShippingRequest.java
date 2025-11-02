package com.logistics.enums;

public class ShippingRequest {

    public enum ShippingRequestType {
        COMPLAINT,
        PICKUP_REMINDER,
        DELIVERY_REMINDER,
        CHANGE_ORDER_INFO,
        INQUIRY
    }

    public enum ShippingRequestStatus {
        PENDING,
        PROCESSING,
        RESOLVED,
        REJECTED,
        CANCELLED
    }
}