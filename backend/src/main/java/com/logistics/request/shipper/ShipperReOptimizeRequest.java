package com.logistics.request.shipper;

import lombok.Data;

@Data
public class ShipperReOptimizeRequest {
    private Long routeId;
    private Double currentLatitude;
    private Double currentLongitude;
    private String currentAddress;
    private Boolean includeRemainingStopsOnly = true;
    private Boolean returnToOffice = true;
    private String reason; // MANUAL, PICKUP_INSERTION, GPS_DEVIATION, TRAFFIC
}
