package com.logistics.controller.admin;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.admin.CreateServiceTypeRequest;
import com.logistics.request.admin.UpdateServiceTypeRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.ServiceTypeAdminService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/service-types")
@Tag(name = "Admin - Service Type", description = "Quản lý loại dịch vụ vận chuyển")
public class ServiceTypeAdminController {

    @Autowired
    private ServiceTypeAdminService serviceTypeAdminService;

    private boolean isNotAdmin() {
        return !SecurityUtils.hasRole("admin");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listServiceTypes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search) {

        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(serviceTypeAdminService.listServiceTypes(page, limit, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServiceTypeById(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(serviceTypeAdminService.getServiceTypeById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createServiceType(@RequestBody CreateServiceTypeRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        serviceTypeAdminService.createServiceType(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Tạo loại dịch vụ thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateServiceType(
            @PathVariable Integer id,
            @RequestBody UpdateServiceTypeRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        serviceTypeAdminService.updateServiceType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật loại dịch vụ thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteServiceType(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        serviceTypeAdminService.deleteServiceType(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa loại dịch vụ thành công"));
    }
}
