package com.logistics.controller.user;

import com.logistics.dto.user.employee.EmployeeByRoleIdListUserDto;
import com.logistics.dto.user.employee.EmployeeListUserDto;
import com.logistics.dto.user.employee.ShopWorkHistoryListUserDto;
import com.logistics.request.user.employee.*;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.EmployeeUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/employees")
@Tag(name = "User - Employee", description = "Quản lý nhân sự, cập nhật trạng thái, thông tin công việc và lịch sử làm việc của nhân viên tại bưu cục")
public class EmployeeUserController {

    @Autowired
    private EmployeeUserService service;

    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<ListResponse<EmployeeByRoleIdListUserDto>>> listByRoleId(
            @PathVariable Integer roleId,
            @Valid EmployeeByRoleIdSearchUserRequest employeeByRoleIdSearchUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.listByRoleId(userId, roleId, employeeByRoleIdSearchUserRequest)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<EmployeeListUserDto>>> list(
            @Valid EmployeeSearchUserRequest employeeSearchUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, employeeSearchUserRequest)));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<Void>> updateIsActive(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateIsActiveUserRequest updateIsActiveUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.updateIsActive(userId, id, updateIsActiveUserRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @RequestBody CreateEmployeeUserRequest createEmployeeUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.createEmployee(userId, createEmployeeUserRequest);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable int id,
            @Valid @RequestBody UpdateEmployeeUserRequest updateEmployeeUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.updateEmployee(userId, id, updateEmployeeUserRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/work-history")
    public ResponseEntity<ApiResponse<ListResponse<ShopWorkHistoryListUserDto>>> listWorkHistory(
            @PathVariable int id,
            @Valid ShopWorkHistorySearchUserRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.listWorkHistory(userId, id, searchRequest)));
    }
}