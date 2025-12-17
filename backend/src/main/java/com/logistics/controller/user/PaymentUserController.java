package com.logistics.controller.user;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.logistics.request.user.payment.UserPaymentCheck;
import com.logistics.request.user.payment.UserPaymentRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.PaymentUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/user/payment")
public class PaymentUserController {

        @Autowired
        private PaymentUserService service;

        @PostMapping("/vnpay/get-url")
        public ResponseEntity<ApiResponse<String>> createVNPayURL(
                        @Valid @RequestBody UserPaymentRequest paymentRequest,
                        HttpServletRequest request) {

                Integer userId = (Integer) request.getAttribute("currentUserId");
                return ResponseEntity.ok(service.createVNPayURL(userId, paymentRequest, request));
        }

        @PostMapping("/vnpay/check")
        public ResponseEntity<ApiResponse<Boolean>> vnPayReturn(
                        HttpServletRequest request,
                        @RequestBody UserPaymentCheck paymentCheck) {

                Integer userId = (Integer) request.getAttribute("currentUserId");

                // Gọi service truyền toàn bộ PaymentCheck
                return ResponseEntity.ok(
                                service.handleVNPayReturn(userId, paymentCheck));
        }

}