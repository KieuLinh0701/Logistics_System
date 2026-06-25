package com.logistics.request.shipper;

import lombok.Data;

@Data
public class ShipperReOptimizeRequest {
    /** AiRoutePlanRoute.id (legacy AI-source route) */
    private Long routeId;
    /** Shipment-based route: Shipment.id — dùng khi routeInfo.source === "SHIPMENT" */
    private Integer shipmentId;
    private Double currentLatitude;
    private Double currentLongitude;
    private String currentAddress;
    private Boolean includeRemainingStopsOnly = true;
    private Boolean returnToOffice = true;
    private String reason; // MANUAL, PICKUP_INSERTION, GPS_DEVIATION, TRAFFIC
}
