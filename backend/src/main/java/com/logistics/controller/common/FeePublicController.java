package com.logistics.controller.common;

import com.logistics.response.ApiResponse;
import com.logistics.service.common.FeePublicService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/public/fees")
public class FeePublicController {

    @Autowired
    private FeePublicService feeService;

    @GetMapping("/shipping")
    public ResponseEntity<ApiResponse<Integer>> calculateShippingFee(
            @RequestParam BigDecimal weight,
            @RequestParam Integer serviceTypeId,
            @RequestParam Integer senderCodeCity,
            @RequestParam Integer recipientCodeCity
    ) {
        Integer shippingFee = feeService.calculateShippingFee(weight, serviceTypeId, senderCodeCity, recipientCodeCity);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tính phí vận chuyển thành công", shippingFee)
        );
    }

    @GetMapping("/total")
    public ResponseEntity<ApiResponse<Integer>> calculateTotalFee(
            @RequestParam BigDecimal weight,
            @RequestParam Integer serviceTypeId,
            @RequestParam Integer senderCodeCity,
            @RequestParam Integer recipientCodeCity,
            @RequestParam Integer cod,
            @RequestParam Integer orderValue) {

        Integer totalFee = feeService.calculateTotalFee(
                weight,
                serviceTypeId,
                senderCodeCity,
                recipientCodeCity,
                orderValue,
                cod
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tính tổng phí vận chuyển thành công", totalFee));
    }

    @GetMapping("/total-manager")
    public ResponseEntity<ApiResponse<Integer>> calculateTotalFeeManager(
            @RequestParam BigDecimal weight,
            @RequestParam Integer serviceTypeId,
            @RequestParam Integer senderCodeCity,
            @RequestParam Integer recipientCodeCity,
            @RequestParam Integer cod,
            @RequestParam Integer orderValue) {

        Integer totalFee = feeService.calculateTotalFeeManager(
                weight,
                serviceTypeId,
                senderCodeCity,
                recipientCodeCity,
                orderValue,
                cod
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tính tổng phí vận chuyển thành công", totalFee));
    }
}