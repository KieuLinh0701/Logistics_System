package com.logistics.controller.shipper;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.enums.PickupAttemptStatus;
import com.logistics.enums.PickupFailReason;
import com.logistics.request.shipper.PickupAttemptRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.PickupAttemptService;
import com.logistics.utils.SecurityUtils;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shipper/orders")
@RequiredArgsConstructor
public class ShipperPickupController {

    private final PickupAttemptService pickupAttemptService;

    private boolean isNotShipper() {
        return !SecurityUtils.hasRole("shipper");
    }

    @PostMapping("/{orderId}/pickup-attempt")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordPickupAttempt(
            @PathVariable Integer orderId,
            @RequestBody PickupAttemptRequest request) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        PickupAttemptStatus status;
        try {
            status = PickupAttemptStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.INVALID_ENUM_VALUE);
        }

        PickupFailReason failReason = null;
        if (request.getFailReason() != null && !request.getFailReason().isBlank()) {
            try {
                failReason = PickupFailReason.valueOf(request.getFailReason().toUpperCase());
            } catch (Exception e) {
                throw new AppException(CommonErrorCode.INVALID_ENUM_VALUE);
            }
        }

        if (status == PickupAttemptStatus.FAILED && failReason == null) {
            throw new AppException(CommonErrorCode.VALIDATION_ERROR);
        }

        Integer shipperId = SecurityUtils.getAuthenticatedUserId();

        Map<String, Object> data = pickupAttemptService.recordPickupAttempt(
                orderId,
                shipperId,
                status,
                failReason,
                request.getNote());

        return ResponseEntity.ok(ApiResponse.success("Ghi nhận lần lấy hàng thành công", data));
    }
}
