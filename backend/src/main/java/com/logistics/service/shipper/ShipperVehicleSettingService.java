package com.logistics.service.shipper;

import com.logistics.dto.shipper.vehicle.ShipperVehicleSettingRequestDto;
import com.logistics.dto.shipper.vehicle.ShipperVehicleSettingResponseDto;
import com.logistics.dto.shipper.vehicle.ShipperVehicleStatusUpdateRequestDto;

public interface ShipperVehicleSettingService {

    ShipperVehicleSettingResponseDto getMyVehicleSetting();

    ShipperVehicleSettingResponseDto updateMyVehicleSetting(ShipperVehicleSettingRequestDto request);

    ShipperVehicleSettingResponseDto updateMyVehicleStatus(ShipperVehicleStatusUpdateRequestDto request);
}
