package com.logistics.controller.common;

import com.logistics.dto.common.PublicPromotionDto;
import com.logistics.request.common.promotion.PromotionPublicRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.common.PromotionPublicService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/promotions")
@Tag(name = "Public - Promotion", description = "Tra cứu danh sách chương trình khuyến mãi đang hoạt động")
public class PromotionPublicController {

    @Autowired
    private PromotionPublicService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ListResponse<PublicPromotionDto>>> getActivePromotions(@Valid PromotionPublicRequest request) {
        ListResponse<PublicPromotionDto> result = service.getActivePromotions(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}