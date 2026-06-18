package com.logistics.controller.admin;

import com.logistics.request.admin.CreateUserRequest;
import com.logistics.request.admin.UpdateUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.UserAdminService;
import com.logistics.utils.SecurityUtils;
import com.logistics.repository.RoleRepository;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
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
    public ResponseEntity<ApiResponse<String>> createUser(@RequestBody CreateUserRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        userAdminService.createUser(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Tạo người dùng thành công"));
    }

    @PutMapping("/{id}")
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
}
