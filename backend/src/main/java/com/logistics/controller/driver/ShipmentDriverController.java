package com.logistics.controller.driver;

import com.logistics.request.driver.FinishShipmentRequest;
import com.logistics.request.driver.UpdateVehicleTrackingRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.driver.ShipmentDriverService;
import com.logistics.utils.SecurityUtils;
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
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipmentDriverService.startShipment(id));
    }

    @PostMapping("/finish")
    public ResponseEntity<ApiResponse<String>> finishShipment(@RequestBody FinishShipmentRequest request) {
        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipmentDriverService.finishShipment(request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShipments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(shipmentDriverService.getShipments(page, limit));
    }

    @GetMapping("/route")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoute() {
        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipmentDriverService.getRoute());
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(shipmentDriverService.getHistory(page, limit));
    }

    @PostMapping("/tracking")
    public ResponseEntity<ApiResponse<String>> updateVehicleTracking(@RequestBody UpdateVehicleTrackingRequest request) {
        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipmentDriverService.updateVehicleTracking(request));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVehicleTracking(@PathVariable Integer id) {
        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipmentDriverService.getVehicleTracking(id));
    }
}




