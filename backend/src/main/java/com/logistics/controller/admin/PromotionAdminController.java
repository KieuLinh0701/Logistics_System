package com.logistics.controller.admin;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.admin.CreatePromotionRequest;
import com.logistics.request.admin.UpdatePromotionRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.PromotionAdminService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/promotions")
@Tag(name = "Admin - Promotion", description = "Quản lý chương trình khuyến mãi")
public class PromotionAdminController {

    @Autowired
    private PromotionAdminService promotionAdminService;

    private boolean isNotAdmin() {
        return !SecurityUtils.hasRole("admin");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listPromotions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isGlobal) {

        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(promotionAdminService.listPromotions(page, limit, search, status, isGlobal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPromotionById(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(promotionAdminService.getPromotionById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createPromotion(@RequestBody CreatePromotionRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        promotionAdminService.createPromotion(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Tạo khuyến mãi thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updatePromotion(
            @PathVariable Integer id,
            @RequestBody UpdatePromotionRequest request) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        promotionAdminService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khuyến mãi thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePromotion(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        promotionAdminService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa khuyến mãi thành công"));
    }
}
