package com.logistics.controller.user;

import com.logistics.dto.user.dashboard.*;
import com.logistics.request.SearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.DashboardUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/dashboard")
@Tag(name = "User - Dashboard", description = "Cung cấp số liệu tổng quan về sản phẩm, đơn hàng, doanh thu và biểu đồ thống kê cho người dùng")
public class DashboardUserController {

    @Autowired
    private DashboardUserService service;

    @GetMapping("/overview/products")
    public ResponseEntity<ApiResponse<UserDashboardOverviewProductsResponseDTO>> getOverviewProducts(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getOverviewProducts(userId)));
    }

    @GetMapping("/overview/orders")
    public ResponseEntity<ApiResponse<UserOrderStatsDTO>> getOverviewOrders(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getOverviewOrders(userId)));
    }

    @GetMapping("/overview/revenue")
    public ResponseEntity<ApiResponse<UserRevenueStatsDTO>> getOverviewRevenue(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getOverviewRevenue(userId)));
    }

    @GetMapping("/chart/products")
    public ResponseEntity<ApiResponse<UserDashboardChartProductResponseDTO>> getChartProducts(
            SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getChartProducts(userId, searchRequest)));
    }

    @GetMapping("/chart/orders")
    public ResponseEntity<ApiResponse<List<UserOrderTimelineDTO>>> getChartOrders(
            SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getChartOrders(userId, searchRequest)));
    }
}