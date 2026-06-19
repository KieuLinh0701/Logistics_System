package com.logistics.controller.manager;

import com.logistics.dto.manager.dashboard.ManagerDashboardOverviewResponseDTO;
import com.logistics.response.ApiResponse;
import com.logistics.service.manager.DashboardManagerService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/dashboard")
public class DashboardManagerController {

    @Autowired
    private DashboardManagerService service;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<ManagerDashboardOverviewResponseDTO>> getOverview(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getOverview(userId)));
    }
}