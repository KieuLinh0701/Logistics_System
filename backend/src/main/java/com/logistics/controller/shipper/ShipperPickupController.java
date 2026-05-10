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
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        PickupAttemptStatus status;
        try {
            status = PickupAttemptStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Trạng thái không hợp lệ", null));
        }

        PickupFailReason failReason = null;
        if (request.getFailReason() != null && !request.getFailReason().isBlank()) {
            try {
                failReason = PickupFailReason.valueOf(request.getFailReason().toUpperCase());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Lý do thất bại không hợp lệ", null));
            }
        }

        if (status == PickupAttemptStatus.FAILED && failReason == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Vui lòng chọn lý do thất bại", null));
        }

        Integer shipperId = SecurityUtils.getAuthenticatedUserId();

        Map<String, Object> data = pickupAttemptService.recordPickupAttempt(
                orderId,
                shipperId,
                status,
                failReason,
                request.getNote());

        return ResponseEntity.ok(new ApiResponse<>(true, "Ghi nhận lần lấy hàng thành công", data));
    }
}
