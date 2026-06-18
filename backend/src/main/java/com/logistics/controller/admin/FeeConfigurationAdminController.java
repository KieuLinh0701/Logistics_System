package com.logistics.controller.admin;

import com.logistics.request.admin.CreateFeeConfigurationRequest;
import com.logistics.request.admin.UpdateFeeConfigurationRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.FeeConfigurationAdminService;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/fee-configurations")
public class FeeConfigurationAdminController {

    @Autowired
    private FeeConfigurationAdminService feeConfigurationAdminService;

    private boolean isNotAdmin() {
        return !SecurityUtils.hasRole("admin");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFeeConfigurations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String feeType,
            @RequestParam(required = false) Integer serviceTypeId,
            @RequestParam(required = false) Boolean active) {

        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(ApiResponse.success(feeConfigurationAdminService.listFeeConfigurations(page, limit, search, feeType, serviceTypeId, active)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeeConfigurationById(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(ApiResponse.success(feeConfigurationAdminService.getFeeConfigurationById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createFeeConfiguration(@RequestBody CreateFeeConfigurationRequest request) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        feeConfigurationAdminService.createFeeConfiguration(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Tạo cấu hình phí thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateFeeConfiguration(
            @PathVariable Integer id,
            @RequestBody UpdateFeeConfigurationRequest request) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        feeConfigurationAdminService.updateFeeConfiguration(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình phí thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFeeConfiguration(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        feeConfigurationAdminService.deleteFeeConfiguration(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cấu hình phí thành công"));
    }
}
