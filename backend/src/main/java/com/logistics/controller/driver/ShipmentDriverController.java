package com.logistics.controller.driver;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.driver.FinishShipmentRequest;
import com.logistics.request.driver.UpdateVehicleTrackingRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.driver.ShipmentDriverService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/driver/shipments")
@Tag(name = "Driver - Shipment", description = "Quản lý hành trình, lộ trình và cập nhật vị trí đơn hàng của tài xế")
public class ShipmentDriverController {

    @Autowired
    private ShipmentDriverService shipmentDriverService;

    private boolean isNotDriver() {
        return !SecurityUtils.hasRole("driver");
    }

    @PostMapping("/{id}/start")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.SHIPMENT_START,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> startShipment(@PathVariable Integer id) {
        if (isNotDriver()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipmentDriverService.startShipment(id);
        return ResponseEntity.ok(ApiResponse.success("Đã bắt đầu vận chuyển"));
    }

    @PostMapping("/finish")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.SHIPMENT_FINISH
    )
    public ResponseEntity<ApiResponse<String>> finishShipment(@RequestBody FinishShipmentRequest request) {
        if (isNotDriver()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipmentDriverService.finishShipment(request);
        return ResponseEntity.ok(ApiResponse.success("Đã hoàn tất chuyến hàng"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShipments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        if (isNotDriver()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(shipmentDriverService.getShipments(page, limit)));
    }

    @GetMapping("/route")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoute() {
        if (isNotDriver()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipmentDriverService.getRoute()));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        if (isNotDriver()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(shipmentDriverService.getHistory(page, limit)));
    }

    @PostMapping("/tracking")
    @Audit(
            entity = EntityType.VEHICLE_TRACKING,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.VEHICLE_TRACKING_UPDATE
    )
    public ResponseEntity<ApiResponse<String>> updateVehicleTracking(@RequestBody UpdateVehicleTrackingRequest request) {
        if (isNotDriver()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipmentDriverService.updateVehicleTracking(request);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật vị trí"));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVehicleTracking(@PathVariable Integer id) {
        if (isNotDriver()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipmentDriverService.getVehicleTracking(id)));
    }
}
