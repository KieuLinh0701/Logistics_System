package com.logistics.controller.user;

import com.logistics.dto.user.employee.EmployeeByRoleIdListUserDto;
import com.logistics.dto.user.employee.EmployeeListUserDto;
import com.logistics.dto.user.employee.ShopWorkHistoryListUserDto;
import com.logistics.request.user.employee.CreateEmployeeUserRequest;
import com.logistics.request.user.employee.EmployeeByRoleIdSearchUserRequest;
import com.logistics.request.user.employee.EmployeeSearchUserRequest;
import com.logistics.request.user.employee.ShopWorkHistorySearchUserRequest;
import com.logistics.request.user.employee.UpdateEmployeeUserRequest;
import com.logistics.request.user.employee.UpdateIsActiveUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.EmployeeUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/employees")
public class EmployeeUserController {

    @Autowired
    private EmployeeUserService service;

    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<ListResponse<EmployeeByRoleIdListUserDto>>> listByRoleId(
            @PathVariable Integer roleId,
            @Valid EmployeeByRoleIdSearchUserRequest employeeByRoleIdSearchUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.listByRoleId(userId, roleId, employeeByRoleIdSearchUserRequest));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<EmployeeListUserDto>>> list(
            @Valid EmployeeSearchUserRequest employeeSearchUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.list(userId, employeeSearchUserRequest));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<Void>> updateIsActive(
            @PathVariable Integer id,
            @RequestBody UpdateIsActiveUserRequest updateIsActiveUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.updateIsActive(userId, id, updateIsActiveUserRequest));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @RequestBody CreateEmployeeUserRequest createEmployeeUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.createEmployee(userId, createEmployeeUserRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable int id,
            @Valid @RequestBody UpdateEmployeeUserRequest updateEmployeeUserRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.updateEmployee(userId, id, updateEmployeeUserRequest));
    }

    @GetMapping("/{id}/work-history")
    public ResponseEntity<ApiResponse<ListResponse<ShopWorkHistoryListUserDto>>> listWorkHistory(
            @PathVariable int id,
            @Valid ShopWorkHistorySearchUserRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.listWorkHistory(userId, id, searchRequest));
    }
}