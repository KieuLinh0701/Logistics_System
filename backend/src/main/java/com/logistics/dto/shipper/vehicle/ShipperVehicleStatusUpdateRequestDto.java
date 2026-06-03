package com.logistics.dto.shipper.vehicle;

import com.logistics.enums.ShipperVehicleStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipperVehicleStatusUpdateRequestDto {
    private ShipperVehicleStatus status;
}
