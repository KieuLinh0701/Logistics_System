package com.logistics.controller.admin;

import com.logistics.dto.admin.*;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.AdminService;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // USER MANAGEMENT

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search) {
        
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.listUsers(page, limit, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(@PathVariable Integer id) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.getUserById(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUser(@RequestBody CreateUserRequest request) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.createUser(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUser(
            @PathVariable Integer id,
            @RequestBody UpdateUserRequest request) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.updateUser(id, request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Integer id) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<String> response = adminService.deleteUser(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // OFFICE MANAGEMENT

    @GetMapping("/offices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listOffices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search) {
        
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.listOffices(page, limit, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/offices/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOfficeById(@PathVariable Integer id) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.getOfficeById(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/offices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOffice(@RequestBody CreateOfficeRequest request) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.createOffice(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/offices/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOffice(
            @PathVariable Integer id,
            @RequestBody UpdateOfficeRequest request) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.updateOffice(id, request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/offices/{id}")
    public ResponseEntity<ApiResponse<String>> deleteOffice(@PathVariable Integer id) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<String> response = adminService.deleteOffice(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // VEHICLE MANAGEMENT

    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listVehicles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search) {
        
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.listVehicles(page, limit, search);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vehicles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createVehicle(@RequestBody CreateVehicleRequest request) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.createVehicle(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateVehicle(
            @PathVariable Integer id,
            @RequestBody UpdateVehicleRequest request) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.updateVehicle(id, request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<ApiResponse<String>> deleteVehicle(@PathVariable Integer id) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<String> response = adminService.deleteVehicle(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // ORDER MANAGEMENT

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.listOrders(page, limit, search, status);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOrderStatus(
            @PathVariable Integer id,
            @RequestBody UpdateOrderStatusRequest request) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = adminService.updateOrderStatus(id, request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<String>> deleteOrder(@PathVariable Integer id) {
        if (!SecurityUtils.hasRole("admin")) {
            return ResponseEntity.status(403).body(
                new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<String> response = adminService.deleteOrder(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }
}

