package com.logistics.controller.admin;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.admin.CreateOfficeRequest;
import com.logistics.request.admin.UpdateOfficeRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.OfficeAdminService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/offices")
@Tag(name = "Admin - Office", description = "Quản lý bưu cục")
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
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(officeAdminService.listOffices(page, limit, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOfficeById(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        Map<String, Object> result = officeAdminService.getOfficeById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @Audit(
            entity = EntityType.OFFICE,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.OFFICE_CREATE
    )
    public ResponseEntity<ApiResponse<String>> createOffice(@RequestBody CreateOfficeRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        officeAdminService.createOffice(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Tạo bưu cục thành công"));
    }

    @PutMapping("/{id}")
    @Audit(
            entity = EntityType.OFFICE,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.OFFICE_UPDATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> updateOffice(
            @PathVariable Integer id,
            @RequestBody UpdateOfficeRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        officeAdminService.updateOffice(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bưu cục thành công"));
    }

    @DeleteMapping("/{id}")
    @Audit(
            entity = EntityType.OFFICE,
            action = AuditLogAction.DELETE,
            description = AuditLogDescriptionConstant.OFFICE_DELETE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> deleteOffice(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        officeAdminService.deleteOffice(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa bưu cục thành công"));
    }
}
