package com.logistics.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.request.promotion.PublicPromotionRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.PromotionResponse;
import com.logistics.service.PromotionService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/public/promotions")
public class PublicPromotionController {

    @Autowired
    private PromotionService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PromotionResponse>> getActivePromotions(@Valid PublicPromotionRequest request) {
        System.out.println("Hello");
        ApiResponse<PromotionResponse> result = service.getActivePromotions(request);
        return ResponseEntity.ok(result);
    }
}