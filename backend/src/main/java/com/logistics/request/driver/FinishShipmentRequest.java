package com.logistics.request.driver;

import com.logistics.enums.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinishShipmentRequest {
    private Integer shipmentId;
    private ShipmentStatus status; // COMPLETED hoặc CANCELLED
}




