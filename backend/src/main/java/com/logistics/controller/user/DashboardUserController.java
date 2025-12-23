package com.logistics.controller.user;

import com.logistics.dto.user.dashboard.UserDashboardChartResponseDTO;
import com.logistics.dto.user.dashboard.UserDashboardOverviewResponseDTO;
import com.logistics.request.SearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.DashboardUserService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/dashboard")
public class DashboardUserController {

    @Autowired
    private DashboardUserService service;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<UserDashboardOverviewResponseDTO>> getOverview(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.getOverview(userId));
    }

    @GetMapping("/chart")
    public ResponseEntity<ApiResponse<UserDashboardChartResponseDTO>> getChart(
            SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.getChart(userId, searchRequest));
    }
}