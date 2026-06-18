package com.logistics.controller.driver;

import com.logistics.request.driver.FinishShipmentRequest;
import com.logistics.request.driver.UpdateVehicleTrackingRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.driver.ShipmentDriverService;
import com.logistics.utils.SecurityUtils;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/driver/shipments")
public class ShipmentDriverController {

    @Autowired
    private ShipmentDriverService shipmentDriverService;

    private boolean isNotDriver() {
        return !SecurityUtils.hasRole("driver");
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<String>> startShipment(@PathVariable Integer id) {
        if (isNotDriver()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipmentDriverService.startShipment(id);
        return ResponseEntity.ok(ApiResponse.success("Đã bắt đầu vận chuyển"));
    }
    @PostMapping("/finish")
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
