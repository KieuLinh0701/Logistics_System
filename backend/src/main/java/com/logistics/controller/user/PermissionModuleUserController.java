package com.logistics.controller.user;

import com.logistics.dto.user.role.PermissionModuleDto;
import com.logistics.dto.user.role.RoleListUserDto;
import com.logistics.request.user.role.RoleSearchUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.PermissionModuleUserService;
import com.logistics.service.user.RoleUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/permission-modules")
public class PermissionModuleUserController {

    @Autowired
    private PermissionModuleUserService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PermissionModuleDto>>> activeList() {

        return ResponseEntity.ok(service.activeList());
    }
}