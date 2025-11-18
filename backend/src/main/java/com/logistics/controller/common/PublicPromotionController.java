package com.logistics.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.request.common.promotion.PublicPromotionRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.PromotionResponse;
import com.logistics.service.common.PublicPromotionService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/public/promotions")
public class PublicPromotionController {

    @Autowired
    private PublicPromotionService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PromotionResponse>> getActivePromotions(@Valid PublicPromotionRequest request) {
        System.out.println("Hello");
        ApiResponse<PromotionResponse> result = service.getActivePromotions(request);
        return ResponseEntity.ok(result);
    }
}