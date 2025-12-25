package com.logistics.request.user.payment;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserPaymentsRequest {
    private String settlementIds;
    private BigDecimal amount;
}
