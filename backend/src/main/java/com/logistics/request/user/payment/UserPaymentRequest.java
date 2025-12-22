package com.logistics.request.user.payment;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserPaymentRequest {
    private Integer settlementId;
    private BigDecimal amount;
}
