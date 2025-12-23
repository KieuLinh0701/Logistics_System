package com.logistics.controller.shipper;

import com.logistics.request.shipper.CreateIncidentReportRequest;
import com.logistics.request.shipper.UpdateDeliveryStatusRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.OrderShipperService;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipper")
public class OrderShipperController {

    @Autowired
    private OrderShipperService shipperService;

    private boolean isNotShipper() {
        return !SecurityUtils.hasRole("shipper");
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipperService.getDashboard());
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(shipperService.listOrders(page, limit, status, search));
    }

    @GetMapping("/orders-unassigned")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listUnassignedOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(shipperService.listUnassignedOrders(page, limit));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrder(@PathVariable Integer id) {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipperService.getOrderById(id));
    }

    @PostMapping("/orders/{id}/claim")
    public ResponseEntity<ApiResponse<String>> claimOrder(@PathVariable Integer id) {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipperService.claimOrder(id));
    }

    @PostMapping("/orders/{id}/unclaim")
    public ResponseEntity<ApiResponse<String>> unclaimOrder(@PathVariable Integer id) {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipperService.unclaimOrder(id));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateDeliveryStatus(
            @PathVariable Integer id,
            @RequestBody UpdateDeliveryStatusRequest request) {

        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(shipperService.updateDeliveryStatus(id, request));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeliveryHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status) {

        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(shipperService.getDeliveryHistory(page, limit, status));
    }

    @PostMapping("/incident")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createIncident(
            @RequestBody CreateIncidentReportRequest request) {

        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(shipperService.createIncidentReport(request));
    }

    @GetMapping("/incidents")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listIncidents() {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(shipperService.listIncidentReports());
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIncident(@PathVariable Integer id) {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(shipperService.getIncidentDetail(id));
    }

    @GetMapping("/route")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeliveryRoute() {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(shipperService.getDeliveryRoute());
    }

    @PostMapping("/route/start")
    public ResponseEntity<ApiResponse<String>> startRoute(@RequestBody Map<String, Object> request) {
        if (isNotShipper()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        Integer routeId = (Integer) request.get("routeId");
        return ResponseEntity.ok(shipperService.startRoute(routeId));
    }
}
