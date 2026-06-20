package com.logistics.controller.common;

import com.logistics.dto.OrderHistoryDto;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.OrderPublicService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/orders")
@Tag(name = "Public - Order", description = "Tra cứu lịch sử hành trình đơn hàng công khai")
public class OrderPublicController {

    @Autowired
    private OrderPublicService service;

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<ApiResponse<List<OrderHistoryDto>>> getOrderHistoriesByTrackingNumber(
            @PathVariable String trackingNumber) {

        return ResponseEntity.ok(ApiResponse.success(service.getOrderHistoriesByTrackingNumber(trackingNumber)));
    }
}