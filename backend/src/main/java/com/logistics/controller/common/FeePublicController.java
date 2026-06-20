package com.logistics.controller.common;

import com.logistics.response.ApiResponse;
import com.logistics.service.common.FeePublicService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/public/fees")
@Tag(name = "Public - Fee", description = "Tính toán cước phí vận chuyển và quy đổi khối lượng")
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
        return ResponseEntity.ok(ApiResponse.success(shippingFee));
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
                ApiResponse.success("Tính tổng phí vận chuyển thành công", totalFee));
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

        return ResponseEntity.ok(ApiResponse.success(totalFee));
    }

    @GetMapping("/weight")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateWeight(
            @RequestParam BigDecimal originalWeight,
            @RequestParam BigDecimal height,
            @RequestParam BigDecimal length,
            @RequestParam BigDecimal width) {

        BigDecimal weight = feeService.calculateWeight(
                originalWeight,
                height,
                length,
                width
        );

        return ResponseEntity.ok(ApiResponse.success(weight));
    }
}