package com.logistics.controller.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.request.manager.employee.ManagerEmployeeSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.EmployeeManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;

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

    // @GetMapping("/{id}")
    // public ResponseEntity<ApiResponse<ManagerShippingRequestDetailDto>> getShippingRequestById(
    //         @PathVariable Integer id,
    //         HttpServletRequest request) {
    //     Integer userId = (Integer) request.getAttribute("currentUserId");

    //     ApiResponse<ManagerShippingRequestDetailDto> result = service.getShippingRequestById(userId, id);
    //     return ResponseEntity.ok(result);
    // }

    // @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<ApiResponse<Boolean>> processing(@PathVariable Integer id,
    //         @ModelAttribute ManagerShippingRequestForm managerShippingRequestForm,
    //         HttpServletRequest request) {
    //     Integer userId = (Integer) request.getAttribute("currentUserId");

    //     return ResponseEntity.ok(service.processing(userId, id, managerShippingRequestForm));
    // }
}