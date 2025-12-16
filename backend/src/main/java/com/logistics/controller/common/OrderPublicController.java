package com.logistics.controller.common;

import com.logistics.dto.OrderHistoryDto;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.OrderPublicService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/orders")
public class OrderPublicController {

    @Autowired
    private OrderPublicService service;

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<ApiResponse<List<OrderHistoryDto>>> getOrderHistoriesByTrackingNumber(
            @PathVariable String trackingNumber) {

        return ResponseEntity.ok(service.getOrderHistoriesByTrackingNumber(trackingNumber));
    }
}