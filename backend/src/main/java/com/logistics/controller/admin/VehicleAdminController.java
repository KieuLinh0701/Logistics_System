package com.logistics.controller.admin;

import com.logistics.request.admin.CreateVehicleRequest;
import com.logistics.request.admin.UpdateVehicleRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.VehicleAdminService;
import com.logistics.utils.SecurityUtils;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
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
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(vehicleAdminService.listVehicles(page, limit, search, type, status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createVehicle(@RequestBody CreateVehicleRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        vehicleAdminService.createVehicle(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Tạo phương tiện thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateVehicle(
            @PathVariable Integer id,
            @RequestBody UpdateVehicleRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        vehicleAdminService.updateVehicle(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phương tiện thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteVehicle(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        vehicleAdminService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phương tiện thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVehicleById(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(vehicleAdminService.getVehicleById(id)));
    }

    @GetMapping("/{id}/trackings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVehicleTrackings(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(vehicleAdminService.getVehicleTrackings(id)));
    }
}
