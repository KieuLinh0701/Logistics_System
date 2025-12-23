package com.logistics.dto.user;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSettlementOrderDto {
    private Integer id;
    private String trackingNumber;
    private Integer cod;
    private Integer totalFee;
    private String status;
    private LocalDateTime deliveriedAt;
    private String payer;
    private String paymentStatus;
}