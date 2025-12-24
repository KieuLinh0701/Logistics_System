package com.logistics.controller.admin;

import com.logistics.request.admin.CreateVehicleRequest;
import com.logistics.request.admin.UpdateVehicleRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.VehicleAdminService;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/vehicles")
public class VehicleAdminController {

    @Autowired
    private VehicleAdminService vehicleAdminService;

    private boolean isNotAdmin() {
        return !SecurityUtils.hasRole("admin");
    }

        @GetMapping
        public ResponseEntity<ApiResponse<Map<String, Object>>> listVehicles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {

        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(vehicleAdminService.listVehicles(page, limit, search, type, status));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createVehicle(@RequestBody CreateVehicleRequest request) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = vehicleAdminService.createVehicle(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateVehicle(
            @PathVariable Integer id,
            @RequestBody UpdateVehicleRequest request) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = vehicleAdminService.updateVehicle(id, request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteVehicle(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<String> response = vehicleAdminService.deleteVehicle(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVehicleById(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        ApiResponse<Map<String, Object>> response = vehicleAdminService.getVehicleById(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/trackings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVehicleTrackings(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        ApiResponse<Map<String, Object>> response = vehicleAdminService.getVehicleTrackings(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }
}


