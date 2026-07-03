package com.logistics.controller.user;

import com.logistics.dto.user.role.PermissionModuleDto;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.PermissionModuleUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/permission-modules")
@Tag(name = "User - Permission Module", description = "Quản lý và truy xuất danh sách các module quyền hạn khả dụng trong hệ thống")
public class PermissionModuleUserController {

    private final PermissionModuleUserService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PermissionModuleDto>>> activeList() {

        return ResponseEntity.ok(ApiResponse.success(service.activeList()));
    }
}