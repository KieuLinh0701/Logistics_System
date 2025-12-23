package com.logistics.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.user.UserPromotionDto;
import com.logistics.request.user.promotion.PromotionUserRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.PromotionUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/user/promotions")
public class PromotionUserController {

    @Autowired
    private PromotionUserService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ListResponse<UserPromotionDto>>> getActiveUserPromotions(
            @Valid PromotionUserRequest promotionUserRequest, 
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        
        ApiResponse<ListResponse<UserPromotionDto>> result = service.getActiveUserPromotions(userId, promotionUserRequest);
        return ResponseEntity.ok(result);
    }
}