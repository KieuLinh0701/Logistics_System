package com.logistics.controller.manager;

import com.logistics.dto.OfficeDto;
import com.logistics.request.manager.ManagerOfficeEditRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.manager.OfficeManagerService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/offices")
public class OfficeManagerController {

    @Autowired
    private OfficeManagerService service;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<OfficeDto>> getMyOffice(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(service.getMyOffice(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Boolean>> updateMyOffice(HttpServletRequest request,
    @RequestBody ManagerOfficeEditRequest managerOfficeEditRequest
    ) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(service.updateMyOffice(userId, managerOfficeEditRequest));
    }

    @GetMapping("/me/city-code")
    public ResponseEntity<ApiResponse<Integer>> getMyOfficeCityCode(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(service.getMyOfficeCityCode(userId));
    }
}
