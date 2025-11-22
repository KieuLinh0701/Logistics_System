package com.logistics.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.PromotionDto;
import com.logistics.request.common.promotion.PromotionPublicRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.common.PromotionPublicService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/public/promotions")
public class PromotionPublicController {

    @Autowired
    private PromotionPublicService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ListResponse<PromotionDto>>> getActivePromotions(@Valid PromotionPublicRequest request) {
        ApiResponse<ListResponse<PromotionDto>> result = service.getActivePromotions(request);
        return ResponseEntity.ok(result);
    }
}