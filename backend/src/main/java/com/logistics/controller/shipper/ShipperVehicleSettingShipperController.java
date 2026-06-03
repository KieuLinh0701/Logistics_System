package com.logistics.controller.shipper;

import com.logistics.dto.shipper.vehicle.ShipperVehicleSettingRequestDto;
import com.logistics.dto.shipper.vehicle.ShipperVehicleSettingResponseDto;
import com.logistics.dto.shipper.vehicle.ShipperVehicleStatusUpdateRequestDto;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.ShipperVehicleSettingService;
import com.logistics.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipper/vehicle-setting")
@RequiredArgsConstructor
public class ShipperVehicleSettingShipperController {

    private final ShipperVehicleSettingService shipperVehicleSettingService;

    private boolean isNotShipper() {
        return !SecurityUtils.hasRole("shipper");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ShipperVehicleSettingResponseDto>> getVehicleSetting() {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipperVehicleSettingService.getMyVehicleSetting());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ShipperVehicleSettingResponseDto>> updateVehicleSetting(
            @RequestBody(required = false) ShipperVehicleSettingRequestDto request) {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipperVehicleSettingService.updateMyVehicleSetting(request));
    }

    @PutMapping("/status")
    public ResponseEntity<ApiResponse<ShipperVehicleSettingResponseDto>> updateVehicleStatus(
            @RequestBody(required = false) ShipperVehicleStatusUpdateRequestDto request) {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipperVehicleSettingService.updateMyVehicleStatus(request));
    }
}
