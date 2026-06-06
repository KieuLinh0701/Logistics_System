package com.logistics.enums;

public enum OrderStatus {
    DRAFT,
    PENDING,
    CONFIRMED,
    READY_FOR_PICKUP,
    PICKUP_PENDING,
    PICKUP_SUCCESS,
    PICKUP_RETRY,
    PICKUP_FAILED_FINAL,
    PICKING_UP, // Thêm này báo Chuẩn bị lấy hàng sau khi admin confirm thì shipper có thể nhận
                // rồi và đợi ready for pickup là lấy hàng
    PICKED_UP,
    AT_ORIGIN_OFFICE, // Thêm này báo Hàng đã về bưu cục gửi
    IN_TRANSIT,
    AT_DEST_OFFICE, // Thêm này báo Hàng đã đến trạm giao hàng tại khu vực người nhận lúc này mới
                    // lưu toOffice
    DELIVERING,
    DELIVERED,
    PARTIAL_DELIVERY,
    PARTIAL_RETURN,
    FAILED_DELIVERY, // Thêm này báo Giao không thành công
    DELIVERY_RETRY,
    DELIVERY_FAILED_FINAL,
    CANCELLED,
    RETURNING,
    RETURNED
}