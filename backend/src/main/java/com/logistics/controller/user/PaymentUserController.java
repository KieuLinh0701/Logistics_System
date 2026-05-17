package com.logistics.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.request.user.payment.UserPaymentCheck;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.PaymentUserService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/user/payment")
public class PaymentUserController {

        @Autowired
        private PaymentUserService service;

        @PostMapping("/vnpay/check")
        public ResponseEntity<ApiResponse<Boolean>> vnPayReturn(
                        HttpServletRequest request,
                        @RequestBody UserPaymentCheck paymentCheck) {

                return ResponseEntity.ok(
                                service.handleVNPayReturn(paymentCheck));
        }

         @PostMapping("/vnpay/settlements")
        public ResponseEntity<ApiResponse<String>> createVNPayURLForSettlements(
                        HttpServletRequest request) {

                Integer userId = (Integer) request.getAttribute("currentUserId");
                return ResponseEntity.ok(service.createVNPayURLForSettlements(userId, request));
        }

}