package com.logistics.request.shipper;

import lombok.Data;

@Data
public class PickupInsertionRequest {
    private Integer pickupOrderId;
    private Integer targetShipperEmployeeId;
    private Long targetRouteId;
    private Boolean autoAssign = false;
    private Boolean reOptimizeAfterInsert = false;
}
