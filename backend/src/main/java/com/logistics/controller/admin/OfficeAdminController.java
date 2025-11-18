package com.logistics.controller.admin;

import com.logistics.request.admin.CreateOfficeRequest;
import com.logistics.request.admin.UpdateOfficeRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.OfficeAdminService;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/offices")
public class OfficeAdminController {

    @Autowired
    private OfficeAdminService officeAdminService;

    private boolean isNotAdmin() {
        return !SecurityUtils.hasRole("admin");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listOffices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search) {

        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(officeAdminService.listOffices(page, limit, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOfficeById(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = officeAdminService.getOfficeById(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOffice(@RequestBody CreateOfficeRequest request) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = officeAdminService.createOffice(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOffice(
            @PathVariable Integer id,
            @RequestBody UpdateOfficeRequest request) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<Map<String, Object>> response = officeAdminService.updateOffice(id, request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteOffice(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        ApiResponse<String> response = officeAdminService.deleteOffice(id);
        if (!response.isSuccess()) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }
}


