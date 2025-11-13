package com.logistics.controller;

import com.logistics.response.ApiResponse;
import com.logistics.service.ShippingFeeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/public/shipping-fee")
public class ShippingFeeController {

    @Autowired
    private ShippingFeeService shippingFeeService;

    @GetMapping("/calculate")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateShippingFee(
            @RequestParam BigDecimal weight,
            @RequestParam Integer serviceTypeId,
            @RequestParam Integer senderCodeCity,
            @RequestParam Integer recipientCodeCity
    ) {
        BigDecimal shippingFee = shippingFeeService.calculateShippingFee(weight, serviceTypeId, senderCodeCity, recipientCodeCity);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tính phí vận chuyển thành công", shippingFee)
        );
    }
}