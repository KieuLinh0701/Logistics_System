package com.logistics.controller.shipper;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.shipper.vehicle.ShipperVehicleSettingRequestDto;
import com.logistics.dto.shipper.vehicle.ShipperVehicleSettingResponseDto;
import com.logistics.dto.shipper.vehicle.ShipperVehicleStatusUpdateRequestDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.ShipperVehicleSettingService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipper/vehicle-setting")
@RequiredArgsConstructor
@Tag(name = "Shipper - Vehicle Setting", description = "Quản lý cài đặt phương tiện và cập nhật trạng thái hoạt động của nhân viên giao hàng")
public class ShipperVehicleSettingShipperController {

    private final ShipperVehicleSettingService shipperVehicleSettingService;

    private boolean isNotShipper() {
        return !SecurityUtils.hasRole("shipper");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ShipperVehicleSettingResponseDto>> getVehicleSetting() {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperVehicleSettingService.getMyVehicleSetting()));
    }

    @PutMapping
    @Audit(
            entity = EntityType.VEHICLE,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.SHIPPER_VEHICLE_SETTING_UPDATE
    )
    public ResponseEntity<ApiResponse<ShipperVehicleSettingResponseDto>> updateVehicleSetting(
            @RequestBody(required = false) ShipperVehicleSettingRequestDto request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperVehicleSettingService.updateMyVehicleSetting(request)));
    }

    @PutMapping("/status")
    public ResponseEntity<ApiResponse<ShipperVehicleSettingResponseDto>> updateVehicleStatus(
            @RequestBody(required = false) ShipperVehicleStatusUpdateRequestDto request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperVehicleSettingService.updateMyVehicleStatus(request)));
    }
}
