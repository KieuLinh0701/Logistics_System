package com.logistics.controller.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.request.manager.employee.ManagerEmployeeEditRequest;
import com.logistics.request.manager.employee.ManagerEmployeeSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.EmployeeManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/manager/employees")
public class EmployeeManagerController {

    @Autowired
    private EmployeeManagerService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeeListDto>>> list(
            @Valid ManagerEmployeeSearchRequest managerShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<ListResponse<ManagerEmployeeListDto>> result = service.list(userId,
                managerShippingRequestSearchRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(
            @RequestBody ManagerEmployeeEditRequest managerEmployeeEditRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.createEmployee(userId, managerEmployeeEditRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Integer id,
            @RequestBody ManagerEmployeeEditRequest managerEmployeeEditRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.updateEmployee(userId, id, managerEmployeeEditRequest));
    }
}