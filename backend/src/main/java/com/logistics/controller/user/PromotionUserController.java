package com.logistics.controller.user;

import com.logistics.dto.user.UserPromotionDto;
import com.logistics.request.user.promotion.PromotionUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.PromotionUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/promotions")
@Tag(name = "User - Promotion", description = "Quản lý và truy vấn các chương trình khuyến mãi hiện hành dành cho người dùng")
public class PromotionUserController {

    @Autowired
    private PromotionUserService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ListResponse<UserPromotionDto>>> getActiveUserPromotions(
            @Valid PromotionUserRequest promotionUserRequest, 
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        ListResponse<UserPromotionDto> result = service.getActiveUserPromotions(userId, promotionUserRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}