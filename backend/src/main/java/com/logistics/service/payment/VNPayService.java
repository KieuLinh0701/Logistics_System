package com.logistics.service.payment;

import java.math.BigDecimal;

public interface VNPayService {
    String createPaymentUrl(String transactionCode, String settlementCode, Integer settlementId,
            BigDecimal amount, String ip);
}
