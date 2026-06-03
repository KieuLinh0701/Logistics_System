package com.logistics.dto.shipper.vehicle;

import com.logistics.enums.ShipperVehicleType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipperVehicleSettingRequestDto {
    private ShipperVehicleType vehicleType;
    private Integer maxOrders;
    private Integer maxWeightKg;
    private Integer batteryLevel;
    private String notes;
}
