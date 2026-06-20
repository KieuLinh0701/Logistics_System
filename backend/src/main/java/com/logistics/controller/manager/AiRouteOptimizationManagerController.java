package com.logistics.controller.manager;

import com.logistics.dto.manager.ai.ManagerAiRoutePlanDetailDto;
import com.logistics.dto.manager.ai.ManagerAiRoutePlanSummaryDto;
import com.logistics.request.manager.ai.ManagerAiOptimizeRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.manager.AiRouteOptimizationManagerService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/ai-routes")
@RequiredArgsConstructor
@Tag(name = "Manager - AI Route Optimization", description = "Quản lý tối ưu hóa lộ trình giao hàng bằng AI cho bưu cục")
public class AiRouteOptimizationManagerController {

    private final AiRouteOptimizationManagerService service;

    private boolean denyManager() {
        return !SecurityUtils.hasRole("manager");
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> preview(HttpServletRequest request) {
        if (denyManager()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        }
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.previewDeliveryReadyOrders(userId)));
    }

    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<ManagerAiRoutePlanDetailDto>> optimize(
            HttpServletRequest request,
            @RequestBody(required = false) ManagerAiOptimizeRequest body) {
        if (denyManager()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        }
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.optimize(userId, body)));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<ManagerAiRoutePlanSummaryDto>>> listPlans(HttpServletRequest request) {
        if (denyManager()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        }
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.listPlans(userId)));
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<ManagerAiRoutePlanDetailDto>> getPlan(
            HttpServletRequest request,
            @PathVariable Long planId) {
        if (denyManager()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        }
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.getPlan(userId, planId)));
    }

    @PostMapping("/plans/{planId}/confirm")
    public ResponseEntity<ApiResponse<ManagerAiRoutePlanDetailDto>> confirm(
            HttpServletRequest request,
            @PathVariable Long planId) {
        if (denyManager()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        }
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.confirmPlan(userId, planId)));
    }

    @PostMapping("/plans/{planId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            HttpServletRequest request,
            @PathVariable Long planId) {
        if (denyManager()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        }
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.cancelPlan(userId, planId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
