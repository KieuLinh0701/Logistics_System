package com.logistics.controller.user;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.user.role.RoleDetailUserDto;
import com.logistics.dto.user.role.RoleListUserDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.user.role.RoleSearchUserRequest;
import com.logistics.request.user.role.RoleUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.RoleUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/roles")
@Tag(name = "User - Role", description = "Quản lý phân quyền và vai trò người dùng trong hệ thống (thêm, sửa, xóa, liệt kê danh sách vai trò)")
public class RoleUserController {

    @Autowired
    private RoleUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<RoleListUserDto>>> list(
            @Valid RoleSearchUserRequest roleSearchUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, roleSearchUserRequest)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<RoleListUserDto>>> findAll(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.findAll(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDetailUserDto>> detail(
            @PathVariable int id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.detail(userId, id)));
    }

    @PostMapping
    @Audit(
            entity = EntityType.ROLE,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.ROLE_CREATE
    )
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @RequestBody RoleUserRequest roleUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.create(userId, roleUserRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}")
    @Audit(
            entity = EntityType.ROLE,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.ROLE_UPDATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable int id,
            @Valid @RequestBody RoleUserRequest roleUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.update(userId, id, roleUserRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @Audit(
            entity = EntityType.ROLE,
            action = AuditLogAction.DELETE,
            description = AuditLogDescriptionConstant.ROLE_DELETE
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable int id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}