package com.logistics.request.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVehicleTrackingRequest {
    private Integer shipmentId;
    private Double latitude;
    private Double longitude;
    private Double speed;
}




