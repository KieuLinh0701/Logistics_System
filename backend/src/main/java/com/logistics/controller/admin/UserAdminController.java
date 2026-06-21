package com.logistics.controller.admin;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.BaseAuditLogDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.repository.RoleRepository;
import com.logistics.request.admin.CreateUserRequest;
import com.logistics.request.admin.UpdateUserRequest;
import com.logistics.request.manager.audit.AuditLogSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.admin.UserAdminService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin - User", description = "Quản lý người dùng và phân quyền")
public class UserAdminController {

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private RoleRepository roleRepository;

    private boolean isNotAdmin() {
        return !SecurityUtils.hasRole("admin");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {

        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(userAdminService.listUsers(page, limit, search, status, role)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(userAdminService.getUserById(id)));
    }

    @PostMapping
    @Audit(
            entity = EntityType.USER,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.USER_CREATE
    )
    public ResponseEntity<ApiResponse<String>> createUser(@RequestBody CreateUserRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        userAdminService.createUser(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Tạo người dùng thành công"));
    }

    @PutMapping("/{id}")
    @Audit(
            entity = EntityType.USER,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.USER_UPDATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> updateUser(
            @PathVariable Integer id,
            @RequestBody UpdateUserRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        userAdminService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công"));
    }

    @DeleteMapping("/{id}")
    @Audit(
            entity = EntityType.USER,
            action = AuditLogAction.DELETE,
            description = AuditLogDescriptionConstant.USER_DELETE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        userAdminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công"));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> listRoles() {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        var roles = roleRepository.findAll().stream()
                .map(r -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", r.getId());
                    m.put("name", r.getName());
                    return m;
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<ApiResponse<ListResponse<BaseAuditLogDto>>> listAuditLogsByUserId(
            @PathVariable Integer id,
            @Valid AuditLogSearchRequest auditLogSearchRequest) {

        ListResponse<BaseAuditLogDto> result = userAdminService.listAuditLogsByUserId(id, auditLogSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @GetMapping("/{id}/logs/export")
    @Audit(
            entity = EntityType.AUDIT_LOG,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.AUDIT_LOG_EXPORT_BY_USER
    )
    public ResponseEntity<byte[]> export(
            @PathVariable Integer id,
            AuditLogSearchRequest auditLogSearchRequest) throws Exception {

        byte[] data = userAdminService.export(id, auditLogSearchRequest);

        String fileName = "UTE Logistics_Báo cáo lịch sử hoạt động của người dùng.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}
