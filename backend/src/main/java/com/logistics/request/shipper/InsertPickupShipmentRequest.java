package com.logistics.request.shipper;

import lombok.Data;

@Data
public class InsertPickupShipmentRequest {
    
    // ID của đơn hàng cần pickup.
    // Order phải là PICKUP_BY_COURIER và đang ở status CONFIRMED/READY_FOR_PICKUP/PICKUP_RETRY.
    private Integer pickupOrderId;
}