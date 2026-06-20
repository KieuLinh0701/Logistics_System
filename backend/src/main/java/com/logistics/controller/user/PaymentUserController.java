package com.logistics.controller.user;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.user.payment.UserPaymentCheck;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.PaymentUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/payment")
@Tag(name = "User - Payment", description = "Quản lý tích hợp thanh toán trực tuyến qua VNPAY cho các giao dịch và đối soát của người dùng")
public class PaymentUserController {

    @Autowired
    private PaymentUserService service;

    @PostMapping("/vnpay/check")
    public ResponseEntity<ApiResponse<Void>> vnPayReturn(
            @Valid @RequestBody UserPaymentCheck paymentCheck) {

        if (service.handleVNPayReturn(paymentCheck)) {
            return ResponseEntity.ok(ApiResponse.success("Thanh toán thành công", null));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Thanh toán thất bại", null));
        }

    }

    @PostMapping("/vnpay/settlements")
    @Audit(
            entity = EntityType.SETTLEMENT_BATCH,
            action = AuditLogAction.PAY,
            description = AuditLogDescriptionConstant.PAYMENT_VNPAY_CREATE_SETTLEMENT
    )
    public ResponseEntity<ApiResponse<String>> createVNPayURLForSettlements(
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.createVNPayURLForSettlements(userId, request)));
    }

}