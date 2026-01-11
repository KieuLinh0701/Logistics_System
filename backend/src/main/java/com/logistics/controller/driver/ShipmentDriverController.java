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

    @PostMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<String>> claimShipment(@PathVariable Integer id) {
        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipmentDriverService.claimShipment(id));
    }
    @PostMapping("/{id}/mark-picked-up")
    public ResponseEntity<ApiResponse<String>> markShipmentPickedUp(@PathVariable Integer id) {
        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipmentDriverService.markShipmentPickedUp(id));
    }
    @PostMapping("/{id}/pickup")
    public ResponseEntity<ApiResponse<String>> pickupShipmentOrders(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        try {
            Object o = body.get("orderIds");
            java.util.List<Integer> orderIds = new java.util.ArrayList<>();
            if (o instanceof java.util.List) {
                for (Object el : (java.util.List<?>) o) {
                    try { orderIds.add(Integer.parseInt(el.toString())); } catch (Exception ignored) {}
                }
            }
            return ResponseEntity.ok(shipmentDriverService.pickupShipmentOrders(id, orderIds));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(false, e.getMessage(), null));
        }
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




