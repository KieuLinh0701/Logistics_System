package com.logistics.service.user;

import com.logistics.request.user.payment.UserPaymentCheck;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentUserService {
    String createVNPayURLForSettlements(Integer userId, HttpServletRequest request);
    boolean handleVNPayReturn(UserPaymentCheck paymentCheck);
}