package com.logistics.controller.shipper;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.enums.PickupAttemptStatus;
import com.logistics.enums.PickupFailReason;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.shipper.PickupAttemptRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.PickupAttemptService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shipper/orders")
@RequiredArgsConstructor
@Tag(name = "Shipper - Pickup Attempt", description = "Quản lý ghi nhận các lần nỗ lực lấy hàng (pickup) của nhân viên giao hàng")
public class ShipperPickupController {

    private final PickupAttemptService pickupAttemptService;

    private boolean isNotShipper() {
        return !SecurityUtils.hasRole("shipper");
    }

    @PostMapping("/{orderId}/pickup-attempt")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_PICKED_UP,
            params = {"orderId"}
    )
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
