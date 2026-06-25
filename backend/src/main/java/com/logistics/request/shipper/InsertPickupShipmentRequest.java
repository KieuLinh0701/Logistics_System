package com.logistics.request.shipper;

import lombok.Data;

/**
 * Phase 3C: Insert pickup order trực tiếp vào Shipment (ShipmentOrder là source of truth).
 *
 * Endpoint: POST /api/shipper/shipments/{shipmentId}/pickup-insert
 *
 * Flow cũ {@link PickupInsertionRequest} (targetRouteId-based) vẫn còn cho AI fallback,
 * nhưng khi routeInfo.source === "SHIPMENT" frontend phải dùng request này.
 */
@Data
public class InsertPickupShipmentRequest {
    /**
     * ID của đơn hàng cần pickup.
     * Order phải là PICKUP_BY_COURIER và đang ở status CONFIRMED/READY_FOR_PICKUP/PICKUP_RETRY.
     */
    private Integer pickupOrderId;
}